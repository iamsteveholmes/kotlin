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

package org.jetbrains.k2js.translate.intrinsic.functions.patterns;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.OverrideResolver;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.Arrays;
import java.util.List;

public final class PatternBuilder {

    @NotNull
    private static final NamePredicate KOTLIN_NAME_PREDICATE = new NamePredicate("kotlin");

    @NotNull
    private static final Name KOTLIN_NAME = Name.identifier(Namer.KOTLIN_LOWER_NAME);

    private PatternBuilder() {
    }

    public static class Pattern  {
        @NotNull
        public static final Pattern DOUBLE = new Pattern("Double");
        @NotNull
        public static final Pattern FLOAT = new Pattern("Float");
        @NotNull
        public static final Pattern LONG = new Pattern("Long");
        @NotNull
        public static final Pattern INT = new Pattern("Int");
        @NotNull
        public static final Pattern SHORT = new Pattern("Short");
        @NotNull
        public static final Pattern BYTE = new Pattern("Byte");
        @NotNull
        public static final Pattern EQUALS = new Pattern("equals");
        @NotNull
        public static final Pattern COMPARE_TO = new Pattern("compareTo");
        @NotNull
        public static final Pattern RANGE_TO = new Pattern("rangeTo");
        @NotNull
        public static final Pattern PLUS = new Pattern("plus");
        @NotNull
        public static final Pattern MINUS = new Pattern("minus");
        @NotNull
        public static final Pattern DIV = new Pattern("div");
        @NotNull
        public static final Pattern MOD = new Pattern("mod");
        @NotNull
        public static final Pattern TIMES = new Pattern("times");
        @NotNull
        public static final Pattern AND = new Pattern("and");
        @NotNull
        public static final Pattern OR = new Pattern("or");
        @NotNull
        public static final Pattern XOR = new Pattern("xor");

        @NotNull
        private final String value;

        Pattern(@NotNull String value) {
            this.value = value;
        }

        @Override
        @NotNull
        public String toString() {
            return value;
        }

        @NotNull
        public Pattern or(@NotNull Pattern other) {
            return new Pattern(value + "|" + other);
        }

        @NotNull
        public Pattern dot(@NotNull Pattern other) {
            return new Pattern(value + "." + other);
        }

        @NotNull
        public Pattern withArguments(@NotNull Pattern other) {
            return new Pattern(value + "(" + other + ")");
        }
    }

    @NotNull
    public static DescriptorPredicate pattern(@NotNull NamePredicate checker, @NotNull String stringWithPattern) {
        List<NamePredicate> checkers = Lists.newArrayList(checker);
        String namePatternString = getNamePatternFromString(stringWithPattern);
        checkers.addAll(parseStringAsCheckerList(namePatternString));
        String argumentsString = getArgumentsPatternFromString(stringWithPattern);
        List<NamePredicate> argumentCheckers = argumentsString != null ? parseStringAsArgumentCheckerList(argumentsString) : null;
        return pattern(checkers, argumentCheckers);
    }

    @NotNull
    public static DescriptorPredicate pattern(@NotNull String stringWithPattern, @NotNull NamePredicate checker) {
        String namePatternString = getNamePatternFromString(stringWithPattern);
        List<NamePredicate> checkers = Lists.newArrayList(parseStringAsCheckerList(namePatternString));
        checkers.add(checker);
        return pattern(checkers);
    }

    @NotNull
    public static DescriptorPredicate pattern(@NotNull String string) {
        String namePatternString = getNamePatternFromString(string);
        List<NamePredicate> checkers = parseStringAsCheckerList(namePatternString);
        String argumentsString = getArgumentsPatternFromString(string);
        List<NamePredicate> argumentCheckers = argumentsString != null ? parseStringAsArgumentCheckerList(argumentsString) : null;
        return pattern(checkers, argumentCheckers);
    }

    @NotNull
    public static DescriptorPredicate pattern(@NotNull Pattern p) {
        return pattern(p.toString());
    }

    @NotNull
    private static List<NamePredicate> parseStringAsCheckerList(@NotNull String stringWithPattern) {
        String[] subPatterns = stringWithPattern.split("\\.");
        List<NamePredicate> checkers = Lists.newArrayList();
        for (String subPattern : subPatterns) {
            String[] validNames = subPattern.split("\\|");
            checkers.add(new NamePredicate(validNames));
        }
        return checkers;
    }

    @NotNull
    private static List<NamePredicate> parseStringAsArgumentCheckerList(@NotNull String stringWithPattern) {
        List<NamePredicate> checkers = Lists.newArrayList();
        if (stringWithPattern.isEmpty()) {
            return checkers;
        }

        String[] subPatterns = stringWithPattern.split("\\,");
        for (String subPattern : subPatterns) {
            String[] validNames = subPattern.split("\\|");
            checkers.add(new NamePredicate(validNames));
        }
        return checkers;
    }

    @NotNull
    private static String getNamePatternFromString(@NotNull String stringWithPattern) {
        int left = stringWithPattern.indexOf("(");
        if (left < 0) {
            return stringWithPattern;
        }
        else {
            return stringWithPattern.substring(0, left);
        }
    }

    @Nullable
    private static String getArgumentsPatternFromString(@NotNull String stringWithPattern) {
        int left = stringWithPattern.indexOf("(");
        if (left < 0) {
            return null;
        }
        else {
            int right = stringWithPattern.indexOf(")");
            assert right == stringWithPattern.length() - 1;
            return stringWithPattern.substring(left + 1, right);
        }
    }

    @NotNull
    private static DescriptorPredicate pattern(@NotNull List<NamePredicate> checkers) {
        return pattern(checkers, null);
    }

    @NotNull
    private static DescriptorPredicate pattern(@NotNull List<NamePredicate> checkers, @Nullable List<NamePredicate> arguments) {
        assert !checkers.isEmpty();
        final List<NamePredicate> checkersWithPrefixChecker = Lists.newArrayList();
        if (!checkers.get(0).apply(KOTLIN_NAME)) {
            checkersWithPrefixChecker.add(KOTLIN_NAME_PREDICATE);
        }

        checkersWithPrefixChecker.addAll(checkers);

        assert checkersWithPrefixChecker.size() > 1;

        final List<NamePredicate> argumentCheckers = arguments != null ? Lists.newArrayList(arguments) : null;

        return new DescriptorPredicate() {
            @Override
            public boolean apply(@Nullable FunctionDescriptor descriptor) {
                assert descriptor != null : "argument for DescriptorPredicate.apply should not be null, checkers=" + checkersWithPrefixChecker;
                //TODO: no need to wrap if we check beforehand
                try {
                    return doApply(descriptor);
                }
                catch (IllegalArgumentException e) {
                    return false;
                }
            }

            private boolean doApply(@NotNull FunctionDescriptor descriptor) {
                List<Name> nameParts = DescriptorUtils.getFqName(descriptor).pathSegments();
                if (nameParts.size() != checkersWithPrefixChecker.size()) {
                    return false;
                }
                if (!allNamePartsValid(nameParts)) {
                    return false;
                }
                if (argumentCheckers != null) {
                    List<ValueParameterDescriptor> valueParameterDescriptors = descriptor.getValueParameters();
                    if (valueParameterDescriptors.size() != argumentCheckers.size()) {
                        return false;
                    }
                    return allArgumentsValid(valueParameterDescriptors);
                }
                return true;
            }

            private boolean allNamePartsValid(@NotNull List<Name> nameParts) {
                for (int i = 0; i < nameParts.size(); ++i) {
                    Name namePart = nameParts.get(i);
                    NamePredicate correspondingPredicate = checkersWithPrefixChecker.get(i);
                    if (!correspondingPredicate.apply(namePart)) {
                        return false;
                    }
                }
                return true;
            }

            private boolean allArgumentsValid(List<ValueParameterDescriptor> valueParameterDescriptors) {
                assert argumentCheckers != null;
                for (int i = 0; i < valueParameterDescriptors.size(); i++) {
                    ValueParameterDescriptor valueParameterDescriptor = valueParameterDescriptors.get(i);
                    Name name = JsDescriptorUtils.getNameIfStandardType(valueParameterDescriptor.getType());
                    NamePredicate namePredicate = argumentCheckers.get(i);
                    if (name == null || !namePredicate.apply(name)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    @NotNull
    public static DescriptorPredicate pattern(@NotNull NamePredicate... checkers) {
        return pattern(Arrays.asList(checkers));
    }

    @NotNull
    public static DescriptorPredicateImpl pattern(@NotNull String... names) {
        return new DescriptorPredicateImpl(names);
    }

    public static class DescriptorPredicateImpl implements DescriptorPredicate {
        private final String[] names;

        private String receiverFqName;

        private boolean checkOverridden;

        public DescriptorPredicateImpl(String... names) {
            this.names = names;
        }

        public DescriptorPredicateImpl isExtensionOf(String receiverFqName) {
            this.receiverFqName = receiverFqName;
            return this;
        }

        public DescriptorPredicateImpl checkOverridden() {
            this.checkOverridden = true;
            return this;
        }

        private boolean matches(@NotNull CallableDescriptor callable) {
            DeclarationDescriptor descriptor = callable;
            int nameIndex = names.length - 1;
            while (true) {
                if (nameIndex == -1) {
                    return false;
                }

                if (!descriptor.getName().asString().equals(names[nameIndex])) {
                    return false;
                }

                nameIndex--;
                descriptor = descriptor.getContainingDeclaration();
                if (descriptor instanceof PackageFragmentDescriptor) {
                    return nameIndex == 0 && names[0].equals(((PackageFragmentDescriptor) descriptor).getFqName().asString());
                }
            }
        }

        @Override
        public boolean apply(@Nullable FunctionDescriptor functionDescriptor) {
            assert functionDescriptor != null :
                    "argument for DescriptorPredicate.apply should not be null, receiverFqName=" + receiverFqName + " names=" + Arrays.asList(names);
            ReceiverParameterDescriptor actualReceiver = functionDescriptor.getReceiverParameter();
            if (actualReceiver != null) {
                if (receiverFqName == null) return false;

                String actualReceiverFqName = TranslationUtils.getJetTypeFqName(actualReceiver.getType());

                if (!actualReceiverFqName.equals(receiverFqName)) return false;
            }

            if (!(functionDescriptor.getContainingDeclaration() instanceof ClassDescriptor)) {
                return matches(functionDescriptor);
            }

            for (CallableMemberDescriptor real : OverrideResolver.getOverriddenDeclarations(functionDescriptor)) {
                if (matches(real)) {
                    return true;
                }
            }

            if (checkOverridden) {
                for (CallableDescriptor overridden : OverrideResolver.getAllOverriddenDescriptors(functionDescriptor)) {
                    if (matches(overridden)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }
}
