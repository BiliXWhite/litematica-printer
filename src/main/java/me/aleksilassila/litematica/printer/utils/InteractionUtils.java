package me.aleksilassila.litematica.printer.utils;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.ExcavateListMode;
import me.aleksilassila.litematica.printer.mixin_extension.BlockBreakResult;
import me.aleksilassila.litematica.printer.mixin_extension.MultiPlayerGameModeExtension;
import me.aleksilassila.litematica.printer.printer.SchematicBlockContext;
import me.aleksilassila.litematica.printer.utils.minecraft.PlayerUtils;
import me.aleksilassila.litematica.printer.utils.mods.ModLoadUtils;
import me.aleksilassila.litematica.printer.utils.mods.TweakerooUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST;
import static fi.dy.masa.tweakeroo.config.Configs.Lists.BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST;
import static fi.dy.masa.tweakeroo.tweaks.PlacementTweaks.BLOCK_TYPE_BREAK_RESTRICTION;

@SuppressWarnings({"DataFlowIssue", "BooleanMethodIsAlwaysInverted"})
@Environment(EnvType.CLIENT)
public class InteractionUtils {
    public static final Minecraft client = Minecraft.getInstance();
    public static final InteractionUtils INSTANCE = new InteractionUtils();

    private final Deque<BlockPos> breakQueue = new ArrayDeque<>();
    private final Set<BlockPos> queuedBreakPositions = new HashSet<>();
    private BlockPos breakPos;

    private InteractionUtils() {
    }

    public static boolean canBreakBlock(BlockPos pos) {
        ClientLevel world = client.level;
        LocalPlayer player = client.player;
        if (world == null || player == null) return false;
        BlockState currentState = world.getBlockState(pos);
        if (Configs.Break.BREAK_CHECK_HARDNESS.getBooleanValue() && currentState.getBlock().defaultDestroyTime() < 0) {
            return false;
        }
        return !currentState.isAir() &&
                !currentState.is(Blocks.AIR) &&
                !currentState.is(Blocks.CAVE_AIR) &&
                !currentState.is(Blocks.VOID_AIR) &&
                !(currentState.getBlock() instanceof LiquidBlock) &&
                !player.blockActionRestricted(client.level, pos, client.gameMode.getPlayerMode());
    }

    public static boolean breakRestriction(BlockState blockState) {
        if (Configs.Print.BREAK_LIMITER.getOptionListValue().equals(ExcavateListMode.TWEAKEROO)) {
            if (!ModLoadUtils.isTweakerooLoaded()) return true;
            UsageRestriction.ListType listType = BLOCK_TYPE_BREAK_RESTRICTION.getListType();
            if (listType == UsageRestriction.ListType.BLACKLIST) {
                return BLOCK_TYPE_BREAK_RESTRICTION_BLACKLIST.getStrings().stream()
                        .noneMatch(string -> FilterUtils.matchBlockName(string, blockState));
            } else if (listType == UsageRestriction.ListType.WHITELIST) {
                return BLOCK_TYPE_BREAK_RESTRICTION_WHITELIST.getStrings().stream()
                        .anyMatch(string -> FilterUtils.matchBlockName(string, blockState));
            } else {
                return true;
            }
        } else {
            IConfigOptionListEntry optionListValue = Configs.Print.BREAK_LIMIT.getOptionListValue();
            if (optionListValue == UsageRestriction.ListType.BLACKLIST) {
                return Configs.Print.BREAK_BLACKLIST.getStrings().stream()
                        .noneMatch(string -> FilterUtils.matchBlockName(string, blockState));
            } else if (optionListValue == UsageRestriction.ListType.WHITELIST) {
                return Configs.Print.BREAK_WHITELIST.getStrings().stream()
                        .anyMatch(string -> FilterUtils.matchBlockName(string, blockState));
            } else {
                return true;
            }
        }
    }

    public void add(BlockPos pos) {
        if (pos == null) return;
        if (pos.equals(breakPos)) {
            return;
        }
        if (queuedBreakPositions.add(pos)) {
            breakQueue.addLast(pos);
        }
    }

    public void add(SchematicBlockContext ctx) {
        if (ctx == null) return;
        this.add(ctx.blockPos);
    }

    public void preprocess() {
        if (!ConfigUtils.isEnable()) {
            if (!breakQueue.isEmpty()) {
                breakQueue.clear();
            }
            if (!queuedBreakPositions.isEmpty()) {
                queuedBreakPositions.clear();
            }
            if (breakPos != null) {
                breakPos = null;
            }
        }
    }

    public boolean isNeedHandle() {
        return !breakQueue.isEmpty() || breakPos != null;
    }

    public void onTick() {
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null) {
            return;
        }
        if (breakPos == null && breakQueue.isEmpty()) {
            return;
        }
        while (true) {
            if (breakPos != null) {
                if (continueDestroyBlock(breakPos, Direction.DOWN) == BlockBreakResult.IN_PROGRESS) {
                    return;
                }
                breakPos = null;
            }

            BlockPos pos = pollNextBreakPos();
            if (pos == null) {
                return;
            }
            if (!ConfigUtils.canInteracted(pos) || !canBreakBlock(pos) || !breakRestriction(level.getBlockState(pos))) {
                continue;
            }
            if (ModLoadUtils.isTweakerooLoaded() && TweakerooUtils.isToolSwitchEnabled()) {
                switchToBestTool(player, level.getBlockState(pos));
            }
            if (continueDestroyBlock(pos, Direction.DOWN) == BlockBreakResult.IN_PROGRESS) {
                breakPos = pos;
                return;
            }
        }
    }

    @Nullable
    private BlockPos pollNextBreakPos() {
        BlockPos pos = breakQueue.pollFirst();
        if (pos != null) {
            queuedBreakPositions.remove(pos);
        }
        return pos;
    }

    private void switchToBestTool(LocalPlayer player, BlockState blockState) {
        Inventory inventory = player.getInventory();
        int bestSlot = InventoryUtils.getSelectedSlot(inventory);
        float bestSpeed = PlayerUtils.getBlockBreakingSpeed(player, blockState, player.getMainHandItem());

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (candidate.isEmpty()) {
                continue;
            }
            float speed = PlayerUtils.getBlockBreakingSpeed(player, blockState, candidate);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = slot;
            }
        }

        if (bestSlot != InventoryUtils.getSelectedSlot(inventory)) {
            InventoryUtils.setPickedItemToHand(bestSlot, inventory.getItem(bestSlot), client);
        }
    }

    public BlockBreakResult continueDestroyBlock(final BlockPos blockPos, Direction direction, boolean localPrediction) {
        MultiPlayerGameModeExtension gameMode = (@Nullable MultiPlayerGameModeExtension) client.gameMode;
        BlockBreakResult result = gameMode.litematica_printer$continueDestroyBlock(localPrediction, blockPos, direction);
        if (result == BlockBreakResult.IN_PROGRESS) {
            breakPos = blockPos;
        }
        return result;
    }

    public BlockBreakResult continueDestroyBlock(BlockPos blockPos, Direction direction) {
        return this.continueDestroyBlock(blockPos, direction, !Configs.Break.BREAK_USE_PACKET.getBooleanValue());
    }

    public BlockBreakResult continueDestroyBlock(BlockPos blockPos) {
        return this.continueDestroyBlock(blockPos, Direction.DOWN);
    }

    public InteractionResult useItemOn(boolean localPrediction, InteractionHand hand, BlockHitResult blockHit) {
        MultiPlayerGameModeExtension gameMode = (@Nullable MultiPlayerGameModeExtension) client.gameMode;
        return gameMode.litematica_printer$useItemOn(localPrediction, hand, blockHit);
    }

    public InteractionResult useItemOn(InteractionHand hand, BlockHitResult blockHit) {
        return this.useItemOn(!Configs.Placement.PRINT_USE_PACKET.getBooleanValue(), hand, blockHit);
    }
}
