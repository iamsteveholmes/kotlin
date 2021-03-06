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

package org.jetbrains.k2js.analyze;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.context.ContextPackage;
import org.jetbrains.jet.context.GlobalContextImpl;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJs;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.k2js.config.Config;

import java.util.Collection;
import java.util.List;

public final class TopDownAnalyzerFacadeForJS {
    public static final List<ImportPath> DEFAULT_IMPORTS = ImmutableList.of(
            new ImportPath("js.*"),
            new ImportPath("java.lang.*"),
            new ImportPath("kotlin.*")
    );

    private TopDownAnalyzerFacadeForJS() {
    }

    //NOTE: web demo related method
    @SuppressWarnings("UnusedDeclaration")
    @NotNull
    public static BindingContext analyzeFiles(@NotNull Collection<JetFile> files, @NotNull Config config) {
        return analyzeFiles(files, Predicates.<PsiFile>alwaysTrue(), config).getBindingContext();
    }

    //TODO: refactor
    @NotNull
    public static AnalyzeExhaust analyzeFiles(
            @NotNull Collection<JetFile> files,
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely,
            @NotNull Config config
    ) {
        Project project = config.getProject();

        ModuleDescriptorImpl owner = createJsModule("<module>");

        Predicate<PsiFile> completely = Predicates.and(notLibFiles(config.getLibFiles()), filesToAnalyzeCompletely);

        GlobalContextImpl globalContext = ContextPackage.GlobalContext();
        TopDownAnalysisParameters topDownAnalysisParameters = TopDownAnalysisParameters.create(
                globalContext.getStorageManager(), globalContext.getExceptionTracker(), completely, false, false);

        owner.addDependencyOnModule(owner);
        owner.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule());
        ModuleDescriptor libraryModule = config.getLibraryModule();
        if (libraryModule != null) {
            owner.addDependencyOnModule((ModuleDescriptorImpl) libraryModule); // "import" analyzed library module
        }
        owner.seal();

        BindingContext libraryContext = config.getLibraryContext();
        BindingTrace trace = libraryContext == null
                             ? new BindingTraceContext()
                             : new DelegatingBindingTrace(libraryContext, "trace with preanalyzed library");
        InjectorForTopDownAnalyzerForJs injector = new InjectorForTopDownAnalyzerForJs(project, topDownAnalysisParameters, trace, owner);
        try {
            Collection<JetFile> allFiles = libraryModule != null ?
                                           files :
                                           Config.withJsLibAdded(files, config);
            injector.getTopDownAnalyzer().analyzeFiles(topDownAnalysisParameters, allFiles);
            return AnalyzeExhaust.success(trace.getBindingContext(), owner);
        }
        finally {
            injector.destroy();
        }
    }

    public static void checkForErrors(@NotNull Collection<JetFile> allFiles, @NotNull BindingContext bindingContext) {
        AnalyzingUtils.throwExceptionOnErrors(bindingContext);
        for (JetFile file : allFiles) {
            AnalyzingUtils.checkForSyntacticErrors(file);
        }
    }

    @NotNull
    private static Predicate<PsiFile> notLibFiles(@NotNull final List<JetFile> jsLibFiles) {
        return new Predicate<PsiFile>() {
            @Override
            public boolean apply(@Nullable PsiFile file) {
                assert file instanceof JetFile;
                @SuppressWarnings("UnnecessaryLocalVariable") boolean notLibFile = !jsLibFiles.contains(file);
                return notLibFile;
            }
        };
    }

    @NotNull
    private static ModuleDescriptorImpl createJsModule(@NotNull String name) {
        return new ModuleDescriptorImpl(Name.special(name), DEFAULT_IMPORTS, PlatformToKotlinClassMap.EMPTY);
    }

}
