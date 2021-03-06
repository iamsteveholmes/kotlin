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

package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.Method;
import org.jetbrains.jet.descriptors.serialization.*;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedPropertyDescriptor;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedSimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.kotlin.SignatureDeserializer;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Arrays;

import static org.jetbrains.jet.codegen.JvmSerializationBindings.*;

public class JavaSerializerExtension extends SerializerExtension {
    private final JvmSerializationBindings bindings;

    public JavaSerializerExtension(@NotNull JvmSerializationBindings bindings) {
        this.bindings = bindings;
    }

    @Override
    public void serializeCallable(
            @NotNull CallableMemberDescriptor callable,
            @NotNull ProtoBuf.Callable.Builder proto,
            @NotNull NameTable nameTable
    ) {
        saveSignature(callable, proto, nameTable);
        saveImplClassName(callable, proto, nameTable);
    }

    @Override
    public void serializeValueParameter(
            @NotNull ValueParameterDescriptor descriptor,
            @NotNull ProtoBuf.Callable.ValueParameter.Builder proto,
            @NotNull NameTable nameTable
    ) {
        Integer index = bindings.get(INDEX_FOR_VALUE_PARAMETER, descriptor);
        if (index != null) {
            proto.setExtension(JavaProtoBuf.index, index);
        }
    }

    private void saveSignature(
            @NotNull CallableMemberDescriptor callable,
            @NotNull ProtoBuf.Callable.Builder proto,
            @NotNull NameTable nameTable
    ) {
        SignatureSerializer signatureSerializer = new SignatureSerializer(nameTable);
        if (callable instanceof FunctionDescriptor) {
            JavaProtoBuf.JavaMethodSignature signature;
            if (callable instanceof DeserializedSimpleFunctionDescriptor) {
                DeserializedSimpleFunctionDescriptor deserialized = (DeserializedSimpleFunctionDescriptor) callable;
                signature = signatureSerializer.copyMethodSignature(
                        deserialized.getProto().getExtension(JavaProtoBuf.methodSignature), deserialized.getNameResolver());
            }
            else {
                Method method = bindings.get(METHOD_FOR_FUNCTION, (FunctionDescriptor) callable);
                signature = method != null ? signatureSerializer.methodSignature(method) : null;
            }
            if (signature != null) {
                proto.setExtension(JavaProtoBuf.methodSignature, signature);
            }
        }
        else if (callable instanceof PropertyDescriptor) {
            PropertyDescriptor property = (PropertyDescriptor) callable;

            PropertyGetterDescriptor getter = property.getGetter();
            PropertySetterDescriptor setter = property.getSetter();
            Method getterMethod = getter == null ? null : bindings.get(METHOD_FOR_FUNCTION, getter);
            Method setterMethod = setter == null ? null : bindings.get(METHOD_FOR_FUNCTION, setter);

            Pair<Type, String> field = bindings.get(FIELD_FOR_PROPERTY, property);
            Type fieldType;
            String fieldName;
            boolean isStaticInOuter;
            Method syntheticMethod;
            if (field != null) {
                fieldType = field.first;
                fieldName = field.second;
                isStaticInOuter = bindings.get(STATIC_FIELD_IN_OUTER_CLASS, property);
                syntheticMethod = null;
            }
            else {
                fieldType = null;
                fieldName = null;
                isStaticInOuter = false;
                syntheticMethod = bindings.get(SYNTHETIC_METHOD_FOR_PROPERTY, property);
            }

            JavaProtoBuf.JavaPropertySignature signature;
            if (callable instanceof DeserializedPropertyDescriptor) {
                DeserializedPropertyDescriptor deserializedCallable = (DeserializedPropertyDescriptor) callable;
                signature = signatureSerializer.copyPropertySignature(
                        deserializedCallable.getProto().getExtension(JavaProtoBuf.propertySignature),
                        deserializedCallable.getNameResolver()
                );
            }
            else {
                signature = signatureSerializer
                        .propertySignature(fieldType, fieldName, isStaticInOuter, syntheticMethod, getterMethod, setterMethod);
            }
            proto.setExtension(JavaProtoBuf.propertySignature, signature);
        }
    }

    private void saveImplClassName(
            @NotNull CallableMemberDescriptor callable,
            @NotNull ProtoBuf.Callable.Builder proto,
            @NotNull NameTable nameTable
    ) {
        String name = bindings.get(IMPL_CLASS_NAME_FOR_CALLABLE, callable);
        if (name != null) {
            proto.setExtension(JavaProtoBuf.implClassName, nameTable.getSimpleNameIndex(Name.identifier(name)));
        }
    }

    private static class SignatureSerializer {
        private final NameTable nameTable;

        public SignatureSerializer(@NotNull NameTable nameTable) {
            this.nameTable = nameTable;
        }

        @NotNull
        public JavaProtoBuf.JavaMethodSignature copyMethodSignature(
                @NotNull JavaProtoBuf.JavaMethodSignature signature,
                @NotNull NameResolver nameResolver
        ) {
            String method = new SignatureDeserializer(nameResolver).methodSignatureString(signature);
            return methodSignature(getAsmMethod(method));
        }

        @NotNull
        public JavaProtoBuf.JavaMethodSignature methodSignature(@NotNull Method method) {
            JavaProtoBuf.JavaMethodSignature.Builder signature = JavaProtoBuf.JavaMethodSignature.newBuilder();

            signature.setName(nameTable.getSimpleNameIndex(Name.guess(method.getName())));

            signature.setReturnType(type(method.getReturnType()));

            for (Type type : method.getArgumentTypes()) {
                signature.addParameterType(type(type));
            }

            return signature.build();
        }

        @NotNull
        public JavaProtoBuf.JavaPropertySignature copyPropertySignature(
                @NotNull JavaProtoBuf.JavaPropertySignature signature,
                @NotNull NameResolver nameResolver
        ) {
            Type fieldType;
            String fieldName;
            boolean isStaticInOuter;
            SignatureDeserializer signatureDeserializer = new SignatureDeserializer(nameResolver);
            if (signature.hasField()) {
                JavaProtoBuf.JavaFieldSignature field = signature.getField();
                fieldType = Type.getType(signatureDeserializer.typeDescriptor(field.getType()));
                fieldName = nameResolver.getName(field.getName()).asString();
                isStaticInOuter = field.getIsStaticInOuter();
            }
            else {
                fieldType = null;
                fieldName = null;
                isStaticInOuter = false;
            }

            Method syntheticMethod = signature.hasSyntheticMethod()
                    ? getAsmMethod(signatureDeserializer.methodSignatureString(signature.getSyntheticMethod()))
                    : null;

            Method getter = signature.hasGetter() ? getAsmMethod(signatureDeserializer.methodSignatureString(signature.getGetter())) : null;
            Method setter = signature.hasSetter() ? getAsmMethod(signatureDeserializer.methodSignatureString(signature.getSetter())) : null;

            return propertySignature(fieldType, fieldName, isStaticInOuter, syntheticMethod, getter, setter);
        }

        @NotNull
        public JavaProtoBuf.JavaPropertySignature propertySignature(
                @Nullable Type fieldType,
                @Nullable String fieldName,
                boolean isStaticInOuter,
                @Nullable Method syntheticMethod,
                @Nullable Method getter,
                @Nullable Method setter
        ) {
            JavaProtoBuf.JavaPropertySignature.Builder signature = JavaProtoBuf.JavaPropertySignature.newBuilder();

            if (fieldType != null) {
                assert fieldName != null : "Field name shouldn't be null when there's a field type: " + fieldType;
                signature.setField(fieldSignature(fieldType, fieldName, isStaticInOuter));
            }

            if (syntheticMethod != null) {
                signature.setSyntheticMethod(methodSignature(syntheticMethod));
            }

            if (getter != null) {
                signature.setGetter(methodSignature(getter));
            }
            if (setter != null) {
                signature.setSetter(methodSignature(setter));
            }

            return signature.build();
        }

        @NotNull
        public JavaProtoBuf.JavaFieldSignature fieldSignature(@NotNull Type type, @NotNull String name, boolean isStaticInOuter) {
            JavaProtoBuf.JavaFieldSignature.Builder signature = JavaProtoBuf.JavaFieldSignature.newBuilder();
            signature.setName(nameTable.getSimpleNameIndex(Name.guess(name)));
            signature.setType(type(type));
            if (isStaticInOuter) {
                signature.setIsStaticInOuter(true);
            }
            return signature.build();
        }

        @NotNull
        public JavaProtoBuf.JavaType type(@NotNull Type givenType) {
            JavaProtoBuf.JavaType.Builder builder = JavaProtoBuf.JavaType.newBuilder();

            int arrayDimension = 0;
            Type type = givenType;
            while (type.getSort() == Type.ARRAY) {
                arrayDimension++;
                type = type.getElementType();
            }
            if (arrayDimension != 0) {
                builder.setArrayDimension(arrayDimension);
            }

            if (type.getSort() == Type.OBJECT) {
                FqName fqName = internalNameToFqName(type.getInternalName());
                builder.setClassFqName(nameTable.getFqNameIndex(fqName));
            }
            else {
                builder.setPrimitiveType(JavaProtoBuf.JavaType.PrimitiveType.valueOf(type.getSort()));
            }

            return builder.build();
        }

        @NotNull
        private static FqName internalNameToFqName(@NotNull String internalName) {
            return FqName.fromSegments(Arrays.asList(internalName.split("/")));
        }
    }

    @NotNull
    private static Method getAsmMethod(@NotNull String nameAndDesc) {
        int indexOf = nameAndDesc.indexOf('(');
        return new Method(nameAndDesc.substring(0, indexOf), nameAndDesc.substring(indexOf));
    }
}
