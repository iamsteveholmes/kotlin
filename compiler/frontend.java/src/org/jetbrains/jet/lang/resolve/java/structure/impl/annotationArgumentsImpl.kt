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

package org.jetbrains.jet.lang.resolve.java.structure.impl

import com.intellij.psi.*
import org.jetbrains.jet.lang.resolve.java.structure.*
import org.jetbrains.jet.lang.resolve.name.Name

abstract class JavaAnnotationArgumentImpl<Psi : PsiAnnotationMemberValue>(
        psiAnnotationMemberValue: Psi,
        override val name: Name?
) : JavaElementImpl<Psi>(psiAnnotationMemberValue), JavaAnnotationArgument {
    class object {
        fun create(argument: PsiAnnotationMemberValue, name: Name?): JavaAnnotationArgument {
            val value = JavaPsiFacade.getInstance(argument.getProject()).getConstantEvaluationHelper().computeConstantExpression(argument)
            if (value != null || argument is PsiLiteralExpression) {
                return JavaLiteralAnnotationArgumentImpl(name, value)
            }

            return when (argument) {
                is PsiReferenceExpression -> JavaEnumValueAnnotationArgumentImpl(argument as PsiReferenceExpression, name)
                is PsiArrayInitializerMemberValue -> JavaArrayAnnotationArgumentImpl(argument as PsiArrayInitializerMemberValue, name)
                is PsiAnnotation -> JavaAnnotationAsAnnotationArgumentImpl(argument as PsiAnnotation, name)
                is PsiClassObjectAccessExpression -> JavaClassObjectAnnotationArgumentImpl(argument as PsiClassObjectAccessExpression, name)
                else -> throw UnsupportedOperationException("Unsupported annotation argument type: " + argument)
            }
        }
    }
}

class JavaLiteralAnnotationArgumentImpl(
        override val name: Name?,
        override val value: Any?
) : JavaLiteralAnnotationArgument

class JavaArrayAnnotationArgumentImpl(
        psiValue: PsiArrayInitializerMemberValue,
        name: Name?
) : JavaAnnotationArgumentImpl<PsiArrayInitializerMemberValue>(psiValue, name), JavaArrayAnnotationArgument {
    override fun getElements() = getPsi().getInitializers().map { JavaAnnotationArgumentImpl.create(it, null) }
}

class JavaEnumValueAnnotationArgumentImpl(
        psiReference: PsiReferenceExpression,
        name: Name?
) : JavaAnnotationArgumentImpl<PsiReferenceExpression>(psiReference, name), JavaEnumValueAnnotationArgument {
    override fun resolve(): JavaField? {
        val element = getPsi().resolve()
        return when (element) {
            null -> null
            is PsiEnumConstant -> JavaFieldImpl(element)
            else -> throw IllegalStateException("Reference argument should be an enum value, but was $element: ${element.getText()}")
        }
    }
}

class JavaClassObjectAnnotationArgumentImpl(
        psiExpression: PsiClassObjectAccessExpression,
        name: Name?
) : JavaAnnotationArgumentImpl<PsiClassObjectAccessExpression>(psiExpression, name), JavaClassObjectAnnotationArgument {
    override fun getReferencedType() = JavaTypeImpl.create(getPsi().getOperand().getType())
}

class JavaAnnotationAsAnnotationArgumentImpl(
        psiAnnotation: PsiAnnotation,
        name: Name?
) : JavaAnnotationArgumentImpl<PsiAnnotation>(psiAnnotation, name), JavaAnnotationAsAnnotationArgument {
    override fun getAnnotation() = JavaAnnotationImpl(getPsi())
}
