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

package org.jetbrains.k2js.translate.intrinsic.functions.factories

import com.google.dart.compiler.backend.js.ast.*
import com.intellij.util.containers.hash.HashMap
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.k2js.translate.context.TranslationContext
import org.jetbrains.k2js.translate.intrinsic.functions.basic.FunctionIntrinsic
import org.jetbrains.k2js.translate.utils.JsAstUtils
import org.jetbrains.k2js.translate.utils.LongUtils
import org.jetbrains.k2js.translate.utils.LongUtils.*

public object LongOperationFIF : FunctionIntrinsicFactory {

    private val longUnaryOperations: Map<String, (receiver: JsExpression, context: TranslationContext) -> JsExpression> =
            mapOf(
                    "plus" to { (receiver, context) -> receiver },
                    "minus" to { (receiver, context) -> LongUtils.negate(receiver) },
                    "inc" to { (receiver, context) -> LongUtils.inc(receiver) },
                    "dec" to { (receiver, context) -> LongUtils.dec(receiver) },
                    "inv" to { (receiver, context) -> LongUtils.inv(receiver) }
            );

    private val longBinaryOperations: Map<String, (left: JsExpression, right: JsExpression, context: TranslationContext) -> JsExpression> =
            mapOf(
                    "equals" to { (left, right, context) -> LongUtils.equalsSafe(left, right) },
                    "compareTo" to { (left, right, context) -> LongUtils.compare(left, right) },
                    "rangeTo" to { (left, right, context) -> LongUtils.rangeTo(left, right) },

                    "plus" to { (left, right, context) -> LongUtils.sum(left, right) },
                    "minus" to { (left, right, context) -> LongUtils.subtract(left, right) },
                    "times" to { (left, right, context) -> LongUtils.mul(left, right) },
                    "div" to { (left, right, context) -> LongUtils.div(left, right) },
                    "mod" to { (left, right, context) -> LongUtils.mod(left, right) },

                    "shl" to { (left, right, context) -> LongUtils.shl(left, right) },
                    "shr" to { (left, right, context) -> LongUtils.shr(left, right) },
                    "ushr" to { (left, right, context) -> LongUtils.ushr(left, right) },
                    "and" to { (left, right, context) -> LongUtils.and(left, right) },
                    "or" to { (left, right, context) -> LongUtils.or(left, right) },
                    "xor" to { (left, right, context) -> LongUtils.xor(left, right) }
            );

    private val floatBinaryOperations: Map<String, (left: JsExpression, right: JsExpression, context: TranslationContext) -> JsExpression> =
            mapOf(
                    "compareTo" to { (left, right, context) -> JsAstUtils.compareTo(left, right) },
                    "rangeTo" to { (left, right, context) -> JsAstUtils.rangeTo(left, right) },

                    "plus" to { (left, right, context) -> JsAstUtils.sum(left, right) },
                    "minus" to { (left, right, context) -> JsAstUtils.subtract(left, right) },
                    "times" to { (left, right, context) -> JsAstUtils.mul(left, right) },
                    "div" to { (left, right, context) -> JsAstUtils.div(left, right) },
                    "mod" to { (left, right, context) -> JsAstUtils.mod(left, right) }
            );

    fun newUnaryIntrinsic(applyFun: (receiver: JsExpression, context: TranslationContext) -> JsExpression): FunctionIntrinsic =
            object : FunctionIntrinsic() {
                override fun apply(receiver: JsExpression?, arguments: List<JsExpression>, context: TranslationContext): JsExpression {
                    assert(receiver != null)
                    assert(arguments.size() == 0)
                    return applyFun(receiver!!, context)
                }
            }

    fun newBinaryIntrinsic(applyFun: (left: JsExpression, right: JsExpression, context: TranslationContext) -> JsExpression): FunctionIntrinsic =
            object : FunctionIntrinsic() {
                override fun apply(receiver: JsExpression?, arguments: List<JsExpression>, context: TranslationContext): JsExpression {
                    assert(receiver != null)
                    assert(arguments.size() == 1)
                    return applyFun(receiver!!, arguments.get(0), context)
                }
            }

    override fun getIntrinsic(descriptor: FunctionDescriptor): FunctionIntrinsic? {
        if (LONG_UNARY.apply(descriptor)) {
            val operationName = descriptor.getName().asString()
            val applyFun = longUnaryOperations[operationName]
            return if (applyFun == null) null else newUnaryIntrinsic(applyFun)
        }

        if (LONG_EQUALS_ANY.apply(descriptor) || LONG_BINARY_LONG.apply(descriptor) || LONG_BIT_SHIFTS.apply(descriptor)) {
            val operationName = descriptor.getName().asString()
            val applyFun = longBinaryOperations[operationName]
            return if (applyFun == null) null else newBinaryIntrinsic(applyFun)
        }

        if (INTEGER_BINARY_LONG.apply(descriptor)) {
            val operationName = descriptor.getName().asString()
            val applyFun = longBinaryOperations[operationName]
            return if (applyFun == null) null else newBinaryIntrinsic() {
                (left, right, context) ->
                    applyFun(LongUtils.fromInt(left), right, context)
            }
        }

        if (LONG_BINARY_INTEGER.apply(descriptor)) {
            val operationName = descriptor.getName().asString()
            val applyFun = longBinaryOperations[operationName]
            return if (applyFun == null) null else newBinaryIntrinsic() {
                (left, right, context) ->
                    applyFun(left, LongUtils.fromInt(right), context)
            }
        }

        if (FLOATING_POINT_BINARY_LONG.apply(descriptor)) {
            val operationName = descriptor.getName().asString()
            val applyFun = floatBinaryOperations[operationName]
            return if (applyFun == null) null else newBinaryIntrinsic() {
                (left, right, context) ->
                    applyFun(left, LongUtils.toNumber(right), context)
            }
        }

        if (LONG_BINARY_FLOATING_POINT.apply(descriptor)) {
            val operationName = descriptor.getName().asString()
            val applyFun = floatBinaryOperations[operationName]
            return if (applyFun == null) null else newBinaryIntrinsic() {
                (left, right, context) ->
                    applyFun(LongUtils.toNumber(left), right, context)
            }
        }

        return null;
    }
}