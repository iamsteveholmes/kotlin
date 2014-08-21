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

package org.jetbrains.jet.codegen.optimization

import org.jetbrains.jet.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.jet.codegen.optimization.common.OptimizationBasicInterpreter
import org.jetbrains.jet.codegen.inline.InlineCodegenUtil

import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode
import org.jetbrains.org.objectweb.asm.Type

import java.util.Stack

class StoreStackBeforeInlineMethodTransformer(delegate: MethodTransformer?) : MethodTransformer(delegate) {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        val frames = MethodTransformer.analyze(internalClassName, methodNode, OptimizationBasicInterpreter())
        if (needToProcess(methodNode, frames)) {
            process(methodNode, frames)
        }

        removeInlineMarkers(methodNode)

        super<MethodTransformer>.transform(internalClassName, methodNode)
    }
}

fun needToProcess(node: MethodNode, frames: Array<Frame<BasicValue>>): Boolean {
    return isBalancedInlineMarkers(node, frames) &&
           node.instructions.toArray().any { isInlineMarker(it) }
}

fun isBalancedInlineMarkers(node: MethodNode, frames: Array<Frame<BasicValue>>) : Boolean {
    val insns = node.instructions.toArray()
    var balance = 0

    for (i in insns.indices) {
        val insn = insns[i]
        val frame = frames[i]

        // inline marker is not available
        if (isInlineMarker(insn) && frame == null) return false

        if (isBeforeInlineMarker(insn)) {
            balance += 1
        }
        else if(isAfterInlineMarker(insn)) {
            balance -= 1
        }

        if (balance < 0) return false
    }

    return balance == 0
}

fun isBeforeInlineMarker(insn: AbstractInsnNode): Boolean {
    return isInlineMarker(insn) &&
           (insn as MethodInsnNode).name == InlineCodegenUtil.INLINE_MARKER_BEFORE_METHOD_NAME
}

fun isAfterInlineMarker(insn: AbstractInsnNode): Boolean {
    return isInlineMarker(insn) &&
           (insn as MethodInsnNode).name == InlineCodegenUtil.INLINE_MARKER_AFTER_METHOD_NAME
}

fun isInlineMarker(insn: AbstractInsnNode): Boolean {
    return insn.getOpcode() == Opcodes.INVOKESTATIC &&
           insn is MethodInsnNode &&
           insn.owner == InlineCodegenUtil.INLINE_MARKER_CLASS_NAME
}

fun process(methodNode: MethodNode, frames: Array<Frame<BasicValue>>) {
    val insns = methodNode.instructions.toArray()

    val storedValuesDescriptorsStack = Stack<StoredStackValuesDescriptor>()
    var firstAvailableVarIndex = methodNode.maxLocals
    var storedValuesCount = 0

    for (i in insns.indices) {
        val frame = frames[i]
        val insn = insns[i]

        if (isBeforeInlineMarker(insn)) {
            val desc = storeStackValues(methodNode, frame, insn, firstAvailableVarIndex, storedValuesCount)

            firstAvailableVarIndex += desc.storedStackSize
            storedValuesCount += desc.storedValuesCount
            storedValuesDescriptorsStack.push(desc)
        }
        else if (isAfterInlineMarker(insn)) {
            val desc = storedValuesDescriptorsStack.pop() ?:
                       throw AssertionError("should be non null becase markers are balanced")

            loadStackValues(methodNode, frame, insn, desc)
            firstAvailableVarIndex -= desc.storedStackSize
            storedValuesCount -= desc.storedValuesCount
        }
    }
}

class StoredStackValuesDescriptor(
        val storedValues: List<BasicValue>,
        val firstVariableIndex: Int,
        val storedStackSize: Int,
        val storedValuesCountBefore: Int
) {
    val nextFreeVarIndex : Int get() = firstVariableIndex + storedStackSize
    val storedValuesCount : Int get() = storedValues.size
    val isThereStoredSomething : Boolean get() = storedValuesCount > 0
    val stackSizeBeforeInline : Int get() = storedValuesCount + storedValuesCountBefore
}

fun removeInlineMarkers(node: MethodNode) {
    for (insn in node.instructions.toArray()) {
        if (isInlineMarker(insn)) {
            node.instructions.remove(insn)
        }
    }
}

fun storeStackValues(
        node: MethodNode,
        frame: Frame<BasicValue>,
        beforeInlineMarker: AbstractInsnNode,
        firstAvailableVarIndex: Int,
        alreadyStoredValuesCount: Int
) : StoredStackValuesDescriptor {
    val insns = node.instructions
    var stackSize = 0

    val values = frame.requireNonNullTypeStackValuesStartingFrom(alreadyStoredValuesCount)

    for (value in values.reverse()) {
        insns.insertBefore(
                beforeInlineMarker,
                VarInsnNode(
                        value.getType()!!.getOpcode(Opcodes.ISTORE),
                        firstAvailableVarIndex + stackSize
                )
        )
        stackSize += value.getSize()
    }

    node.updateMaxLocals(firstAvailableVarIndex + stackSize)

    return StoredStackValuesDescriptor(values, firstAvailableVarIndex, stackSize, alreadyStoredValuesCount)
}

fun loadStackValues(
        node: MethodNode,
        frame: Frame<BasicValue>,
        afterInlineMarker: AbstractInsnNode,
        desc: StoredStackValuesDescriptor
) {
    if (!desc.isThereStoredSomething) return

    val insns = node.instructions
    var returnValueVarIndex = -1
    var returnType : Type? = null

    if (frame.getStackSize() != desc.stackSizeBeforeInline) {
        // only returned value
        assert(
                (frame.getStackSize() - desc.stackSizeBeforeInline) == 1,
                "Stack sized should not differ by more than 1 (returned value)"
        )

        returnValueVarIndex = desc.nextFreeVarIndex
        returnType = frame.getStack(frame.getStackSize() - 1)!!.getType()
        node.updateMaxLocals(returnValueVarIndex + returnType!!.getSize())

        insns.insertBefore(
                afterInlineMarker,
                VarInsnNode(returnType!!.getOpcode(Opcodes.ISTORE), returnValueVarIndex)
        )
    }

    var currentVarIndex = desc.firstVariableIndex + desc.storedStackSize

    for (value in desc.storedValues) {
        currentVarIndex -= value.getSize()
        insns.insertBefore(
                afterInlineMarker,
                VarInsnNode(
                        value.getType()!!.getOpcode(Opcodes.ILOAD),
                        currentVarIndex
                )
        )
    }

    if (returnValueVarIndex != -1) {
        insns.insertBefore(
                afterInlineMarker,
                VarInsnNode(returnType!!.getOpcode(Opcodes.ILOAD), returnValueVarIndex)
        )
    }
}

fun <V : BasicValue> Frame<V>.requireNonNullTypeStackValuesStartingFrom(from: Int) : List<V> {
    val nonNulls = IntRange(from, getStackSize() - 1).map { getStack(it) }.requireNoNulls()
    assert(nonNulls.all { it.getType() != null }, "non null value type required")

    return nonNulls
}

fun MethodNode.updateMaxLocals(newMaxLocals: Int) {
    maxLocals = Math.max(maxLocals, newMaxLocals)
}
