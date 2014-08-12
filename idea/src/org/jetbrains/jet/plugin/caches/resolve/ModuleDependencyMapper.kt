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
import org.jetbrains.jet.context.GlobalContext
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleSourceOrderEntry
import com.intellij.openapi.module.ModuleManager
import org.jetbrains.jet.utils.keysToMap
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import com.intellij.openapi.roots.LibraryOrderEntry
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import org.jetbrains.jet.lang.resolve.java.JvmPlatformParameters
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaClassImpl
import com.intellij.openapi.roots.JdkOrderEntry
import org.jetbrains.kotlin.util.sure
import com.intellij.psi.PsiElement
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.LibraryOrSdkOrderEntry
import org.jetbrains.jet.lang.resolve.java.jetAsJava.*
import org.jetbrains.jet.analyzer.AnalyzerFacade
import org.jetbrains.jet.analyzer.ResolverForModule
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.asJava.FakeLightClassForFileOfPackage
import org.jetbrains.jet.asJava.KotlinLightClassForPackage

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

    val syntheticFilesByModule = syntheticFiles.groupBy { it.getModuleInfo() }
    allModuleInfos.addAll(syntheticFilesByModule.keySet())

    val jvmPlatformParameters = {(module: IdeaModuleInfo) ->
        JvmPlatformParameters(syntheticFilesByModule[module] ?: listOf(), module.filesScope()) {
            javaClass ->
            val psiClass = (javaClass as JavaClassImpl).getPsi()
            psiClass.getModuleInfo()
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
class ModuleSetup(private val descriptorByModule: Map<IdeaModuleInfo, ModuleDescriptor>,
                  private val setupByModuleDescriptor: Map<ModuleDescriptor, ResolverForModule>,
                  private val bodiesResolveByModule: Map<IdeaModuleInfo, ResolveSessionForBodies>
) {
    fun descriptorByModule(module: IdeaModuleInfo) = descriptorByModule[module].sure("$module")
    fun setupByModule(module: IdeaModuleInfo) = setupByModuleDescriptor[descriptorByModule[module]!!].sure("$module")
    fun setupByDescriptor(module: ModuleDescriptor) = setupByModuleDescriptor[module].sure("$module")
    fun resolveSessionForBodiesByModule(module: IdeaModuleInfo) = bodiesResolveByModule[module].sure("$module")
    fun resolveSessionForBodiesByModuleDescriptor(module: ModuleDescriptor): ResolveSessionForBodies? {
        val moduleInfo = descriptorByModule.entrySet().firstOrNull() { it.value == module }?.key ?: return null
        return bodiesResolveByModule[moduleInfo]
    }
    val modules: Collection<IdeaModuleInfo> = descriptorByModule.keySet()
}