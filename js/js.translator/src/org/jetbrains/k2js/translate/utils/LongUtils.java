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

package org.jetbrains.k2js.translate.utils;

import com.google.dart.compiler.backend.js.ast.*;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.DescriptorPredicate;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder;

import java.util.List;

import static org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder.pattern;
import static org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder.Pattern.*;

public class LongUtils {
    private LongUtils() {
    }

    @NotNull
    public static final DescriptorPredicate LONG_EQUALS_ANY = pattern(LONG.dot(EQUALS));

    @NotNull
    public static final DescriptorPredicate FLOATING_POINT_COMPARE_TO_LONG = pattern(DOUBLE.or(FLOAT).dot(COMPARE_TO).withArguments(LONG));

    @NotNull
    public static final DescriptorPredicate LONG_COMPARE_TO_FLOATING_POINT = pattern("Long.compareTo(Double|Float)");

    @NotNull
    public static final DescriptorPredicate INTEGER_COMPARE_TO_LONG = pattern("Int|Short|Byte.compareTo(Long)");

    @NotNull
    public static final DescriptorPredicate LONG_COMPARE_TO_INTEGER = pattern("Long.compareTo(Int|Short|Byte)");

    @NotNull
    public static final DescriptorPredicate LONG_COMPARE_TO_LONG = pattern("Long.compareTo(Long)");

    @NotNull
    public static final DescriptorPredicate LONG_BINARY_LONG =
            pattern(LONG.dot(COMPARE_TO.or(RANGE_TO).or(PLUS).or(MINUS).or(TIMES).or(DIV).or(MOD).or(AND).or(OR).or(XOR)).withArguments(LONG));

    @NotNull
    public static final DescriptorPredicate LONG_UNARY = pattern("Long.plus|minus|inc|dec|inv()");

    @NotNull
    public static final DescriptorPredicate LONG_BIT_SHIFTS = pattern("Long.shl|shr|ushr(Int)");

    @NotNull
    public static final DescriptorPredicate LONG_BINARY_INTEGER = pattern("Long.compareTo|rangeTo|plus|minus|times|div|mod(Int|Short|Byte)");

    @NotNull
    public static final DescriptorPredicate LONG_BINARY_FLOATING_POINT = pattern("Long.compareTo|rangeTo|plus|minus|times|div|mod(Double|Float)");

    @NotNull
    public static final DescriptorPredicate INTEGER_BINARY_LONG = pattern("Int|Short|Byte.compareTo|rangeTo|plus|minus|times|div|mod(Long)");

    @NotNull
    public static final DescriptorPredicate FLOATING_POINT_BINARY_LONG = pattern("Double|Float.compareTo|rangeTo|plus|minus|times|div|mod(Long)");

    @NotNull
    public static final JsNameRef kotlinLongNameRef = new JsNameRef("Long", Namer.KOTLIN_OBJECT_REF);

    public static JsExpression newLong(long value, @NotNull TranslationContext context) {
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            int low = (int) value;
            int high = (int) (value >> 32);
            List<JsExpression> args = new SmartList<JsExpression>();
            args.add(context.program().getNumberLiteral(low));
            args.add(context.program().getNumberLiteral(high));
            return new JsNew(kotlinLongNameRef, args);
        }
        else {
            return fromInt(context.program().getNumberLiteral((int) value));
        }
    }

    @NotNull
    public static JsBinaryOperation jsInstanceOfLong(@NotNull JsExpression expression) {
        return JsAstUtils.instanceOf(expression, kotlinLongNameRef);
    }

    @NotNull
    public static JsExpression rangeTo(@NotNull JsExpression rangeStart, @NotNull JsExpression rangeEnd) {
        JsNameRef expr = new JsNameRef("LongRange", Namer.KOTLIN_NAME);
        JsNew numberRangeConstructorInvocation = new JsNew(expr);
        JsAstUtils.setArguments(numberRangeConstructorInvocation, rangeStart, rangeEnd);
        return numberRangeConstructorInvocation;
    }

    @NotNull
    public static JsExpression fromInt(@NotNull JsExpression expression) {
        return new JsInvocation(new JsNameRef("fromInt", kotlinLongNameRef), expression);
    }

    @NotNull
    public static JsExpression fromNumber(@NotNull JsExpression expression) {
        return new JsInvocation(new JsNameRef("fromNumber", kotlinLongNameRef), expression);
    }

    @NotNull
    public static JsExpression equalsSafe(@NotNull JsExpression left, @NotNull JsExpression right) {
        return new JsInvocation(new JsNameRef("equalsSafe", left), right);
    }

    @NotNull
    public static JsExpression toInt(@NotNull JsExpression expression) {
        return new JsInvocation(new JsNameRef("toInt", expression));
    }

    @NotNull
    public static JsExpression toNumber(@NotNull JsExpression expression) {
        return new JsInvocation(new JsNameRef("toNumber", expression));
    }

    @NotNull
    public static JsExpression negate(@NotNull JsExpression expression) {
        return new JsInvocation(new JsNameRef("negate", expression));
    }

    @NotNull
    public static JsExpression inc(@NotNull JsExpression expression) {
        return new JsInvocation(new JsNameRef("inc", expression));
    }

    @NotNull
    public static JsExpression dec(@NotNull JsExpression expression) {
        return new JsInvocation(new JsNameRef("dec", expression));
    }

    @NotNull
    public static JsExpression inv(@NotNull JsExpression expression) {
        return new JsInvocation(new JsNameRef("not", expression));
    }

    @NotNull
    public static JsExpression shl(@NotNull JsExpression left, @NotNull JsExpression right) {
        return new JsInvocation(new JsNameRef("shiftLeft", left), right);
    }

    @NotNull
    public static JsExpression shr(@NotNull JsExpression left, @NotNull JsExpression right) {
        return new JsInvocation(new JsNameRef("shiftRight", left), right);
    }

    @NotNull
    public static JsExpression ushr(@NotNull JsExpression left, @NotNull JsExpression right) {
        return new JsInvocation(new JsNameRef("shiftRightUnsigned", left), right);
    }

    @NotNull
    public static JsExpression and(@NotNull JsExpression left, @NotNull JsExpression right) {
        return new JsInvocation(new JsNameRef("and", left), right);
    }

    @NotNull
    public static JsExpression or(@NotNull JsExpression left, @NotNull JsExpression right) {
        return new JsInvocation(new JsNameRef("or", left), right);
    }

    @NotNull
    public static JsExpression xor(@NotNull JsExpression left, @NotNull JsExpression right) {
        return new JsInvocation(new JsNameRef("xor", left), right);
    }

    @NotNull
    public static JsExpression sum(@NotNull JsExpression left, @NotNull JsExpression right) {
        return new JsInvocation(new JsNameRef("add", left), right);
    }

    @NotNull
    public static JsExpression subtract(@NotNull JsExpression left, @NotNull JsExpression right) {
        return new JsInvocation(new JsNameRef("subtract", left), right);
    }

    @NotNull
    public static JsExpression mul(@NotNull JsExpression left, @NotNull JsExpression right) {
        return new JsInvocation(new JsNameRef("multiply", left), right);
    }

    @NotNull
    public static JsExpression div(@NotNull JsExpression left, @NotNull JsExpression right) {
        return new JsInvocation(new JsNameRef("div", left), right);
    }

    @NotNull
    public static JsExpression mod(@NotNull JsExpression left, @NotNull JsExpression right) {
        return new JsInvocation(new JsNameRef("modulo", left), right);
    }

    @NotNull
    public static JsExpression compare(@NotNull JsExpression left, @NotNull JsExpression right) {
        return new JsInvocation(new JsNameRef("compare", left), right);
    }

}
