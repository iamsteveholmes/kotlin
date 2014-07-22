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

package org.jetbrains.jet.codegen.inline;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.AsmUtil;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.*;

import java.util.*;

import static org.jetbrains.jet.codegen.inline.InlineCodegenUtil.*;

public class InternalFinallyBlockInliner {

    private static class FinallyBlockInfo {

        final AbstractInsnNode startIns;

        final AbstractInsnNode endInsExclude;

        private FinallyBlockInfo(@NotNull AbstractInsnNode node, @NotNull AbstractInsnNode exclude) {
            startIns = node;
            endInsExclude = exclude;
        }
    }

    public static void processInlineFunFinallyBlocks(@NotNull MethodNode inlineFun, int lambdaFinallyBlocks) {
        new InternalFinallyBlockInliner(inlineFun, lambdaFinallyBlocks).processInlineFunFinallyBlocks();
    }

    @NotNull
    private final MethodNode inlineFun;

    private final List<TryCatchBlockNodeInfo> inlineFunTryBlockInfo = new ArrayList<TryCatchBlockNodeInfo>();

    private InternalFinallyBlockInliner(@NotNull MethodNode inlineFun, int lambdaFinallyBlocks) {
        this.inlineFun = inlineFun;
        int index = 0;
        for (TryCatchBlockNode block : inlineFun.tryCatchBlocks) {
            inlineFunTryBlockInfo.add(new TryCatchBlockNodeInfo(block, block.start, block.end, index++ < lambdaFinallyBlocks));
        }
        sortTryCatchBlock(inlineFun, inlineFunTryBlockInfo);
    }

    private void processInlineFunFinallyBlocks() {
        if (!hasFinallyBlocks()) return;

        HashMapOfList<LabelNode, TryCatchBlockNodeInfo> starts = new HashMapOfList<LabelNode, TryCatchBlockNodeInfo>();
        HashMapOfList<LabelNode, TryCatchBlockNodeInfo> ends = new HashMapOfList<LabelNode, TryCatchBlockNodeInfo>();

        mapLabelsToTryCatchBlocks(starts, ends, inlineFunTryBlockInfo);

        MaxCalcNode tempCalcNode = new MaxCalcNode(inlineFun.desc, (inlineFun.access & Opcodes.ACC_STATIC) != 0);
        inlineFun.accept(tempCalcNode);
        int nextTempNonLocalVarIndex = tempCalcNode.getMaxLocal();

        Stack<TryCatchBlockNodeInfo> tryBlockStack = new Stack<TryCatchBlockNodeInfo>();
        InsnList instructions = inlineFun.instructions;
        AbstractInsnNode prev = instructions.getLast();
        while (prev != null) {
            //external block goes first
            if (prev instanceof LabelNode) {
                List<TryCatchBlockNodeInfo> startNodes = starts.getList((LabelNode) prev);
                for (TryCatchBlockNodeInfo startNode : startNodes) {
                    if (!startNode.getOnlyCopyNotProcess()) {
                        TryCatchBlockNodeInfo pop = tryBlockStack.pop();
                        assert startNode == pop : "Wrong try-catch structure " + startNode + " " + pop;
                    }
                }

                for (TryCatchBlockNodeInfo info : Lists.reverse(ends.getList((LabelNode) prev))) {
                    if (!info.getOnlyCopyNotProcess()) {
                        tryBlockStack.add(info);
                    }
                }
            }

            AbstractInsnNode globalFlag = prev.getPrevious();
            if (InlineCodegenUtil.isReturnOpcode(prev.getOpcode()) && InlineCodegenUtil.isMarkedReturn(prev)) {
                //at this point only global return is possible //local one already substituted with goto end

                AbstractInsnNode nextPrev = globalFlag.getPrevious();
                Type nonLocalReturnType = InlineCodegenUtil.getReturnType(prev.getOpcode());

                List<TryBlockCluster> clusters = TryBlockCluster.OBJECT$.doClustering(tryBlockStack);
                ListIterator<TryBlockCluster> tryCatchBlockIterator = clusters.listIterator(clusters.size());
                while (tryCatchBlockIterator.hasPrevious()) {
                    TryBlockCluster originalFinallyCluster = tryCatchBlockIterator.previous();
                    List<TryCatchBlockNodeInfo> clusterBlocks = originalFinallyCluster.getBlocks();
                    TryCatchBlockNodeInfo originalFinallyBlock = clusterBlocks.get(0);

                    instructions.resetLabels();

                    FinallyBlockInfo finallyInfo = findFinallyBlockBody(originalFinallyBlock, inlineFunTryBlockInfo);

                    if (finallyInfo != null) {
                        MethodNode finallyBlockCopy = createEmptyMethodNode();
                        Label newFinallyStart = new Label();
                        Label newFinallyEnd = new Label();


                        if (nonLocalReturnType != Type.VOID_TYPE) {
                            finallyBlockCopy.visitVarInsn(nonLocalReturnType.getOpcode(Opcodes.ISTORE), nextTempNonLocalVarIndex);
                        }
                        finallyBlockCopy.visitLabel(newFinallyStart);

                        List<TryCatchBlockNodePosition> tryCatchBlockInlinedInFinally = findTryCatchBlocksInlinedInFinally(finallyInfo,
                                                                                                                           starts, ends);
                        AbstractInsnNode currentIns = finallyInfo.startIns;
                        Set<LabelNode> labelNodes = new HashSet<LabelNode>();
                        while (currentIns != finallyInfo.endInsExclude) {
                            if (currentIns instanceof LabelNode) {
                                labelNodes.add((LabelNode) currentIns);
                            }
                            currentIns = currentIns.getNext();
                        }

                        currentIns = finallyInfo.startIns;
                        while (currentIns != finallyInfo.endInsExclude) {
                            //This condition allows another model for non-local returns processing
                            if (false && InlineCodegenUtil.isReturnOpcode(currentIns.getOpcode()) && !InlineCodegenUtil.isMarkedReturn(currentIns)) {
                                //subustitute all local returns in finally finallyInfo with non-local one lambdaFinallyBlocks try finallyInfo
                                //TODO same for jumps
                                Type localReturnType = InlineCodegenUtil.getReturnType(currentIns.getOpcode());
                                substituteReturnValueInFinally(nextTempNonLocalVarIndex, nonLocalReturnType, finallyBlockCopy,
                                                               localReturnType, true);

                                globalFlag.accept(finallyBlockCopy);
                                prev.accept(finallyBlockCopy);
                            }
                            else {
                                boolean isMyInsOrLabelJump = true;
                                if (currentIns instanceof JumpInsnNode) {
                                    isMyInsOrLabelJump = labelNodes.contains(((JumpInsnNode) currentIns).label);
                                }
                                if (isMyInsOrLabelJump) {
                                    currentIns.accept(finallyBlockCopy);
                                } else {
                                    finallyBlockCopy.instructions.add(new JumpInsnNode(currentIns.getOpcode(), ((JumpInsnNode) currentIns).label));
                                }
                            }

                            currentIns = currentIns.getNext();
                        }

                        finallyBlockCopy.visitLabel(newFinallyEnd);
                        if (nonLocalReturnType != Type.VOID_TYPE) {
                            finallyBlockCopy.visitVarInsn(nonLocalReturnType.getOpcode(Opcodes.ILOAD), nextTempNonLocalVarIndex);
                            nextTempNonLocalVarIndex += nonLocalReturnType.getSize(); //TODO: do more wise indexing
                        }

                        InlineCodegenUtil.insertNodeBefore(finallyBlockCopy, inlineFun, globalFlag);

                        for (TryCatchBlockNodePosition position : tryCatchBlockInlinedInFinally) {

                            TryCatchBlockNode additionalTryCatchBlock =
                            new TryCatchBlockNode((LabelNode) position.getNodeInfo().getNode().start.getLabel().info,
                                                  (LabelNode) position.getNodeInfo().getNode().end.getLabel().info,
                                                  (LabelNode) position.getNodeInfo().getNode().handler.getLabel().info,
                                                  position.getNodeInfo().getNode().type /*+ "_added " + block.hashCode()*/);

                            TryCatchBlockNodeInfo newInfo =
                                    new TryCatchBlockNodeInfo(additionalTryCatchBlock, additionalTryCatchBlock.start,
                                                              additionalTryCatchBlock.end, true);
                            starts.putInList(additionalTryCatchBlock.start, newInfo);
                            ends.putInList(additionalTryCatchBlock.end, newInfo);
                            inlineFunTryBlockInfo.add(newInfo);
                        }

                        for (TryCatchBlockNodeInfo block : clusterBlocks) {
                            //update exception mapping
                            LabelNode oldStartNode = block.getNode().start;
                            starts.removeFromList(oldStartNode, block);
                            block.getNode().start = (LabelNode) newFinallyEnd.info;

                            TryCatchBlockNode additionalTryCatchBlock =
                                    new TryCatchBlockNode(oldStartNode, (LabelNode) newFinallyStart.info, block.getNode().handler, block.getNode().type);

                            TryCatchBlockNodeInfo newInfo =
                                    new TryCatchBlockNodeInfo(additionalTryCatchBlock, additionalTryCatchBlock.start,
                                                              additionalTryCatchBlock.end, false);
                            starts.putInList(additionalTryCatchBlock.start, newInfo);
                            ends.putInList(additionalTryCatchBlock.end, newInfo);

                            inlineFunTryBlockInfo.add(newInfo);

                            //TODO add assert
                            nextPrev = additionalTryCatchBlock.end;
                            tryBlockStack.pop();
                        }
                        tryCatchBlockIterator.remove();
                        sortTryCatchBlock(inlineFun, inlineFunTryBlockInfo);
                    }

                }
                prev = nextPrev;
                continue;
            }
            prev = prev.getPrevious();
        }

        inlineFun.tryCatchBlocks.clear();
        for (TryCatchBlockNodeInfo info : inlineFunTryBlockInfo) {
            inlineFun.tryCatchBlocks.add(info.getNode());
        }
    }

    private boolean hasFinallyBlocks() {
        for (TryCatchBlockNodeInfo block : inlineFunTryBlockInfo) {
            if (!block.getOnlyCopyNotProcess() && block.getNode().type == null) {
                return true;
            }
        }
        return false;
    }

    private static void mapLabelsToTryCatchBlocks(
            @NotNull HashMapOfList<LabelNode, TryCatchBlockNodeInfo> starts,
            @NotNull HashMapOfList<LabelNode, TryCatchBlockNodeInfo> ends,
            @NotNull List<TryCatchBlockNodeInfo> inlineFunTryBlockInfo
    ) {
        for (TryCatchBlockNodeInfo block : inlineFunTryBlockInfo) {
            starts.putInList(block.getNode().start, block);
            ends.putInList(block.getNode().end, block);
        }
    }

    @Nullable
    private FinallyBlockInfo findFinallyBlockBody(
            @NotNull TryCatchBlockNodeInfo startTryBlock,
            @NotNull List<TryCatchBlockNodeInfo> tryBlocks
    ) {
        List<TryCatchBlockNodeInfo> sameDefaultHandler = new ArrayList<TryCatchBlockNodeInfo>();
        LabelNode defaultHandler = null;
        boolean afterStartBlock = false;
        for (TryCatchBlockNodeInfo block : tryBlocks) {
            if (startTryBlock == block) {
                afterStartBlock = true;
            }

            if (afterStartBlock) {
                if (block.getNode().type == null && (firstLabelInChain(startTryBlock.getNode().start) == firstLabelInChain(block.getNode().start) &&
                                                     firstLabelInChain(startTryBlock.getNode().end) == firstLabelInChain(block.getNode().end)
                                                     || defaultHandler == firstLabelInChain(block.getNode().handler))) {
                    sameDefaultHandler.add(block); //first is startTryBlock if no catch clauses
                    if (defaultHandler == null) {
                        defaultHandler = firstLabelInChain(block.getNode().handler);
                    }
                }
            }
        }

        if (sameDefaultHandler.isEmpty()) {
            //there is no finally block
            //it always should be present in default handler
            return null;
        }

        AbstractInsnNode startFinallyChain;
        AbstractInsnNode endFinallyChainExclusive;

        startFinallyChain = startTryBlock.getNode().end;
        endFinallyChainExclusive = sameDefaultHandler.get(1).getNode().start;

        AbstractInsnNode prev = getPrevNoLineNumberOrLabel(endFinallyChainExclusive, true);
        assert prev != null : "Empty finally block!";

        if (prev.getOpcode() == Opcodes.GOTO) {
            //there we should understand is goto is jump over catches or last break/continue command inside finally
            LabelNode handler = sameDefaultHandler.get(1).getNode().handler;
            LabelNode targetJump = ((JumpInsnNode) prev).label;

            if (inlineFun.instructions.indexOf(handler) < inlineFun.instructions.indexOf(targetJump)) {
                AbstractInsnNode cur = handler;
                boolean keepJump = false;
                while (cur != targetJump) {
                    if (cur.getOpcode() == Opcodes.GOTO) {
                        if (((JumpInsnNode) cur).label == targetJump) {
                            keepJump = true;
                            break;
                        }
                    }
                    cur = cur.getNext();
                }
                if (!keepJump) {
                    endFinallyChainExclusive = prev;
                }
            }
            else {
                //otherwise continue
            }
        }

        return new FinallyBlockInfo(startFinallyChain.getNext(), endFinallyChainExclusive);
    }

    @NotNull
    private static List<TryCatchBlockNodePosition> findTryCatchBlocksInlinedInFinally(
            @NotNull FinallyBlockInfo finallyInfo,
            @NotNull Map<LabelNode, List<TryCatchBlockNodeInfo>> starts,
            @NotNull Map<LabelNode, List<TryCatchBlockNodeInfo>> ends
    ) {
        AbstractInsnNode curNodes = finallyInfo.startIns;
        List<TryCatchBlockNodePosition> result = new ArrayList<TryCatchBlockNodePosition>();
        Map<TryCatchBlockNodeInfo, TryCatchBlockNodePosition> processedBlocks = new HashMap<TryCatchBlockNodeInfo, TryCatchBlockNodePosition>();
        while (curNodes != finallyInfo.endInsExclude) {
            List<TryCatchBlockNodeInfo> startedTryBlocks = starts.get(curNodes);
            if (startedTryBlocks != null) {
                for (TryCatchBlockNodeInfo block : startedTryBlocks) {
                    if (block.getOnlyCopyNotProcess()) {
                        assert !processedBlocks.containsKey(block) : "Try catch block already processed before start label!!! " + block;
                        TryCatchBlockNodePosition info = new TryCatchBlockNodePosition(block, TryCatchPosition.START);
                        processedBlocks.put(block, info);
                        result.add(info);
                    }
                }
            }

            List<TryCatchBlockNodeInfo> endedTryBlocks = ends.get(curNodes);
            if (endedTryBlocks != null) {
                for (TryCatchBlockNodeInfo block : endedTryBlocks) {
                    if (block.getOnlyCopyNotProcess()) {
                        TryCatchBlockNodePosition info = processedBlocks.get(block);
                        if (info != null) {
                            assert info.getPosition() == TryCatchPosition.START;
                            info.setPosition(TryCatchPosition.INNER);
                        }
                        else {
                            info = new TryCatchBlockNodePosition(block, TryCatchPosition.END);
                            processedBlocks.put(block, info);
                            result.add(info);
                        }
                    }
                }
            }

            curNodes = curNodes.getNext();
        }
        return result;
    }

    private static void substituteReturnValueInFinally(
            int nonLocalVarIndex,
            @NotNull Type nonLocalReturnType,
            @NotNull MethodNode finallyBlockCopy,
            @NotNull Type localReturnType,
            boolean doPop
    ) {
        if (doPop && localReturnType != Type.VOID_TYPE) {
            AsmUtil.pop(finallyBlockCopy, localReturnType);
        }
        if (nonLocalReturnType != Type.VOID_TYPE) {
            finallyBlockCopy.visitVarInsn(nonLocalReturnType.getOpcode(Opcodes.ILOAD), nonLocalVarIndex);
        }
    }


    @Nullable
    private static AbstractInsnNode getPrevNoLineNumberOrLabel(@NotNull AbstractInsnNode node, boolean strict) {
        AbstractInsnNode result = strict ? node.getPrevious() : node;
        while (isLineNumberOrLabel(result)) {
            result = result.getPrevious();
        }
        return result;
    }

    static class HashMapOfList<T, V> extends HashMap<T, List<V>> {

        public void putInList(@NotNull T key, @NotNull V value) {
            List<V> values = get(key);
            if (values == null) {
                values = new LinkedList<V>();
                put(key, values);
            }
            values.add(value);
        }

        public void removeFromList(@NotNull T key, @NotNull V value) {
            List<V> values = getList(key);
            values.remove(value);
        }

        @NotNull
        public List<V> getList(T key) {
            List<V> values = get(key);
            return values != null ? values : Collections.<V>emptyList();
        }

    }

    public static void sortTryCatchBlock(final MethodNode node, List<TryCatchBlockNodeInfo> tryToSort) {
        Comparator<TryCatchBlockNodeInfo> comp = new Comparator<TryCatchBlockNodeInfo>() {
            @Override
            public int compare(@NotNull TryCatchBlockNodeInfo t1, @NotNull TryCatchBlockNodeInfo t2) {
                int result = node.instructions.indexOf(t1.getNode().handler) - node.instructions.indexOf(t2.getNode().handler);
                if (result == 0) {
                    result = node.instructions.indexOf(t1.getNode().start) - node.instructions.indexOf(t2.getNode().start);
                    if (result == 0) {
                        assert false : "Error";
                        result = node.instructions.indexOf(t1.getNode().end) - node.instructions.indexOf(t2.getNode().end);
                    }
                }
                return result;
            }
        };
        Collections.sort(tryToSort, comp);
    }
}
