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

import org.jetbrains.jet.analyzer.ModuleInfo
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEntry
import com.intellij.openapi.roots.ModuleSourceOrderEntry
import com.intellij.openapi.roots.ModuleOrderEntry
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.module.Module
import org.jetbrains.jet.lang.resolve.name.Name
import java.util.LinkedHashSet
import java.util.ArrayList
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ExportableOrderEntry
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.module.impl.scopes.LibraryScopeBase
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VirtualFile

private abstract class IdeaModuleInfo : ModuleInfo<IdeaModuleInfo> {
    //TODO: add project to this fun and remove from classes params?
    //TODO_r: content scope
    abstract fun filesScope(): GlobalSearchScope
}

private fun orderEntryToModuleInfo(project: Project, orderEntry: OrderEntry): IdeaModuleInfo? {
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

private data class ModuleSourcesInfo(val project: Project, val module: Module) : IdeaModuleInfo() {
    override val name = Name.special("<sources for module ${module.getName()}>")

    override fun filesScope() = GlobalSearchScope.moduleScope(module)

    override fun dependencies(): List<ModuleInfo<IdeaModuleInfo>> {
        return collectModuleDependencies(module).mapTo(LinkedHashSet<IdeaModuleInfo?>(), { orderEntryToModuleInfo(project, it) }).toList().filterNotNull()
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

private data class LibraryInfo(val project: Project, val library: Library) : IdeaModuleInfo() {
    override val name: Name = Name.special("<library ${library.getName()}>")

    override fun filesScope() = LibraryWithoutSourcesScope(project, library)

    override fun dependencies(): List<ModuleInfo<IdeaModuleInfo>> {
        //TODO: correct dependencies
        //val sdk = ProjectRootManager.getInstance(project)!!.getProjectSdk()
        //TODO: Think on this
        val orderEntry = ModuleManager.getInstance(project).getModules().stream().flatMap { ModuleRootManager.getInstance(it).getOrderEntries().stream() }.firstOrNull { it is JdkOrderEntry } as? JdkOrderEntry
        val sdk = orderEntry?.getJdk()
        return if (sdk != null) listOf(SdkInfo(project, sdk), this) else listOf(this)
    }
}

private data class LibrarySourceInfo(val project: Project, val library: Library) : IdeaModuleInfo() {
    override val name: Name = Name.special("<sources for library ${library.getName()}>")

    override fun filesScope() = GlobalSearchScope.EMPTY_SCOPE

    override fun dependencies(): List<ModuleInfo<IdeaModuleInfo>> {
        return listOf(this) + LibraryInfo(project, library).dependencies()
    }
}

private data class SdkInfo(val project: Project, val sdk: Sdk) : IdeaModuleInfo() {
    //TODO: null?
    override val name: Name = Name.special("<library ${sdk.getName()}>")

    override fun filesScope() = SdkScope(project, sdk)

    override fun dependencies(): List<ModuleInfo<IdeaModuleInfo>> = listOf(this)
}

private object NotUnderSourceRootModuleInfo : IdeaModuleInfo() {
    override val name: Name = Name.special("<special module for files not under source root>")

    override fun filesScope() = GlobalSearchScope.EMPTY_SCOPE

    //TODO: provide dependency on runtime
    override fun dependencies(): List<ModuleInfo<IdeaModuleInfo>> = listOf(this)
}

//TODO: duplication with LibraryScope
private data class LibraryWithoutSourcesScope(project: Project, private val library: Library) :
        LibraryScopeBase(project, library.getFiles(OrderRootType.CLASSES), array<VirtualFile>()) {
}

//TODO: deal with android scope
private data class SdkScope(project: Project, private val sdk: Sdk) :
        LibraryScopeBase(project, sdk.getRootProvider().getFiles(OrderRootType.CLASSES), array<VirtualFile>())