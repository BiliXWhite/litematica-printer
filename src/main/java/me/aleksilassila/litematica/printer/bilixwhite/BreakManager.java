package me.aleksilassila.litematica.printer.bilixwhite;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import me.aleksilassila.litematica.printer.bilixwhite.utils.TweakerooUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.config.enums.ExcavateListMode;
import me.aleksilassila.litematica.printer.printer.BlockContext;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.PrinterUtils;
import me.aleksilassila.litematica.printer.utils.FilterUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST;
import static fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST;
import static fi.dy.masa.tweakeroo.tweaks.PlacementTweaks.BLOCK_TYPE_BREAK_RESTRICTION;

public class BreakManager {
    private static final HashMap<Identifier, BlockPos> breakTargets = new HashMap<>();
    private static final Minecraft client = Minecraft.getInstance();
    private static BreakManager INSTANCE = null;
    private BlockPos breakPos;
    private BlockState state;

    private BreakManager() {
        INSTANCE = this;
    }

    public static @NotNull BreakManager instance() {
        if (INSTANCE == null) {
            INSTANCE = new BreakManager();
        }
        return INSTANCE;
    }

    // 添加需要挖掘的方块
    public static void addBlockToBreak(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        breakTargets.put(level.dimension().identifier(), pos);
    }

    public static void addBlockToBreak(BlockContext context) {
        if (context == null) return;
        addBlockToBreak(context.level, context.blockPos);
    }

    public static boolean hasTargets() {
        return !breakTargets.isEmpty();
    }

    public static boolean canBreakBlock(BlockPos pos) {
        if (client == null || client.level == null || client.player == null || client.gameMode == null || pos == null) {
            return false;
        }
        ClientLevel world = client.level;
        BlockState currentState = world.getBlockState(pos);
        return !currentState.isAir() &&
                !(currentState.getBlock() instanceof LiquidBlock) &&
                !currentState.is(Blocks.AIR) &&
                !currentState.is(Blocks.CAVE_AIR) &&
                !currentState.is(Blocks.VOID_AIR) &&
                !(currentState.getBlock().defaultDestroyTime() == -1) &&
                !client.player.blockActionRestricted(client.level, pos, client.gameMode.getPlayerMode());
    }




}
