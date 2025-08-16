package me.aleksilassila.litematica.printer.printer.zxy.Utils;

import me.aleksilassila.litematica.printer.printer.Printer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static me.aleksilassila.litematica.printer.interfaces.Implementation.sendLookPacket;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.canInteracted;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.tick;

public class BlockTask {
    public BlockPos pos;
    public Block block;
    public BlockState state;
    public Direction direction;
    public Map<String, Predicate<BlockTask>> predicateMap = new HashMap<>();
    public String taskName;
    public BlockTaskState taskState = BlockTaskState.INITIAL;
    public boolean useShift = false;

    public BlockTask(BlockPos pos, Block block) {
        this.pos = pos;
        this.block = block;
    }

    public BlockTask(BlockPos pos, BlockState state) {
        this.pos = pos;
        this.state = state;
        this.block = state.getBlock();
    }

    public BlockTask(BlockPos pos) {
        this.pos = pos;
    }

    public BlockTask(BlockPos pos, BlockState state, String taskName) {
        this.pos = pos;
        this.block = state.getBlock();
        this.state = state;
        this.taskName = taskName;
    }

    public BlockTask setBlock(Block block) {
        this.block = block;
        return this;
    }

    public BlockTask setState(BlockState state) {
        this.state = state;
        this.block = state.getBlock();
        return this;
    }

    public BlockTask setPos(BlockPos pos) {
        this.pos = pos;
        return this;
    }

    public BlockTask setDirection(Direction direction) {
        this.direction = direction;
        return this;
    }

    public BlockTask setPredicate(String predicateName, Predicate<BlockTask> predicate) {
        this.predicateMap.put(predicateName, predicate);
        return this;
    }

    public BlockTask setUseShift(boolean useShift) {
        this.useShift = useShift;
        return this;
    }

    public boolean done() {
        return taskState.equals(BlockTaskState.TASK_DONE);
    }

    public boolean runPredicate(String predicateName) {
        return predicateMap.get(predicateName).test(this);
    }

    public boolean tick() {
        if (runTask()) return true;
        return done();
    }

    public boolean runTask() {
        return done();
    }

    public boolean equalsBlockName(String blockName) {
        return block != null && Filters.equalsBlockName(blockName, block);
    }

    public enum BlockTaskState {
        INITIAL,
        NEED_RUN_TASK,
        WAIT,
        TASK_DONE
    }

    public static class BreakBlock extends BlockTask {
        public BreakBlock(BlockPos pos, Block block) {
            super(pos, block);
        }

        public BreakBlock(BlockPos pos, BlockState state) {
            super(pos, state);
        }

        public BreakBlock(BlockPos pos) {
            super(pos);
        }

        public BreakBlock(BlockPos pos, BlockState state, String taskName) {
            super(pos, state, taskName);
        }

        @Override
        public boolean runTask() {
            if (excavateBlock(pos) == null) {
                return false;
            }
            taskState = BlockTaskState.TASK_DONE;
            return true;
        }

        static BlockPos breakTargetBlock = null;
        static int startTick = -1;
        //如果返回了null则表示正在挖掘该方块
        public static BlockPos excavateBlock(BlockPos pos){
            if (!canInteracted(pos)) {
                breakTargetBlock = null;
                return null;
            }
            //一个游戏刻挖一次就好
            if (startTick == tick) {
                return null;
            }
            breakTargetBlock = breakTargetBlock != null ? breakTargetBlock : pos;
            if (!Printer.waJue(breakTargetBlock)) {
                BlockPos breakTargetBlock1 = breakTargetBlock;
                breakTargetBlock = null;
                return breakTargetBlock1;
            }
            startTick = tick;
            return null;
        }
    }

    public class PlaceBlock extends BlockTask {

        public PlaceBlock(BlockPos pos, Block block) {
            super(pos, block);
        }

        public PlaceBlock(BlockPos pos, BlockState state) {
            super(pos, state);
        }

        public PlaceBlock(BlockPos pos) {
            super(pos);
        }

        public PlaceBlock(BlockPos pos, BlockState state, String taskName) {
            super(pos, state, taskName);
        }

        @Override
        public boolean runTask() {
            Vec3d vec3d = Printer.getPrinter().usePrecisionPlacement(pos, state);
            if (vec3d == null && direction != null && taskState == BlockTaskState.INITIAL) {
                sendLookPacket(ZxyUtils.client.player, direction);
                taskState = BlockTaskState.WAIT;
                return false;
            }
            if (vec3d == null) {
                ZxyUtils.interactBlock1(Hand.MAIN_HAND, new Vec3d(0, 0, 0), direction, pos, false, useShift);
            } else {
                ZxyUtils.interactBlock1(Hand.MAIN_HAND, vec3d, direction, pos, false, useShift);
            }
            taskState = BlockTaskState.TASK_DONE;
            return true;
        }
    }
}
