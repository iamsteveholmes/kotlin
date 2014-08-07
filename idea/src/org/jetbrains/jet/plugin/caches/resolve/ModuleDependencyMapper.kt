/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.plugin.caches.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.module.Module
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.context.GlobalContext
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleSourceOrderEntry
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.openapi.module.ModuleManager
import org.jetbrains.jet.utils.keysToMap
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import com.intellij.openapi.roots.ModuleOrderEntry
import com.intellij.openapi.roots.LibraryOrderEntry
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import org.jetbrains.jet.lang.resolve.java.JvmPlatformParameters
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaClassImpl
import com.intellij.openapi.roots.libraries.Library
import org.jetbrains.jet.analyzer.ModuleInfo
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.module.impl.scopes.LibraryScopeBase
import com.intellij.openapi.roots.OrderRootType
import org.jetbrains.kotlin.util.sure
import com.intellij.psi.PsiElement
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.LibraryOrSdkOrderEntry
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jet.lang.resolve.java.jetAsJava.*
import org.jetbrains.jet.asJava.unwrapped
import com.intellij.openapi.projectRoots.Sdk
import org.jetbrains.jet.analyzer.AnalyzerFacade
import org.jetbrains.jet.analyzer.ResolverForModule
import org.jetbrains.jet.lang.psi.*
import com.intellij.openapi.roots.OrderEntry
import java.util.ArrayList
import com.intellij.openapi.roots.ExportableOrderEntry
import java.util.LinkedHashSet
import org.jetbrains.jet.asJava.FakeLightClassForFileOfPackage
import org.jetbrains.jet.asJava.KotlinLightClassForPackage

//TODO: Idea(Ide)ModuleInfo?
private abstract class PluginModuleInfo : ModuleInfo<PluginModuleInfo> {
    //TODO: add project to this fun and remove from classes params?
    abstract fun filesScope(): GlobalSearchScope
}

private fun orderEntryToModuleInfo(project: Project, orderEntry: OrderEntry): PluginModuleInfo? {
    return when (orderEntry) {
        is ModuleSourceOrderEntry -> {
            ModuleSourcesInfo(project, orderEntry.getRootModel().getModule())
        }
        is ModuleOrderEntry -> {
            val dependencyModule = orderEntry.getModule()
            //TODO: null?
            ModuleSourcesInfo(project, dependencyModule!!)
        }
        is LibraryOrderEntry -> {
            //TODO: null?
            val library = orderEntry.getLibrary()!!
            val isKotlinRuntime = library.getName() == "KotlinJavaRuntime"
            if (isKotlinRuntime) {
            }
            LibraryInfo(project, library)
        }
        is JdkOrderEntry -> {
            //TODO: null?
            SdkInfo(project, orderEntry.getJdk()!!)
        }
        else -> {
            null
        }
    }
}

private data class ModuleSourcesInfo(val project: Project, val module: Module) : PluginModuleInfo() {
    override val name = Name.special("<sources for module ${module.getName()}>")

    override fun filesScope() = GlobalSearchScope.moduleScope(module)

    override fun dependencies(): List<ModuleInfo<PluginModuleInfo>> {
        return collectModuleDependencies(module).mapTo(LinkedHashSet<PluginModuleInfo?>(), { orderEntryToModuleInfo(project, it) }).toList().filterNotNull()
    }

    private fun collectModuleDependencies(module: Module, exportedOnly: Boolean = false): List<OrderEntry> {
        val result = ArrayList<OrderEntry>()
        val entries = ModuleRootManager.getInstance(module).getOrderEntries()
        entries.flatMapTo(result) { entry ->
            val isExported = entry is ExportableOrderEntry && entry.isExported()
            if (!isExported && exportedOnly) {
                listOf()
            }
            else if (entry in result) {
                listOf()
            }
            else if (entry is ModuleOrderEntry) {
                listOf(entry) + collectModuleDependencies(entry.getModule()!!, exportedOnly = true)
            }
            else {
                listOf(entry)
            }
        }
        return result
    }
}

private data class LibraryInfo(val project: Project, val library: Library) : PluginModuleInfo() {
    override val name: Name = Name.special("<library ${library.getName()}>")

    override fun filesScope() = LibraryWithoutSourcesScope(project, library)

    override fun dependencies(): List<ModuleInfo<PluginModuleInfo>> {
        //TODO: correct dependencies
        //val sdk = ProjectRootManager.getInstance(project)!!.getProjectSdk()
        //TODO: Think on this
        val orderEntry = ModuleManager.getInstance(project).getModules().stream().flatMap { ModuleRootManager.getInstance(it).getOrderEntries().stream() }.firstOrNull { it is JdkOrderEntry } as? JdkOrderEntry
        val sdk = orderEntry?.getJdk()
        return if (sdk != null) listOf(SdkInfo(project, sdk), this) else listOf(this)
    }
}

private data class LibrarySourcesInfo(val project: Project, val library: Library) : PluginModuleInfo() {
    override val name: Name = Name.special("<sources for library ${library.getName()}>")

    override fun filesScope() = GlobalSearchScope.EMPTY_SCOPE

    override fun dependencies(): List<ModuleInfo<PluginModuleInfo>> {
        return listOf(this) + LibraryInfo(project, library).dependencies()
    }
}

private data class SdkInfo(val project: Project, val sdk: Sdk) : PluginModuleInfo() {
    //TODO: null?
    override val name: Name = Name.special("<library ${sdk.getName()}>")

    override fun filesScope() = SdkScope(project, sdk)

    override fun dependencies(): List<ModuleInfo<PluginModuleInfo>> = listOf(this)
}

private object NotUnderSourceRootModuleInfo : PluginModuleInfo() {
    override val name: Name = Name.special("<special module for files not under source root>")

    override fun filesScope() = GlobalSearchScope.EMPTY_SCOPE

    //TODO: provide dependency on runtime
    override fun dependencies(): List<ModuleInfo<PluginModuleInfo>> = listOf(this)
}


fun createMappingForProject(
        globalContext: GlobalContext,
        project: Project,
        analyzerFacade: AnalyzerFacade<*, *>,
        syntheticFiles: Collection<JetFile>
): ModuleSetup {

    val ideaModules = ModuleManager.getInstance(project).getModules().toList()
    val modulesSourcesInfos = ideaModules.keysToMap { ModuleSourcesInfo(project, it) }

    val ideaLibraries = ideaModules.flatMap { ModuleRootManager.getInstance(it).getOrderEntries().filterIsInstance(javaClass<LibraryOrderEntry>()).map { /*TODO: null*/it.getLibrary()!! } }.toSet()
    val librariesInfos = ideaLibraries.keysToMap { LibraryInfo(project, it) }

    val ideaSdks = ideaModules.flatMap { ModuleRootManager.getInstance(it).getOrderEntries().filterIsInstance(javaClass<JdkOrderEntry>()).map { /*TODO: null*/it.getJdk()!! } }.toSet()
    val sdksInfos = ideaSdks.keysToMap { SdkInfo(project, it) }

    val allModuleInfos = (modulesSourcesInfos.values() + librariesInfos.values() + sdksInfos.values()).toHashSet()

    val syntheticFilesByModule = syntheticFiles.groupBy { it.getModuleInfo()!! /*TODO: null?*/ }
    allModuleInfos.addAll(syntheticFilesByModule.keySet())

    val jvmPlatformParameters = {(module: PluginModuleInfo) ->
        JvmPlatformParameters(syntheticFilesByModule[module] ?: listOf(), module.filesScope()) {
            javaClass ->
            val psiClass = (javaClass as JavaClassImpl).getPsi()
            psiClass.getModuleInfo().sure("Module not found for ${psiClass.getName()} in ${psiClass.getContainingFile()}")
        }
    }
    val resolverForProject = analyzerFacade.setupResolverForProject(globalContext, project, allModuleInfos, jvmPlatformParameters)

    val moduleToBodiesResolveSession = allModuleInfos.keysToMap {
        module ->
        val descriptor = resolverForProject.descriptorByModule[module]!!
        val analyzer = resolverForProject.analyzerByModuleDescriptor[descriptor]!!
        ResolveSessionForBodies(project, analyzer.lazyResolveSession)
    }
    return ModuleSetup(resolverForProject.descriptorByModule, resolverForProject.analyzerByModuleDescriptor, moduleToBodiesResolveSession)
}

//TODO: actually nullable
//TODO: rename
class ModuleSetup(private val descriptorByModule: Map<PluginModuleInfo, ModuleDescriptor>,
                  private val setupByModuleDescriptor: Map<ModuleDescriptor, ResolverForModule>,
                  private val bodiesResolveByModule: Map<PluginModuleInfo, ResolveSessionForBodies>
) {
    fun descriptorByModule(module: PluginModuleInfo) = descriptorByModule[module].sure("$module")
    fun setupByModule(module: PluginModuleInfo) = setupByModuleDescriptor[descriptorByModule[module]!!].sure("$module")
    fun setupByDescriptor(module: ModuleDescriptor) = setupByModuleDescriptor[module].sure("$module")
    fun resolveSessionForBodiesByModule(module: PluginModuleInfo) = bodiesResolveByModule[module].sure("$module")
    fun resolveSessionForBodiesByModuleDescriptor(module: ModuleDescriptor): ResolveSessionForBodies? {
        val moduleInfo = descriptorByModule.entrySet().firstOrNull() { it.value == module }?.key ?: return null
        return bodiesResolveByModule[moduleInfo]
    }
    val modules: Collection<PluginModuleInfo> = descriptorByModule.keySet()
}

//TODO: duplication with LibraryScope
private data class LibraryWithoutSourcesScope(project: Project, private val library: Library) :
        LibraryScopeBase(project, library.getFiles(OrderRootType.CLASSES), array<VirtualFile>()) {
}

//TODO: deal with android scope
private data class SdkScope(project: Project, private val sdk: Sdk) :
        LibraryScopeBase(project, sdk.getRootProvider().getFiles(OrderRootType.CLASSES), array<VirtualFile>())

//TODO: is it nullable?
//TODO: should be private
fun PsiElement.getModuleInfo(): PluginModuleInfo? {
    //TODO: clearer code
    if (this is KotlinLightElement<*, *>) return this.getPluginModuleInfo()
    if (this is JetCodeFragment) return this.getContext()?.getModuleInfo()

    val containingFile = (this as? JetElement)?.getContainingFile()
    val context = containingFile?.getUserData(ANALYSIS_CONTEXT)
    if (context != null) return context.getModuleInfo()

    val doNotAnalyze = containingFile?.getUserData(DO_NOT_ANALYZE)
    if (doNotAnalyze != null) {
        println("Should not analyze element: ${getText()} in file ${containingFile?.getName() ?: " no file"}")
        println(doNotAnalyze)
    }
    val project = getProject()
    //TODO: deal with non physical file
    //TODO: can be clearer and more optimal?
    //TODO: utilities for transforming entry to info
    val virtualFile = getContainingFile().sure("${getText()}").getOriginalFile().getVirtualFile().sure("${getContainingFile()}")

    val projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project)
    val orderEntries = projectFileIndex.getOrderEntriesForFile(virtualFile)

    //TODO: can be multiple?
    val moduleSourceOrderEntry = orderEntries.filterIsInstance(javaClass<ModuleSourceOrderEntry>()).firstOrNull()
    if (moduleSourceOrderEntry != null) {
        return ModuleSourcesInfo(project, moduleSourceOrderEntry.getRootModel().getModule())
    }
    val libraryOrSdkOrderEntry = orderEntries.filterIsInstance(javaClass<LibraryOrSdkOrderEntry>()).firstOrNull()
    return when (libraryOrSdkOrderEntry) {
        is LibraryOrderEntry -> {
            //TODO: deal with null again
            val library = libraryOrSdkOrderEntry.getLibrary().sure("bla bla")
            if (ProjectFileIndex.SERVICE.getInstance(project).isInLibrarySource(virtualFile)) {
                LibrarySourcesInfo(project, library)
            }
            else {
                LibraryInfo(project, library)
            }
        }
        //TODO: JdkSources?
        is JdkOrderEntry -> SdkInfo(project, libraryOrSdkOrderEntry.getJdk()!!)
        else -> NotUnderSourceRootModuleInfo
    }
}

//TODO: make member?
public fun KotlinLightElement<*, *>.getPluginModuleInfo(): PluginModuleInfo {
    val element = origin ?: when (this) {
        is FakeLightClassForFileOfPackage -> this.getContainingFile()!!
        is KotlinLightClassForPackage -> this.getFiles().first()
        else -> throw IllegalStateException("Unknown light class is referenced by IDE lazy resolve: $javaClass")
    }
    return element.getModuleInfo()!!
}