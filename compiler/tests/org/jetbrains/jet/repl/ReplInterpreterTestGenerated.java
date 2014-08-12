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

package org.jetbrains.jet.repl;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.test.InnerTestClasses;
import org.jetbrains.jet.test.TestMetadata;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.jet.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("compiler/testData/repl")
@InnerTestClasses({ReplInterpreterTestGenerated.Classes.class, ReplInterpreterTestGenerated.Multiline.class, ReplInterpreterTestGenerated.Objects.class, ReplInterpreterTestGenerated.PrimitiveTypes.class, ReplInterpreterTestGenerated.Reflection.class})
public class ReplInterpreterTestGenerated extends AbstractReplInterpreterTest {
    public void testAllFilesPresentInRepl() throws Exception {
        JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/repl"), Pattern.compile("^(.+)\\.repl$"), true);
    }
    
    @TestMetadata("analyzeErrors.repl")
    public void testAnalyzeErrors() throws Exception {
        doTest("compiler/testData/repl/analyzeErrors.repl");
    }
    
    @TestMetadata("constants.repl")
    public void testConstants() throws Exception {
        doTest("compiler/testData/repl/constants.repl");
    }
    
    @TestMetadata("empty.repl")
    public void testEmpty() throws Exception {
        doTest("compiler/testData/repl/empty.repl");
    }
    
    @TestMetadata("evaluationErrors.repl")
    public void testEvaluationErrors() throws Exception {
        doTest("compiler/testData/repl/evaluationErrors.repl");
    }
    
    @TestMetadata("function.repl")
    public void testFunction() throws Exception {
        doTest("compiler/testData/repl/function.repl");
    }
    
    @TestMetadata("functionOverloadResolution.repl")
    public void testFunctionOverloadResolution() throws Exception {
        doTest("compiler/testData/repl/functionOverloadResolution.repl");
    }
    
    @TestMetadata("functionOverloadResolutionAnyBeatsString.repl")
    public void testFunctionOverloadResolutionAnyBeatsString() throws Exception {
        doTest("compiler/testData/repl/functionOverloadResolutionAnyBeatsString.repl");
    }
    
    @TestMetadata("functionReferencesPrev.repl")
    public void testFunctionReferencesPrev() throws Exception {
        doTest("compiler/testData/repl/functionReferencesPrev.repl");
    }
    
    @TestMetadata("imports.repl")
    public void testImports() throws Exception {
        doTest("compiler/testData/repl/imports.repl");
    }
    
    @TestMetadata("simple.repl")
    public void testSimple() throws Exception {
        doTest("compiler/testData/repl/simple.repl");
    }
    
    @TestMetadata("syntaxErrors.repl")
    public void testSyntaxErrors() throws Exception {
        doTest("compiler/testData/repl/syntaxErrors.repl");
    }
    
    @TestMetadata("twoClosures.repl")
    public void testTwoClosures() throws Exception {
        doTest("compiler/testData/repl/twoClosures.repl");
    }
    
    @TestMetadata("compiler/testData/repl/classes")
    public static class Classes extends AbstractReplInterpreterTest {
        public void testAllFilesPresentInClasses() throws Exception {
            JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/repl/classes"), Pattern.compile("^(.+)\\.repl$"), true);
        }
        
        @TestMetadata("classInheritance.repl")
        public void testClassInheritance() throws Exception {
            doTest("compiler/testData/repl/classes/classInheritance.repl");
        }
        
        @TestMetadata("classRedeclaration.repl")
        public void testClassRedeclaration() throws Exception {
            doTest("compiler/testData/repl/classes/classRedeclaration.repl");
        }
        
        @TestMetadata("emptyClass.repl")
        public void testEmptyClass() throws Exception {
            doTest("compiler/testData/repl/classes/emptyClass.repl");
        }
        
        @TestMetadata("emptyClassRedeclaration.repl")
        public void testEmptyClassRedeclaration() throws Exception {
            doTest("compiler/testData/repl/classes/emptyClassRedeclaration.repl");
        }
        
        @TestMetadata("enumEntrySubclass.repl")
        public void testEnumEntrySubclass() throws Exception {
            doTest("compiler/testData/repl/classes/enumEntrySubclass.repl");
        }
        
        @TestMetadata("import.repl")
        public void testImport() throws Exception {
            doTest("compiler/testData/repl/classes/import.repl");
        }
        
        @TestMetadata("simpleClass.repl")
        public void testSimpleClass() throws Exception {
            doTest("compiler/testData/repl/classes/simpleClass.repl");
        }
        
        @TestMetadata("simpleEnum.repl")
        public void testSimpleEnum() throws Exception {
            doTest("compiler/testData/repl/classes/simpleEnum.repl");
        }
        
        @TestMetadata("simpleTrait.repl")
        public void testSimpleTrait() throws Exception {
            doTest("compiler/testData/repl/classes/simpleTrait.repl");
        }
        
    }
    
    @TestMetadata("compiler/testData/repl/multiline")
    public static class Multiline extends AbstractReplInterpreterTest {
        public void testAllFilesPresentInMultiline() throws Exception {
            JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/repl/multiline"), Pattern.compile("^(.+)\\.repl$"), true);
        }
        
        @TestMetadata("functionOnSeveralLines.repl")
        public void testFunctionOnSeveralLines() throws Exception {
            doTest("compiler/testData/repl/multiline/functionOnSeveralLines.repl");
        }
        
        @TestMetadata("multilineFunctionInvocation.repl")
        public void testMultilineFunctionInvocation() throws Exception {
            doTest("compiler/testData/repl/multiline/multilineFunctionInvocation.repl");
        }
        
        @TestMetadata("openParenthesisIncomplete.repl")
        public void testOpenParenthesisIncomplete() throws Exception {
            doTest("compiler/testData/repl/multiline/openParenthesisIncomplete.repl");
        }
        
        @TestMetadata("simpleFunctionBodyOnNextLine.repl")
        public void testSimpleFunctionBodyOnNextLine() throws Exception {
            doTest("compiler/testData/repl/multiline/simpleFunctionBodyOnNextLine.repl");
        }
        
    }
    
    @TestMetadata("compiler/testData/repl/objects")
    public static class Objects extends AbstractReplInterpreterTest {
        public void testAllFilesPresentInObjects() throws Exception {
            JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/repl/objects"), Pattern.compile("^(.+)\\.repl$"), true);
        }
        
        @TestMetadata("emptyObject.repl")
        public void testEmptyObject() throws Exception {
            doTest("compiler/testData/repl/objects/emptyObject.repl");
        }
        
        @TestMetadata("localObject.repl")
        public void testLocalObject() throws Exception {
            doTest("compiler/testData/repl/objects/localObject.repl");
        }
        
        @TestMetadata("simpleObjectDeclaration.repl")
        public void testSimpleObjectDeclaration() throws Exception {
            doTest("compiler/testData/repl/objects/simpleObjectDeclaration.repl");
        }
        
    }
    
    @TestMetadata("compiler/testData/repl/primitiveTypes")
    public static class PrimitiveTypes extends AbstractReplInterpreterTest {
        public void testAllFilesPresentInPrimitiveTypes() throws Exception {
            JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/repl/primitiveTypes"), Pattern.compile("^(.+)\\.repl$"), true);
        }
        
        @TestMetadata("arrayOfBoxed.repl")
        public void testArrayOfBoxed() throws Exception {
            doTest("compiler/testData/repl/primitiveTypes/arrayOfBoxed.repl");
        }
        
        @TestMetadata("boxingOnPurpose.repl")
        public void testBoxingOnPurpose() throws Exception {
            doTest("compiler/testData/repl/primitiveTypes/boxingOnPurpose.repl");
        }
        
    }
    
    @TestMetadata("compiler/testData/repl/reflection")
    public static class Reflection extends AbstractReplInterpreterTest {
        public void testAllFilesPresentInReflection() throws Exception {
            JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/repl/reflection"), Pattern.compile("^(.+)\\.repl$"), true);
        }
        
        @TestMetadata("propertyReference.repl")
        public void testPropertyReference() throws Exception {
            doTest("compiler/testData/repl/reflection/propertyReference.repl");
        }
        
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite("ReplInterpreterTestGenerated");
        suite.addTestSuite(ReplInterpreterTestGenerated.class);
        suite.addTestSuite(Classes.class);
        suite.addTestSuite(Multiline.class);
        suite.addTestSuite(Objects.class);
        suite.addTestSuite(PrimitiveTypes.class);
        suite.addTestSuite(Reflection.class);
        return suite;
    }
}
