/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.intrinsic.operation;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.k2js.translate.context.TranslationContext;

public abstract class BinaryOperationIntrinsic {

    public static final BinaryOperationIntrinsic NO_INTRINSIC = new BinaryOperationIntrinsic() {
        @Override
        public boolean exists() {
            return false;
        }

        @NotNull
        @Override
        public JsExpression apply(
                @NotNull JetBinaryExpression expression,
                @NotNull JsExpression left,
                @NotNull JsExpression right,
                @NotNull TranslationContext context
        ) {
            throw new UnsupportedOperationException("BinaryOperationIntrinsic#NO_INTRINSIC_#apply");
        }
    };

    @NotNull
    public abstract JsExpression apply(@NotNull JetBinaryExpression expression, @NotNull JsExpression left,
            @NotNull JsExpression right, @NotNull TranslationContext context);

    public boolean exists() {
        return true;
    }
}
