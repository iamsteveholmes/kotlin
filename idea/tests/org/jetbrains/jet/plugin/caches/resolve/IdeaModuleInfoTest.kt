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

import com.intellij.testFramework.ModuleTestCase
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.DependencyScope

//TODO_r: use smth instead of assertContainsOrdered
class IdeaModuleInfoTest : ModuleTestCase() {

    fun testSimpleDependency() {
        val moduleA = module("a")
        val moduleB = module("b")

        moduleB.addDependency(moduleA)

        val moduleAInfo = moduleA.toSourceInfo()
        val moduleBInfo = moduleB.toSourceInfo()

        assertContainsOrdered(moduleBInfo.dependencies(), moduleBInfo, moduleAInfo)
        assertDoesntContain(moduleAInfo.dependencies(), moduleBInfo)
    }

    fun testCircularDependency() {
        val moduleA = module("a")
        val moduleB = module("b")

        moduleB.addDependency(moduleA)
        moduleA.addDependency(moduleB)

        val moduleAInfo = moduleA.toSourceInfo()
        val moduleBInfo = moduleB.toSourceInfo()

        assertContainsOrdered(moduleAInfo.dependencies(), moduleAInfo, moduleBInfo)
        assertContainsOrdered(moduleBInfo.dependencies(), moduleBInfo, moduleAInfo)
    }

    fun testExportedDependency() {
        val moduleA = module("a")
        val moduleB = module("b")
        val moduleC = module("c")

        moduleB.addDependency(moduleA, exported = true)
        moduleC.addDependency(moduleB)

        val moduleAInfo = moduleA.toSourceInfo()
        val moduleBInfo = moduleB.toSourceInfo()
        val moduleCInfo = moduleC.toSourceInfo()

        assertContainsOrdered(moduleAInfo.dependencies(), moduleAInfo)
        assertContainsOrdered(moduleBInfo.dependencies(), moduleBInfo, moduleAInfo)
        assertContainsOrdered(moduleCInfo.dependencies(), moduleCInfo, moduleBInfo, moduleAInfo)
    }

    fun testRedundantExportedDependency() {
        val moduleA = module("a")
        val moduleB = module("b")
        val moduleC = module("c")

        moduleB.addDependency(moduleA, exported = true)
        moduleC.addDependency(moduleA)
        moduleC.addDependency(moduleB)

        val moduleAInfo = moduleA.toSourceInfo()
        val moduleBInfo = moduleB.toSourceInfo()
        val moduleCInfo = moduleC.toSourceInfo()

        assertContainsOrdered(moduleAInfo.dependencies(), moduleAInfo)
        assertContainsOrdered(moduleBInfo.dependencies(), moduleBInfo, moduleAInfo)
        assertContainsOrdered(moduleCInfo.dependencies(), moduleCInfo, moduleAInfo, moduleBInfo)
    }

    fun testCircularExportedDependency() {
        val moduleA = module("a")
        val moduleB = module("b")
        val moduleC = module("c")

        moduleB.addDependency(moduleA, exported = true)
        moduleC.addDependency(moduleB, exported = true)
        moduleA.addDependency(moduleC, exported = true)

        val moduleAInfo = moduleA.toSourceInfo()
        val moduleBInfo = moduleB.toSourceInfo()
        val moduleCInfo = moduleC.toSourceInfo()

        assertContainsOrdered(moduleAInfo.dependencies(), moduleAInfo, moduleCInfo, moduleBInfo)
        assertContainsOrdered(moduleBInfo.dependencies(), moduleBInfo, moduleAInfo, moduleCInfo)
        assertContainsOrdered(moduleCInfo.dependencies(), moduleCInfo, moduleBInfo, moduleAInfo)
    }

    private fun Module.addDependency(other: Module, dependencyScope: DependencyScope = DependencyScope.COMPILE, exported: Boolean = false) =
            ModuleRootModificationUtil.addDependency(this, other, dependencyScope, exported)

    private fun module(name: String): Module {
        return createModuleFromTestData(createTempDirectory()!!.getAbsolutePath(), name, StdModuleTypes.JAVA, false)!!
    }

}
