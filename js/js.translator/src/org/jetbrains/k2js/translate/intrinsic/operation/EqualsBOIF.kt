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

package org.jetbrains.k2js.translate.intrinsic.operation

import com.google.dart.compiler.backend.js.ast.*
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.types.expressions.OperatorConventions
import org.jetbrains.k2js.translate.context.TranslationContext
import org.jetbrains.k2js.translate.intrinsic.functions.factories.TopLevelFIF
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.NamePredicate
import org.jetbrains.k2js.translate.utils.JsAstUtils
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils
import org.jetbrains.k2js.translate.utils.TranslationUtils

import java.util.Arrays

import org.jetbrains.k2js.translate.utils.BindingUtils.getCallableDescriptorForOperationExpression
import org.jetbrains.k2js.translate.utils.PsiUtils.getOperationToken
import org.jetbrains.jet.lexer.JetToken
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.k2js.translate.utils.LongUtils

public object EqualsBOIF : BinaryOperationIntrinsicFactory {

    private object LONG_EQUALS_ANY_INTRINSIC : BinaryOperationIntrinsic() {
        override fun apply(expression: JetBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val isNegated = getOperationToken(expression) == JetTokens.EXCLEQ
            val invokeEquals = LongUtils.equalsSafe(left, right)
            return if (isNegated) JsAstUtils.not(invokeEquals) else invokeEquals
        }
    }

    private object EqualsIntrinsic : BinaryOperationIntrinsic() {
        override fun apply(expression: JetBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val isNegated = getOperationToken(expression) == JetTokens.EXCLEQ
            if (right == JsLiteral.NULL || left == JsLiteral.NULL) {
                return TranslationUtils.nullCheck(if (right == JsLiteral.NULL) left else right, isNegated)
            }
            else if (canUseSimpleEquals(expression, context)) {
                return JsBinaryOperation(if (isNegated) JsBinaryOperator.REF_NEQ else JsBinaryOperator.REF_EQ, left, right)
            }

            val result = TopLevelFIF.KOTLIN_EQUALS.apply(left, Arrays.asList<JsExpression>(right), context)
            return if (isNegated) JsAstUtils.negated(result) else result
        }

        private fun canUseSimpleEquals(expression: JetBinaryExpression, context: TranslationContext): Boolean {
            val left = expression.getLeft()
            assert(left != null) { "No left-hand side: " + expression.getText() }
            val typeName = JsDescriptorUtils.getNameIfStandardType(left!!, context)
            return typeName != null && NamePredicate.PRIMITIVE_NUMBERS_MAPPED_TO_PRIMITIVE_JS.apply(typeName)
        }
    }

    override public fun getIntrinsic(expression: JetBinaryExpression, context: TranslationContext): BinaryOperationIntrinsic? {
        if (OperatorConventions.EQUALS_OPERATIONS.contains(getOperationToken(expression))) {
            val descriptor = getCallableDescriptorForOperationExpression(context.bindingContext(), expression)
            if (descriptor is FunctionDescriptor && (JsDescriptorUtils.isBuiltin(descriptor) || TopLevelFIF.EQUALS_IN_ANY.apply(descriptor))) {
                return if (LongUtils.LONG_EQUALS_ANY.apply(descriptor)) LONG_EQUALS_ANY_INTRINSIC else EqualsIntrinsic
            }
        }
        return null
    }
}

