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
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.types.expressions.OperatorConventions
import org.jetbrains.jet.lexer.JetToken
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.k2js.translate.context.TranslationContext
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.DescriptorPredicate
import org.jetbrains.k2js.translate.operation.OperatorTable
import org.jetbrains.k2js.translate.utils.BindingUtils
import org.jetbrains.k2js.translate.utils.JsAstUtils
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils
import org.jetbrains.k2js.translate.utils.LongUtils
import org.jetbrains.k2js.translate.utils.PsiUtils.getOperationToken
import org.jetbrains.k2js.translate.utils.BindingUtils.getCallableDescriptorForOperationExpression

public object LongCompareToBOIF : BinaryOperationIntrinsicFactory {

    private object FLOATING_POINT_COMPARE_TO_LONG : BinaryOperationIntrinsic() {
        override fun apply(expression: JetBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            return JsBinaryOperation(operator, left, LongUtils.toNumber(right))
        }
    }

    private object LONG_COMPARE_TO_FLOATING_POINT : BinaryOperationIntrinsic() {
        override fun apply(expression: JetBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            return JsBinaryOperation(operator, LongUtils.toNumber(left), right)
        }
    }

    private object INTEGER_COMPARE_TO_LONG : BinaryOperationIntrinsic() {
        override fun apply(expression: JetBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val correspondingOperator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            val compareInvocation = LongUtils.compare(LongUtils.fromInt(left), right)
            return JsBinaryOperation(correspondingOperator, compareInvocation, context.program().getNumberLiteral(0))
        }
    }

    private object LONG_COMPARE_TO_INTEGER : BinaryOperationIntrinsic() {
        override fun apply(expression: JetBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val correspondingOperator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            val compareInvocation = LongUtils.compare(left, LongUtils.fromInt(right))
            return JsBinaryOperation(correspondingOperator, compareInvocation, context.program().getNumberLiteral(0))
        }
    }

    private object LONG_COMPARE_TO_LONG : BinaryOperationIntrinsic() {
        override fun apply(expression: JetBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val correspondingOperator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            val compareInvocation = LongUtils.compare(left, right)
            return JsBinaryOperation(correspondingOperator, compareInvocation, context.program().getNumberLiteral(0))
        }
    }

    override public fun getIntrinsic(expression: JetBinaryExpression, context: TranslationContext): BinaryOperationIntrinsic? {
        if (OperatorConventions.COMPARISON_OPERATIONS.contains(getOperationToken(expression))) {
            val descriptor = getCallableDescriptorForOperationExpression(context.bindingContext(), expression)
            if (descriptor is FunctionDescriptor && JsDescriptorUtils.isBuiltin(descriptor)) {
                return when {
                    LongUtils.FLOATING_POINT_COMPARE_TO_LONG.apply(descriptor) -> FLOATING_POINT_COMPARE_TO_LONG
                    LongUtils.LONG_COMPARE_TO_FLOATING_POINT.apply(descriptor) -> LONG_COMPARE_TO_FLOATING_POINT
                    LongUtils.INTEGER_COMPARE_TO_LONG.apply(descriptor) -> INTEGER_COMPARE_TO_LONG
                    LongUtils.LONG_COMPARE_TO_INTEGER.apply(descriptor) -> LONG_COMPARE_TO_INTEGER
                    LongUtils.LONG_COMPARE_TO_LONG.apply(descriptor) -> LONG_COMPARE_TO_LONG
                    else -> null
                }
            }
        }
        return null
    }
}
