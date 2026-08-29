package me.aleksilassila.litematica.printer.handler.handlers;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import lombok.Getter;
import lombok.Setter;
import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.BlockMatchingType;
import me.aleksilassila.litematica.printer.enums.HighlightType;
import me.aleksilassila.litematica.printer.handler.Module;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.printer.*;
import me.aleksilassila.litematica.printer.printer.action.Action;
import me.aleksilassila.litematica.printer.printer.ActionManager;
import me.aleksilassila.litematica.printer.printer.action.ClickAction;
import me.aleksilassila.litematica.printer.printer.MissingMaterialTracker;
import me.aleksilassila.litematica.printer.utils.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Print extends Module {
    public final static String NAME = "print";

    private final PlacementGuide guide;

    @Getter
    @Setter
    private boolean pistonNeedFix;

    @Getter
    @Setter
    private boolean printerMemorySync;

    private Action action;

    private SchematicBlockContext ctx;

    // canProcessPos 缓存
    private List<String> lastSkipConfig = Collections.emptyList();
    private Set<String> skipSet = Collections.emptySet();
    private Block lastSkipBlock = null;
    private boolean lastSkipResult = false;

    // 等待水产生队列
    @Getter @Setter
    private BlockPos watingForWaterPos;

    // 等待水生成的最大tick数：超过仍未出水则警告并关闭打印机
    private static final int MAX_WAIT_WATER_TICKS = 60;
    // 等待水ack/冰块放置ack的宽限tick数：期间即使无冰无水也不清除标记，避免重放冰破坏刚生成的水源
    private static final int WAIT_ACK_GRACE_TICKS = 10;
    private int watingForWaterTicks;
    private boolean placingIceForWater;

    public Print() {
        super(NAME, Configs.Print.ENABLED, Configs.Print.PRINT_SELECTION_TYPE, true);
        this.guide = new PlacementGuide(client);
        this.needSchematic = true;
    }

    @Override
    protected int getTickInterval() {
        return Configs.Placement.PLACE_INTERVAL.getIntegerValue();
    }

    @Override
    protected int getMaxExecutions() {
        return Configs.Placement.PLACE_BLOCKS_PER_TICK.getIntegerValue();
    }

    @Override
    public boolean canProcessPos(BlockPos blockPos) {
        WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
        if (schematic == null) return false;

        BlockState required = schematic.getBlockState(blockPos);
        BlockState current = level.getBlockState(blockPos);

        // 如果原理图为空气且不破坏多余方块，则无法处理
        if (required.isAir()) {
            if (!Configs.Print.BREAK_EXTRA_BLOCK.getBooleanValue()) return false;
        }

        this.ctx = new SchematicBlockContext(client, level, schematic, blockPos, current, required);

        if (Configs.Print.PRINT_SKIP.getBooleanValue()) {
            List<String> currentConfig = Configs.Print.PRINT_SKIP_LIST.getStrings();
            if (!currentConfig.equals(lastSkipConfig)) {
                skipSet = new HashSet<>(currentConfig);
                lastSkipConfig = currentConfig;
                lastSkipBlock = null;
            }

            Block block = ctx.requiredState.getBlock();
            if (block != lastSkipBlock) {
                lastSkipBlock = block;
                lastSkipResult = false;
                for (String s : skipSet) {
                    if (PinYinSearchUtils.matchName(s, ctx.requiredState)) {
                        lastSkipResult = true;
                        break;
                    }
                }
            }
            if (lastSkipResult) return false;
        }

        Action action = guide.getAction(ctx);
        if (action == null) return false;
        this.action = action;
        return true;
    }

    @Override
    public boolean isCorrectBlock(BlockPos pos) {
        BlockState required = LitematicaUtils.getBlockState(pos);
        BlockState current = level.getBlockState(pos);
        return BlockMatchingType.get(required, current) == BlockMatchingType.CORRECT;
    }

    @Override
    @Nullable
    protected Item[] getRequiredItems(BlockPos pos) {
        // canProcessPos 已设置 this.action 和 this.ctx
        if (this.action != null && this.ctx != null) {
            return this.action.getRequiredItems(this.ctx.requiredState.getBlock());
        }
        return null;
    }

    @Override
    protected void executeIteration(BlockPos blockPos, AtomicReference<Boolean> skipIteration) {
        placingIceForWater = false;
        if (Configs.Print.PRINT_ICE_FOR_WATER.getBooleanValue()
                && BlockUtils.needsWater(ctx.requiredState)) {
            boolean isWaitingHere = watingForWaterPos != null && watingForWaterPos.equals(blockPos);
            boolean isIce = ctx.currentState.getBlock() instanceof IceBlock;
            boolean matchesWaterRequest = BlockUtils.isWaterSource(ctx.currentState) || BlockUtils.isWaterlogged(ctx.currentState);
            // 等待标记过期：连续 WAIT_ACK_GRACE_TICKS 个等待tick内无冰无水且无待挖掘任务（如水被玩家/活塞移除）
            // 才清除标记。宽限期覆盖冰块放置/破坏后的 ack 往返，避免重放冰破坏刚生成的水源。
            if (isWaitingHere && !isIce && !matchesWaterRequest && !BreakUtils.INSTANCE.inQueue(blockPos)
                    && watingForWaterTicks >= WAIT_ACK_GRACE_TICKS) {
                watingForWaterPos = null;
                watingForWaterTicks = 0;
                isWaitingHere = false;
            }
            switch (IceForWaterFlow.decide(
                    true, isWaitingHere, isIce, matchesWaterRequest, watingForWaterTicks, MAX_WAIT_WATER_TICKS)) {
                case PLACE_BLOCK -> {
                    if (isWaitingHere) {
                        watingForWaterPos = null;
                        watingForWaterTicks = 0;
                    }
                    // 水已生成：走下方正常放置流程，放置含水方块（buildAction 已返回对应 Action）
                }
                case KEEP_WAITING -> {
                    if (isIce) {
                        ensureIceBreakQueued(blockPos);
                    }
                    watingForWaterTicks++;
                    enterWaiting(blockPos);
                    skipIteration.set(true);
                    return;
                }
                case WAIT_TIMEOUT -> {
                    // 等待 MAX_WAIT_WATER_TICKS tick 仍无水生成：警告并关闭打印机
                    watingForWaterPos = null;
                    watingForWaterTicks = 0;
                    MessageUtils.setOverlayMessage(I18n.ICE_WATER_TIMEOUT.getName());
                    Configs.Core.WORK_SWITCH.setBooleanValue(false);
                    return;
                }
                case BREAK_ICE_AND_WAIT -> {
                    ensureIceBreakQueued(blockPos);
                    watingForWaterPos = blockPos.immutable();
                    watingForWaterTicks = 0;
                    enterWaiting(blockPos);
                    skipIteration.set(true);
                    return;
                }
                case PLACE_ICE -> {
                    placingIceForWater = true; // 走下方正常放置流程放冰
                }
                case SKIP -> {
                }
            }
        }
        // 下落检查
        if (Configs.Placement.FALLING_CHECK.getBooleanValue()
                && ctx.requiredState.getBlock() instanceof FallingBlock) {
            BlockPos downPos = blockPos.below();

            if (FallingBlock.isFree(level.getBlockState(downPos))) {
                MessageUtils.setOverlayMessage(
                        I18n.BLOCK_NO_SUPPORT.getName(ctx.getRequiredBlockName().getString()));
                addHighlight(blockPos, HighlightType.FAILED);
                return;
            } else if (level.getBlockState(downPos) != ctx.schematic.getBlockState(downPos)) {
                MessageUtils.setOverlayMessage(
                        I18n.BLOCK_MISMATCH.getName(ctx.getRequiredBlockName().getString()));
                addHighlight(blockPos, HighlightType.FAILED);
                return;
            }
        }
        Item[] reqItems = action.getRequiredItems(ctx.requiredState.getBlock());
        // 检查是否有待交换的物品
        if (RemoteContainerUtils.hasPendingExchange()) {
            enterWaiting(blockPos);
            skipIteration.set(true);
            return;
        }
        Direction side = action.getValidSide(level, blockPos);
        if (side == null) {
            addHighlight(blockPos, HighlightType.FAILED);
            return;
        }
        if (!InventoryUtils.switchToItems(player, reqItems)) {
            if (QuickShulkerUtils.isOpenHandler()) {
                enterWaiting(blockPos);
                skipIteration.set(true);
                return;
            }
            setCooldown(blockPos, ConfigUtils.getPlaceCooldown());
            recordMissingMaterial(reqItems);
            if (reqItems != null && reqItems.length > 0 && reqItems[0] != null
                    && !QuickShulkerUtils.isOpenHandler()
                    && Configs.Print.USE_REMOTE_CONTAINER.getBooleanValue()) {
                RemoteContainerUtils.tryGetItemFromContainers(reqItems[0]);
            }
            addHighlight(blockPos, HighlightType.FAILED);
            return;
        }
        boolean useShift;
        if (action.getShift() == null) {
            useShift =
                    (Implementation.isInteractive(
                                            level.getBlockState(blockPos.relative(side)).getBlock())
                                    && !(action instanceof ClickAction))
                            || Configs.Print.PRINT_FORCED_SNEAK.getBooleanValue();
        } else {
            useShift = action.getShift();
        }
        action.queueAction(blockPos, side, useShift, player);
        // 放冰完成：入队挖掘并进入等待水生成
        if (placingIceForWater) {
            placingIceForWater = false;
            ActionManager.INSTANCE.setLook(action.getPlayerLook());
            ActionManager.INSTANCE.setNeedWaitModifyLookFromAction(action.getNeedWaitModifyLook());
            ActionManager.INSTANCE.sendQueue(player);
            ensureIceBreakQueued(blockPos);
            watingForWaterPos = blockPos.immutable();
            watingForWaterTicks = 0;
            enterWaiting(blockPos);
            skipIteration.set(true);
            return;
        }
        Vec3 hitModifier = LitematicaUtils.usePrecisionPlacement(blockPos, ctx.requiredState);
        if (hitModifier != null) {
            ActionManager.INSTANCE.hitModifier = hitModifier;
            ActionManager.INSTANCE.useProtocol = true;
        }
        ActionManager.INSTANCE.setLook(action.getPlayerLook());
        ActionManager.INSTANCE.setNeedWaitModifyLookFromAction(action.getNeedWaitModifyLook());
        boolean needWait = ActionManager.INSTANCE.sendQueue(player).needWaitModifyLook;
        if (needWait || hitModifier != null) {
            skipIteration.set(true);
        }
        setCooldown(blockPos, ConfigUtils.getPlaceCooldown());
        if (reqItems != null)
            addHighlight(blockPos, HighlightType.PLACE);
        else
            addHighlight(blockPos, HighlightType.ADJUST);
    }

    @Override
    public void resetScanState() {
        super.resetScanState();
        watingForWaterPos = null;
        watingForWaterTicks = 0;
        placingIceForWater = false;
    }

    private void ensureIceBreakQueued(BlockPos pos) {
        if (!BreakUtils.INSTANCE.inQueue(pos) && !BreakUtils.INSTANCE.isBreaking(pos)) {
            BreakUtils.INSTANCE.add(pos);
        }
    }

    private void recordMissingMaterial(Item[] reqItems) {
        if (reqItems != null && reqItems.length > 0 && reqItems[0] != null) {
            MissingMaterialTracker.getInstance()
                    .recordMissing(reqItems[0], ctx.getRequiredBlockName());
        }
    }
}