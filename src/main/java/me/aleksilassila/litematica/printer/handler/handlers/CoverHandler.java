package me.aleksilassila.litematica.printer.handler.handlers;

import com.google.common.collect.Iterables;
import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.core.action.ResourceLease;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeEvent;
import me.aleksilassila.litematica.printer.enums.FillBlockModeType;
import me.aleksilassila.litematica.printer.enums.PrintModeType;
import me.aleksilassila.litematica.printer.handler.FeatureModuleBase;
import me.aleksilassila.litematica.printer.handler.HudStatsManager;
import me.aleksilassila.litematica.printer.handler.HudStatus;
import me.aleksilassila.litematica.printer.handler.scan.ScanIntent;
import me.aleksilassila.litematica.printer.printer.PlayerLook;
import me.aleksilassila.litematica.printer.printer.PrinterBox;
import me.aleksilassila.litematica.printer.printer.PrinterUtils;
import me.aleksilassila.litematica.printer.printer.action.ActionPort;
import me.aleksilassila.litematica.printer.runtime.PrinterRuntime;
import me.aleksilassila.litematica.printer.utils.ConfigUtils;
import me.aleksilassila.litematica.printer.utils.InventoryUtils;
import me.aleksilassila.litematica.printer.utils.RegistryFilterResolver;
import me.aleksilassila.litematica.printer.utils.minecraft.BlockUtils;
import me.aleksilassila.litematica.printer.utils.minecraft.MessageUtils;
import me.aleksilassila.litematica.printer.utils.minecraft.PlayerUtils;
import me.aleksilassila.litematica.printer.utils.minecraft.SpawnCheckUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/** Places a configured block in the two-block spaces where Wither Skeletons can spawn. */
public final class CoverHandler extends FeatureModuleBase {
    public static final String NAME = "cover";

    private List<String> blockListCache = List.of();
    private Item[] coverItems = new Item[0];
    private final Set<BlockPos> retryTargets = new LinkedHashSet<>();
    private final Set<BlockPos> inFlightTargets = new LinkedHashSet<>();
    private final Map<BlockPos, Long> inFlightSince = new HashMap<>();
    private int observedScanConfigHash = Integer.MIN_VALUE;

    public CoverHandler(PrinterRuntime runtime) {
        super(runtime, NAME, PrintModeType.COVER, Configs.Core.COVER, Configs.Cover.COVER_SELECTION_TYPE, true);
        runtime.events().subscribe(event -> {
            if (event instanceof RuntimeEvent.BlockUpdated update) {
                BlockPos pos = new BlockPos(update.x(), update.y(), update.z());
                if (this.inFlightTargets.remove(pos)) {
                    this.inFlightSince.remove(pos);
                    if (this.isCoverTarget(pos)) {
                        this.retryTargets.add(pos);
                    }
                }
            }
        });
    }

    @Override
    protected int getTickInterval() {
        return Configs.Placement.PLACE_INTERVAL.getIntegerValue();
    }

    @Override
    protected int getMaxEffectiveExecutionsPerTick() {
        return Configs.Placement.PLACE_BLOCKS_PER_TICK.getIntegerValue();
    }

    @Override
    protected void preprocess() {
        this.refreshTargetStates();
        FillBlockModeType mode = (FillBlockModeType) Configs.Cover.COVER_BLOCK_MODE.getOptionListValue();
        if (mode == FillBlockModeType.HANDHELD) {
            this.missingMaterials.resolve(this.coverItems, null);
            // The handheld singleton must never be reused as a resolved block list.
            this.blockListCache = List.of();
            ItemStack held = this.player == null ? ItemStack.EMPTY : this.player.getMainHandItem();
            this.coverItems = held.getItem() instanceof BlockItem && !held.isEmpty() && held.getCount() > 0
                    ? new Item[]{held.getItem()}
                    : new Item[0];
            if (this.coverItems.length == 0) {
                this.hudStats.recordStatus(HudStatsManager.Mode.COVER, HudStatus.MAIN_HAND_NO_BLOCK);
            }
        } else {
            List<String> configured = Configs.Cover.COVER_BLOCK_LIST.getStrings();
            if (!configured.equals(this.blockListCache) || this.coverItems.length == 0 && !configured.isEmpty()) {
                this.missingMaterials.resolve(this.coverItems, null);
                this.blockListCache = List.copyOf(configured);
                this.coverItems = RegistryFilterResolver.resolveItems(configured).stream()
                        .filter(item -> item instanceof BlockItem)
                        .distinct()
                        .toArray(Item[]::new);
            }
            if (configured.isEmpty()) {
                this.coverItems = new Item[0];
            }
            if (configured.isEmpty()) {
                this.hudStats.recordStatus(HudStatsManager.Mode.COVER, HudStatus.LIST_NO_MATCH);
            } else if (this.coverItems.length == 0) {
                this.hudStats.recordStatus(HudStatsManager.Mode.COVER, HudStatus.LIST_NO_MATCH);
            }
        }

        int configHash = this.getScanConfigHash();
        if (this.observedScanConfigHash != Integer.MIN_VALUE && this.observedScanConfigHash != configHash) {
            this.scanEngine.resetOwner(NAME);
            this.requestFullScan();
        }
        this.observedScanConfigHash = configHash;
    }

    @Override
    protected void onRuntimeReset() {
        this.retryTargets.clear();
        this.inFlightTargets.clear();
        this.inFlightSince.clear();
        this.blockListCache = List.of();
        this.coverItems = new Item[0];
        this.observedScanConfigHash = Integer.MIN_VALUE;
    }

    @Override
    protected boolean canIterate() {
        return this.coverItems.length > 0;
    }

    @Override
    protected boolean hasRunnableIterationWork() {
        return !this.retryTargets.isEmpty();
    }

    @Override
    protected boolean hasWaitingIterationWork() {
        return !this.inFlightTargets.isEmpty();
    }

    @Override
    protected boolean iterationPositionsPrefilterReachAndSelection() {
        return true;
    }

    @Override
    protected boolean iterationPositionsAreExactCandidates() {
        return true;
    }

    @Override
    protected Iterable<BlockPos> getIterationPositions(PrinterBox interactionBox) {
        List<PrinterBox> sourceBoxes = this.getScanSourceBoxes(interactionBox);
        Predicate<BlockPos> selection = this.createSelectionRangePredicate();
        Predicate<BlockPos> reach = this.createScanReachPredicate();
        List<BlockPos> retained = this.retryTargets.stream()
                .filter(pos -> !this.inFlightTargets.contains(pos)
                        && reach.test(pos)
                        && selection.test(pos)
                        && this.isCoverTarget(pos))
                .toList();
        if (sourceBoxes.isEmpty()) {
            return retained;
        }

        Iterable<BlockPos> source = this.scanEngine.iterable(
                NAME,
                sourceBoxes,
                this.level,
                null,
                this.player,
                this.getScanGuardLimit(),
                ScanIntent.CUSTOM,
                pos -> this.isCoverTarget(pos)
                        && !this.retryTargets.contains(pos)
                        && !this.inFlightTargets.contains(pos),
                pos -> reach.test(pos) && selection.test(pos)
        );
        return retained.isEmpty() ? source : Iterables.concat(retained, source);
    }

    @Override
    public boolean canIterationBlockPos(BlockPos pos) {
        return this.coverItems.length > 0 && this.isCoverTarget(pos);
    }

    @Override
    protected void executeIteration(BlockPos pos, AtomicReference<Boolean> skipIteration) {
        if (this.inFlightTargets.contains(pos)) {
            this.setIterationConsumedEffectiveExecution(false);
            return;
        }

        BlockState currentState = this.level.getBlockState(pos);
        if (!this.isCoverTarget(currentState, pos)) {
            this.setIterationConsumedEffectiveExecution(false);
            return;
        }

        ItemStack held = this.player.getMainHandItem();
        boolean handheld = Configs.Cover.COVER_BLOCK_MODE.getOptionListValue() == FillBlockModeType.HANDHELD;
        if (Configs.Placement.FALLING_CHECK.getBooleanValue()
                && held.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof FallingBlock block
                && FallingBlock.isFree(this.level.getBlockState(pos.below()))) {
            this.hudStats.recordDeferred(HudStatsManager.Mode.COVER, HudStatus.FALLING_NO_SUPPORT);
            MessageUtils.setOverlayMessage(I18n.FALLING_BLOCK_NO_SUPPORT.getName(block.getName().getString()));
            this.retryTargets.add(pos.immutable());
            this.setIterationConsumedEffectiveExecution(false);
            return;
        }

        if (!handheld && !InventoryUtils.switchToItems(this.player, this.coverItems)) {
            if (this.hasAnyCoverMaterial()) {
                this.missingMaterials.resolve(this.coverItems, null);
            } else if (this.actionBroker.isResourceHeld(ResourceLease.INVENTORY)) {
                this.hudStats.recordDeferred(HudStatsManager.Mode.COVER, HudStatus.WAITING_RETRIEVAL);
                this.missingMaterials.resolve(this.coverItems, null);
            } else {
                this.hudStats.recordDeferred(HudStatsManager.Mode.COVER, HudStatus.MISSING_MATERIAL);
                this.missingMaterials.recordMissing(this.coverItems, null, null, this.level.getGameTime());
            }
            this.retryTargets.add(pos.immutable());
            this.setIterationConsumedEffectiveExecution(false);
            return;
        }
        if (!handheld) {
            this.missingMaterials.resolve(this.coverItems, null);
        }

        BlockPos support = pos.below();
        if (!PrinterUtils.canBeClicked(this.level, support) || BlockUtils.isReplaceable(this.level.getBlockState(support))) {
            this.hudStats.recordDeferred(HudStatsManager.Mode.COVER, HudStatus.NO_VALID_FACE);
            this.retryTargets.add(pos.immutable());
            this.setIterationConsumedEffectiveExecution(false);
            return;
        }

        double interactionRange = PlayerUtils.getPlayerBlockInteractionRange();
        Vec3 hitPos = Vec3.atCenterOf(support).add(0.0D, 0.5D, 0.0D);
        if (!this.canReachIterationPosition(pos)
                || this.player.getEyePosition().distanceToSqr(hitPos) > interactionRange * interactionRange) {
            this.retryTargets.add(pos.immutable());
            this.setIterationConsumedEffectiveExecution(false);
            return;
        }

        Item[] expectedItems = handheld ? new Item[]{held.getItem()} : this.coverItems;
        if (!this.actionBroker.queueClick(
                support,
                Direction.UP,
                Vec3.ZERO,
                false,
                1,
                expectedItems,
                ActionPort.ActionSource.COVER
        )) {
            this.hudStats.recordDeferred(HudStatsManager.Mode.COVER, HudStatus.ACTION_QUEUE_BUSY);
            this.retryTargets.add(pos.immutable());
            this.setIterationConsumedEffectiveExecution(false);
            skipIteration.set(true);
            return;
        }
        this.actionBroker.setLook(new PlayerLook(Direction.UP));
        this.actionBroker.setWaitForHorizontalLook(false);
        ActionPort.SendResult sendResult = this.actionBroker.sendQueue(this.player);
        if (sendResult.isWaiting()) {
            this.hudStats.recordDeferred(HudStatsManager.Mode.COVER, HudStatus.WAITING_LOOK);
            this.retryTargets.add(pos.immutable());
            skipIteration.set(true);
            return;
        }
        if (!sendResult.isSent()) {
            this.hudStats.recordDeferred(HudStatsManager.Mode.COVER, HudStatus.PLACEMENT_NOT_SENT);
            this.retryTargets.add(pos.immutable());
            this.setIterationConsumedEffectiveExecution(false);
            return;
        }

        BlockPos retainedPos = pos.immutable();
        this.retryTargets.remove(pos);
        this.inFlightTargets.add(retainedPos);
        this.inFlightSince.put(retainedPos, this.level.getGameTime());
        this.hudStats.trackExpectedBlockChange(HudStatsManager.Mode.COVER, pos, currentState);
        this.hudStats.recordRateUnit(HudStatsManager.Mode.COVER, 1);
        this.hudStats.recordStatus(HudStatsManager.Mode.COVER, HudStatus.RUNNING);
        this.setBlockPosCooldown(pos, ConfigUtils.getPlaceCooldown());
    }

    private boolean hasAnyCoverMaterial() {
        if (this.player == null || this.coverItems.length == 0) {
            return false;
        }
        for (Item item : this.coverItems) {
            if (InventoryUtils.playerHasItemInInventory(this.player, item)) {
                return true;
            }
        }
        return false;
    }

    private void refreshTargetStates() {
        if (this.level == null) {
            return;
        }
        this.retryTargets.removeIf(pos -> !this.isCoverTarget(pos));
        long now = this.level.getGameTime();
        List<BlockPos> stale = new ArrayList<>();
        for (Map.Entry<BlockPos, Long> entry : this.inFlightSince.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!this.inFlightTargets.contains(pos) || !this.isCoverTarget(pos)) {
                stale.add(pos);
            } else if (now > entry.getValue()) {
                this.inFlightTargets.remove(pos);
                this.retryTargets.add(pos);
                stale.add(pos);
            }
        }
        for (BlockPos pos : stale) {
            this.inFlightSince.remove(pos);
        }
    }

    private int getScanConfigHash() {
        int result = Configs.Cover.COVER_SELECTION_TYPE.getOptionListValue().hashCode();
        result = 31 * result + Configs.Cover.COVER_BLOCK_MODE.getOptionListValue().hashCode();
        result = 31 * result + Configs.Cover.COVER_BLOCK_LIST.getStrings().hashCode();
        result = 31 * result + Boolean.hashCode(Configs.Core.CHECK_PLAYER_INTERACTION_RANGE.getBooleanValue());
        result = 31 * result + ConfigUtils.getWorkRange();
        return result;
    }

    private boolean isCoverTarget(BlockPos pos) {
        return this.level != null && this.isCoverTarget(this.level.getBlockState(pos), pos);
    }

    private boolean isCoverTarget(BlockState state, BlockPos pos) {
        return this.level != null
                && BlockUtils.isReplaceable(state)
                && SpawnCheckUtils.canWitherSkeletonSpawn(this.level, pos);
    }
}
