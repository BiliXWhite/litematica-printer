package me.aleksilassila.litematica.printer.bilixwhite.utils;


import fi.dy.masa.malilib.config.options.ConfigStringList;
import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

//#if MC > 11900
import com.github.bunnyi116.bedrockminer.config.Config;
import com.github.bunnyi116.bedrockminer.task.TaskManager;
//#endif

public class BedrockUtils {

    public static void init() {
        //#if MC > 11900
        Config.getInstance().pistonDirections = getDirection(InitHandler.BEDROCK_MINER_PISTON_DIRECTIONS);
        Config.getInstance().pistonFacings = getDirection(InitHandler.BEDROCK_MINER_PISTON_FACINGS);
        Config.getInstance().redstoneTorchDirections = getDirection(InitHandler.BEDROCK_MINER_REDSTONE_TORCH_DIRECTIONS);
        Config.getInstance().redstoneTorchFacings = getDirection(InitHandler.BEDROCK_MINER_REDSTONE_TORCH_FACINGS);
        Config.getInstance().limitMax = InitHandler.BEDROCK_MINER_LIMIT_MAX.getIntegerValue();
        Config.getInstance().shortTsk = InitHandler.BEDROCK_MINER_SHORT_TASK.getBooleanValue();
        Config.getInstance().save();

        InitHandler.BEDROCK_MINER_PISTON_DIRECTIONS.setValueChangeCallback(b -> {
            Config.getInstance().pistonDirections = getDirection(InitHandler.BEDROCK_MINER_PISTON_DIRECTIONS);
        });
        InitHandler.BEDROCK_MINER_PISTON_FACINGS.setValueChangeCallback(b -> {
            Config.getInstance().pistonFacings = getDirection(InitHandler.BEDROCK_MINER_PISTON_FACINGS);
        });
        InitHandler.BEDROCK_MINER_REDSTONE_TORCH_DIRECTIONS.setValueChangeCallback(b -> {
            Config.getInstance().redstoneTorchDirections = getDirection(InitHandler.BEDROCK_MINER_REDSTONE_TORCH_DIRECTIONS);
        });
        InitHandler.BEDROCK_MINER_REDSTONE_TORCH_FACINGS.setValueChangeCallback(b -> {
            Config.getInstance().redstoneTorchFacings = getDirection(InitHandler.BEDROCK_MINER_REDSTONE_TORCH_FACINGS);
        });
        InitHandler.BEDROCK_MINER_LIMIT_MAX.setValueChangeCallback(config -> {
            Config.getInstance().limitMax = config.getIntegerValue();
            Config.getInstance().save();
        });
        InitHandler.BEDROCK_MINER_SHORT_TASK.setValueChangeCallback(config -> {
            Config.getInstance().shortTsk = config.getBooleanValue();
            Config.getInstance().save();
        });
        //#endif
    }

    public static Direction[] getDirection(ConfigStringList configStringList) {
        List<Direction> directions = new ArrayList<>();
        for (String direction : configStringList.getStrings()) {
            switch (direction) {
                case "up", "上" -> directions.add(Direction.UP);
                case "down", "下" -> directions.add(Direction.DOWN);
                case "north", "北" -> directions.add(Direction.NORTH);
                case "south", "南" -> directions.add(Direction.SOUTH);
                case "west", "西" -> directions.add(Direction.WEST);
                case "east", "东" -> directions.add(Direction.EAST);
            }
        }
        return directions.toArray(new Direction[0]);
    }

    public static void addToBreakList(BlockPos pos, ClientLevel world) {
        //#if MC > 11900
        Block block = world.getBlockState(pos).getBlock();
        TaskManager.getInstance().addBlockTask(world, pos, block);
        //#endif
    }

    public static void clearTask() {
        //#if MC > 11900
        TaskManager.getInstance().removeAll(false);
        //#endif
    }

    public static boolean isWorking() {
        //#if MC > 11900
        return TaskManager.getInstance().isRunning();
        //#else
        //$$ return false;
        //#endif
    }

    public static void setWorking(boolean running) {
        //#if MC > 11900
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.isCreative() && running) {
            ZxyUtils.actionBar("创造模式下不支持破基岩！");
            return;
        }
        TaskManager.getInstance().setRunning(running, false);
        if (!running) clearTask();
        //#endif
    }

    public static boolean isBedrockMinerFeatureEnable() {
        //#if MC > 11900
        return TaskManager.getInstance().isBedrockMinerFeatureEnable();
        //#else
        //$$ return false;
        //#endif
    }

    public static void setBedrockMinerFeatureEnable(boolean bedrockMinerFeatureEnable) {
        //#if MC > 11900
        TaskManager.getInstance().setBedrockMinerFeatureEnable(bedrockMinerFeatureEnable);
        //#endif
    }
}