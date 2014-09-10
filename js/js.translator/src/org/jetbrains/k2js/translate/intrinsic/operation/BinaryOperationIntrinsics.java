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

import com.google.common.collect.Lists;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.psi.JetBinaryExpression;
import org.jetbrains.k2js.translate.context.TranslationContext;

import java.util.List;
import java.util.Map;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getCallableDescriptorForOperationExpression;

public final class BinaryOperationIntrinsics {

    @NotNull
    private final Map<CallableDescriptor, BinaryOperationIntrinsic> intrinsicCache = new THashMap<CallableDescriptor, BinaryOperationIntrinsic>();

    @NotNull
    private final List<BinaryOperationIntrinsicFactory> factories = Lists.newArrayList();

    public BinaryOperationIntrinsics() {
        registerFactories();
    }

    private void registerFactories() {
        register(LongCompareToBOIF.INSTANCE$);
        register(EqualsBOIF.INSTANCE$);
        register(CompareToBOIF.INSTANCE$);
    }

    private void register(@NotNull BinaryOperationIntrinsicFactory instance) {
        factories.add(instance);
    }

    @NotNull
    public BinaryOperationIntrinsic getIntrinsic(@NotNull JetBinaryExpression expression, @NotNull TranslationContext context) {
        CallableDescriptor descriptor = getCallableDescriptorForOperationExpression(context.bindingContext(), expression);
        if (descriptor == null) {
            return BinaryOperationIntrinsic.NO_INTRINSIC;
        }

        BinaryOperationIntrinsic intrinsic = lookUpCache(descriptor);
        if (intrinsic != null) {
            return intrinsic;
        }
        intrinsic = computeAndCacheIntrinsic(expression, context);
        return intrinsic;
    }

    @Nullable
    private BinaryOperationIntrinsic lookUpCache(@NotNull CallableDescriptor descriptor) {
        return intrinsicCache.get(descriptor);
    }

    @NotNull
    private BinaryOperationIntrinsic computeAndCacheIntrinsic(@NotNull JetBinaryExpression expression, @NotNull TranslationContext context) {
        BinaryOperationIntrinsic result = computeIntrinsic(expression, context);
        CallableDescriptor descriptor = getCallableDescriptorForOperationExpression(context.bindingContext(), expression);
        intrinsicCache.put(descriptor, result);
        return result;
    }

    @NotNull
    private BinaryOperationIntrinsic computeIntrinsic(@NotNull JetBinaryExpression expression, @NotNull TranslationContext context) {
        for (BinaryOperationIntrinsicFactory factory : factories) {
            BinaryOperationIntrinsic intrinsic = factory.getIntrinsic(expression, context);
            if (intrinsic != null) {
                return intrinsic;
            }
        }
        return BinaryOperationIntrinsic.NO_INTRINSIC;
    }
}