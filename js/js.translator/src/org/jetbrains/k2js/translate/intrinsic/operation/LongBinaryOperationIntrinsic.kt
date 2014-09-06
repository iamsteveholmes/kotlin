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
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder
import org.jetbrains.k2js.translate.utils.BindingUtils
import org.jetbrains.k2js.translate.utils.JsAstUtils
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils
import org.jetbrains.k2js.translate.utils.PsiUtils.getOperationToken

private abstract class BinaryOperationIntrinsicImpl(private val operationTokens: Set<JetToken>, private val predicate: DescriptorPredicate) : BinaryOperationIntrinsic {

    override fun isApplicable(expression: JetBinaryExpression, context: TranslationContext): Boolean {
        if (!operationTokens.contains(getOperationToken(expression))) {
            return false
        }

        val descriptor = BindingUtils.getCallableDescriptorForOperationExpression(context.bindingContext(), expression)
        if (descriptor is FunctionDescriptor && JsDescriptorUtils.isBuiltin(descriptor)) {
            val functionDescriptor = descriptor as FunctionDescriptor
            return predicate.apply(functionDescriptor)
        }
        return false
    }
}

public object LONG_EQUALS_ANY_INTRINSIC : BinaryOperationIntrinsicImpl(
        OperatorConventions.EQUALS_OPERATIONS,
        PatternBuilder.pattern("Long.equals")
) {
    override fun apply(expression: JetBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
        val isNegated = getOperationToken(expression) == JetTokens.EXCLEQ
        // Note: explicit type annotation, see KT-5569 IllegalAccessError for common non-public ancestor
        val invokeEquals: JsExpression = JsInvocation(JsNameRef("equalsSafe", left), right)
        return if (isNegated) JsAstUtils.not(invokeEquals) else invokeEquals
    }
}