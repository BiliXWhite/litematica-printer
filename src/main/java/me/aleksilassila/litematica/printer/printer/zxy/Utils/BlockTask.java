package me.aleksilassila.litematica.printer.printer.zxy.Utils;

import me.aleksilassila.litematica.printer.printer.Printer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Predicate;

import static me.aleksilassila.litematica.printer.interfaces.Implementation.sendLookPacket;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.BlockTask.BlockTaskManager.looking;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.PlayerAction.excavateBlock;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.*;

public class BlockTask {
    public BlockPos pos;
    public Block block;
    public BlockState state;
    public ItemStack itemStack;
    public Direction direction1;
    public Direction direction2;
    public Direction side;
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

    public BlockTask setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
        return this;
    }

    public BlockTask setSide(Direction side) {
        this.side = side;
        return this;
    }

    public BlockTask setPos(BlockPos pos) {
        this.pos = pos;
        return this;
    }

    public BlockTask setDirection1(Direction direction1) {
        this.direction1 = direction1;
        return this;
    }

    public BlockTask setDirection2(Direction direction2) {
        this.direction2 = direction2;
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
        return taskState.equals(BlockTaskState.DONE_TASK);
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
        NEED_RUN_PREDICATE,
        DONE_RUN_PREDICATE,
        WAIT,
        DONE_TASK
    }

    public static class BreakBlock extends BlockTask {
        public boolean switchTool = true;
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
            taskState = BlockTaskState.DONE_TASK;
            return true;
        }
    }

    public static class PlaceBlock extends BlockTask {
        Vec3d vec3d = Vec3d.ZERO;
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

        public PlaceBlock setVec3d(Vec3d vec3d) {
            this.vec3d = vec3d;
            return this;
        }

        @Override
        public boolean runTask() {
            if (done()) return false;
            Vec3d vec3d = Printer.getPrinter().usePrecisionPlacement(pos, state);
            if (!looking && vec3d == null && direction1 != null && taskState == BlockTaskState.INITIAL) {
                looking = true; // TODO 应该有个静态变量记录direction1 和 direction2 改变朝向包时使其达到预想效果
                sendLookPacket(ZxyUtils.client.player, direction1, direction2);
                taskState = BlockTaskState.WAIT;
                return false;
            }
            vec3d = vec3d != null ? vec3d : this.vec3d;
            PlayerAction.interactBlock(Hand.MAIN_HAND, vec3d, side, pos, false, useShift);
            taskState = BlockTaskState.DONE_TASK;
            return true;
        }
    }

    public static class BlockTaskManager {
        public static LinkedList<BlockTask> blockTaskList = new LinkedList<>();
        public static boolean looking = false;
        public static void tick() {
            for (BlockTask blockTask : blockTaskList) {
                if (blockTask.done()) continue;
                blockTask.tick();
            }
            looking = false;
            blockTaskList.removeIf(task -> task.done() || !task.pos.isWithinDistance(ZxyUtils.client.player.getPos(), getRage()));
        }

        public static boolean addTask(BlockTask task) {
            if (blockTaskList.size() >= 256) return false;
            return blockTaskList.add(task);
        }

    }
}
