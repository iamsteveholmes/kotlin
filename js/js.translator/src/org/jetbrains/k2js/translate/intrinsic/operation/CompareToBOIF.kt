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
import org.jetbrains.jet.lang.psi.JetBinaryExpression
import org.jetbrains.jet.lang.types.expressions.OperatorConventions
import org.jetbrains.k2js.translate.context.TranslationContext
import org.jetbrains.k2js.translate.operation.OperatorTable
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils

import org.jetbrains.k2js.translate.utils.BindingUtils.getCallableDescriptorForOperationExpression
import org.jetbrains.k2js.translate.utils.PsiUtils.getOperationToken

public object CompareToBOIF : BinaryOperationIntrinsicFactory {

    private object CompareToIntrinsic : BinaryOperationIntrinsic() {
        override fun apply(expression: JetBinaryExpression, left: JsExpression, right: JsExpression, context: TranslationContext): JsExpression {
            val operator = OperatorTable.getBinaryOperator(getOperationToken(expression))
            return JsBinaryOperation(operator, left, right)
        }
    }

    override public fun getIntrinsic(expression: JetBinaryExpression, context: TranslationContext): BinaryOperationIntrinsic? {
        if (OperatorConventions.COMPARISON_OPERATIONS.contains(getOperationToken(expression))) {
            val descriptor = getCallableDescriptorForOperationExpression(context.bindingContext(), expression)
            if (descriptor != null && JsDescriptorUtils.isBuiltin(descriptor)) {
                return CompareToIntrinsic
            }
        }
        return null
    }
}
