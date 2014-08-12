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

import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.resolve.java.jetAsJava.KotlinLightElement
import org.jetbrains.jet.lang.psi.JetCodeFragment
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.psi.ANALYSIS_CONTEXT
import org.jetbrains.jet.lang.psi.DO_NOT_ANALYZE
import org.jetbrains.kotlin.util.sure
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ModuleSourceOrderEntry
import com.intellij.openapi.roots.LibraryOrSdkOrderEntry
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.JdkOrderEntry
import org.jetbrains.jet.asJava.FakeLightClassForFileOfPackage
import org.jetbrains.jet.asJava.KotlinLightClassForPackage

//TODO: is it nullable?
//TODO: should be private
fun PsiElement.getModuleInfo(): IdeaModuleInfo {
    //TODO: clearer code
    if (this is KotlinLightElement<*, *>) return this.getIdeaModuleInfo()
    //TODO_r: change assertion to LOG
    if (this is JetCodeFragment) return this.getContext().sure("Analyzing code fragment with no context element: $this").getModuleInfo()

    val containingFile = (this as? JetElement)?.getContainingFile()
    val context = containingFile?.getUserData(ANALYSIS_CONTEXT)
    if (context != null) return context.getModuleInfo()

    //TODO_r: log info about contents and file name
    val doNotAnalyze = containingFile?.getUserData(DO_NOT_ANALYZE)
    if (doNotAnalyze != null) {
        //TODO_r: println!!!
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
                LibrarySourceInfo(project, library)
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
private fun KotlinLightElement<*, *>.getIdeaModuleInfo(): IdeaModuleInfo {
    val element = origin ?: when (this) {
        is FakeLightClassForFileOfPackage -> this.getContainingFile()!!
        is KotlinLightClassForPackage -> this.getFiles().first()
        else -> throw IllegalStateException("Unknown light class is referenced by IDE lazy resolve: $javaClass")
    }
    return element.getModuleInfo()
}