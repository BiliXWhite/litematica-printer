package me.aleksilassila.litematica.printer.handler.handlers;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.enums.ExcavateListMode;
import me.aleksilassila.litematica.printer.enums.PrintModeType;
import me.aleksilassila.litematica.printer.handler.FeatureModuleBase;
import me.aleksilassila.litematica.printer.handler.HudStatsManager;
import me.aleksilassila.litematica.printer.handler.HudStatus;
import me.aleksilassila.litematica.printer.handler.TickContext;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeEvent;
import me.aleksilassila.litematica.printer.handler.scan.ScanIntent;
import me.aleksilassila.litematica.printer.mixin_extension.BlockBreakResult;
import me.aleksilassila.litematica.printer.interaction.MiningInteractionPort;
import me.aleksilassila.litematica.printer.printer.PrinterBox;
import me.aleksilassila.litematica.printer.printer.PlayerLook;
import me.aleksilassila.litematica.printer.printer.PrinterUtils;
import me.aleksilassila.litematica.printer.printer.action.ActionPort;
import me.aleksilassila.litematica.printer.utils.ConfigUtils;
import me.aleksilassila.litematica.printer.utils.InteractionUtils;
import me.aleksilassila.litematica.printer.utils.InventoryUtils;
import me.aleksilassila.litematica.printer.utils.RegistryFilterResolver;
import me.aleksilassila.litematica.printer.integration.tweakeroo.TweakerooAdapter;
import me.aleksilassila.litematica.printer.utils.minecraft.BlockUtils;
import me.aleksilassila.litematica.printer.utils.minecraft.PlayerUtils;
import me.aleksilassila.litematica.printer.runtime.PrinterRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public class MineHandler extends FeatureModuleBase {
    public static final String NAME = "mine";
    private final MineBreakExecutor analyzer;
    private final TweakerooAdapter tweakeroo;
    private final MineToolSession toolSession;
    private final MineCandidateQueue candidates = new MineCandidateQueue();
    private final Map<BlockPos, BlockState> candidateStates = new HashMap<>();
    private final TrenchFillState trenchFill = new TrenchFillState();
    private List<String> trenchFillFilters;
    private Item[] trenchFillItems = new Item[0];
    @Nullable
    private BlockPos activeMinePos;

    public MineHandler(PrinterRuntime runtime) {
        super(runtime, NAME, PrintModeType.MINE, Configs.Core.MINE, Configs.Mine.MINE_SELECTION_TYPE, true);
        this.tweakeroo = runtime.tweakeroo();
        this.toolSession = new MineToolSession(this.tweakeroo);
        this.analyzer = new MineBreakExecutor(runtime.client(), this.tweakeroo);
        runtime.events().subscribe(event -> {
            if (event instanceof RuntimeEvent.BlockUpdated update) {
                BlockPos pos = new BlockPos(update.x(), update.y(), update.z());
                this.trenchFill.onBlockUpdated(pos,
                        this.level != null && this.isFluid(this.level.getBlockState(pos)));
            }
        });
    }

    @Override
    public void tick(TickContext context) {
        if (!ConfigUtils.isEnable() || !ConfigUtils.isMineMode()) {
            this.analyzer.reset();
            this.activeMinePos = null;
            this.toolSession.reset();
            this.trenchFill.clear();
        }
        super.tick(context);
    }

    public int getRetryQueueSize() {
        return this.candidates.size() + this.trenchFill.targetCount()
                + (this.activeMinePos == null ? 0 : 1);
    }

    public boolean shouldKeepMiningOnIdleInput() {
        return ConfigUtils.isEnable() && ConfigUtils.isMineMode() && this.activeMinePos != null
                && this.mc != null && this.level != null && this.player != null && this.connection != null
                && this.mc.level == this.level && this.mc.player == this.player
                && this.mc.getConnection() == this.connection && this.mc.gameMode == this.gameMode
                && this.gameMode instanceof MiningInteractionPort port
                && port.isDestroying() && port.matchesDestroyTarget(this.activeMinePos);
    }

    private boolean mineRestriction(BlockState blockState) {
        if (!InteractionUtils.breakRestriction(blockState)) {
            return false;
        }
        if (Configs.Mine.EXCAVATE_LIMITER.getOptionListValue().equals(ExcavateListMode.TWEAKEROO)) {
            return this.tweakeroo.allowsBreak(blockState);
        }
        return this.tweakeroo.allowsConfiguredBreak(blockState);
    }

    @Override
    protected int getTickInterval() {
        return Configs.Break.BREAK_INTERVAL.getIntegerValue();
    }

    @Override
    protected int getMaxEffectiveExecutionsPerTick() {
        return 0;
    }

    @Override
    protected int getScanGuardLimit() {
        return 0;
    }

    @Override
    protected Iterable<BlockPos> getIterationPositions(PrinterBox playerInteractionBox) {
        if (this.hasTrenchFillWork()) {
            if (this.trenchFill.hasWaterloggedTargets()) {
                List<BlockPos> waterlogged = this.trenchFill.readyWaterloggedTargets(this::canReachTrenchTarget);
                if (!waterlogged.isEmpty()) return waterlogged;
            }
            List<BlockPos> fillPositions = this.trenchFill.readyFillTargets(
                    this.level.getGameTime(), pos -> this.isFluid(this.level.getBlockState(pos)),
                    this::canReachTrenchTarget);
            if (!fillPositions.isEmpty()) {
                Iterable<BlockPos> minePositions = this.candidates.isEmpty()
                        ? this.getMineScanPositions(playerInteractionBox)
                        : List.of();
                return () -> Stream.concat(
                        fillPositions.stream(),
                        StreamSupport.stream(minePositions.spliterator(), false)
                ).iterator();
            }
        }
        if (!this.candidates.isEmpty()) {
            return List.of();
        }
        return this.getMineScanPositions(playerInteractionBox);
    }

    private Iterable<BlockPos> getMineScanPositions(PrinterBox playerInteractionBox) {
        List<PrinterBox> scanSourceBoxes = this.getScanSourceBoxes(playerInteractionBox);
        if (scanSourceBoxes.isEmpty()) {
            return List.of();
        }
        Predicate<BlockPos> selectionPredicate = this.createSelectionRangePredicate();
        Predicate<BlockPos> reachPredicate = this.createScanReachPredicate();
        
        return this.scanEngine.iterable(
                NAME,
                scanSourceBoxes,
                this.level,
                this.litematica.schematicWorld(),
                this.player,
                this.getScanGuardLimit(),
                ScanIntent.MINE,
                pos -> this.isMineScanCandidate(pos, false),
                pos -> reachPredicate.test(pos) && selectionPredicate.test(pos)
        );
    }

    @Override
    protected void preprocess() {
        this.analyzer.beginTick();
        this.toolSession.beginTick();
        this.trenchFill.beginTick();
        this.refreshTrenchFillTargets();
        this.continueActiveMineTarget();
    }

    @Override
    protected boolean hasRunnableIterationWork() {
        return !this.candidates.isEmpty() || this.hasTrenchFillWork();
    }

    @Override
    protected boolean hasWaitingIterationWork() {
        return this.activeMinePos != null || this.hasTrenchFillWork();
    }

    @Override
    protected void onRuntimeReset() {
        this.candidates.clear();
        this.candidateStates.clear();
        this.activeMinePos = null;
        this.trenchFill.clear();
        this.trenchFillFilters = null;
        this.trenchFillItems = new Item[0];
        this.analyzer.reset();
        this.toolSession.reset();
    }

    @Override
    protected boolean canIterate() {
        return this.activeMinePos == null && !InteractionUtils.getRuntime().hasActiveDestroyTarget();
    }

    @Override
    protected boolean iterationPositionsPrefilterReachAndSelection() {
        return true;
    }

    @Override
    protected boolean iterationPositionsPrefilterCooldown() {
        return false;
    }

    @Override
    protected boolean iterationPositionsAreExactCandidates() {
        return true;
    }

    @Override
    public boolean canIterationBlockPos(BlockPos pos) {
        if (this.hasTrenchFillWork() && this.trenchFill.hasFillTarget(pos)) {
            return this.level != null
                    && this.trenchFill.canAttempt(pos, this.level.getGameTime())
                    && this.isFluid(this.level.getBlockState(pos));
        }
        if (this.hasTrenchFillWork() && this.trenchFill.hasWaterloggedTarget(pos)) {
            return this.level != null && this.isWaterlogged(this.level.getBlockState(pos));
        }
        return this.isMineScanCandidate(pos, true);
    }

    @Override
    protected void executeIteration(BlockPos blockPos, AtomicReference<Boolean> skipIteration) {
        if (this.hasTrenchFillWork() && this.trenchFill.hasFillTarget(blockPos)) {
            this.executeTrenchFill(blockPos, skipIteration);
            return;
        }
        if (this.hasTrenchFillWork() && this.trenchFill.hasWaterloggedTarget(blockPos)) {
            this.executeTrenchWaterloggedBreak(blockPos, skipIteration);
            return;
        }
        MineBreakExecutor.Target target = this.analyzer.analyze(blockPos, this.candidateStates.remove(blockPos));
        if (target == null) {
            this.setIterationConsumedEffectiveExecution(false);
            return;
        }
        this.candidates.add(target);
        this.setIterationConsumedEffectiveExecution(false);
    }

    @Override
    protected void stopIteration(boolean interrupt) {
        if (this.actionBroker.isWaitingForLook() || this.activeMinePos != null || this.candidates.isEmpty()) {
            return;
        }
        List<MineBreakExecutor.Target> orderedCandidates = this.refreshCandidateTargets();
        if (orderedCandidates.isEmpty()) {
            return;
        }
        MineBreakExecutor.Target nearest = orderedCandidates.get(0);
        MineBreakExecutor.Target selected = this.toolSession.selectTarget(orderedCandidates, this.analyzer, this.player);
        this.executeToolSession(selected, orderedCandidates);
    }

    private List<MineBreakExecutor.Target> refreshCandidateTargets() {
        List<MineBreakExecutor.Target> refreshed = new ArrayList<>();
        for (MineBreakExecutor.Target queued : this.candidates.snapshot()) {
            BlockPos pos = queued.pos();
            this.candidateStates.remove(pos);
            if (!this.isMineScanCandidate(pos, true)) {
                this.candidates.remove(pos);
                continue;
            }
            MineBreakExecutor.Target current = this.analyzer.analyze(pos);
            if (current == null) {
                this.candidates.remove(pos);
                continue;
            }
            this.candidates.add(current);
            refreshed.add(current);
        }
        refreshed.sort(this.toolSession.comparator(this.player));
        return refreshed;
    }

    private void continueActiveMineTarget() {
        BlockPos pos = this.activeMinePos;
        if (pos == null) {
            return;
        }
        if (!this.canContinueActiveMineTarget(pos)) {
            this.activeMinePos = null;
            return;
        }
        BlockBreakResult result = InteractionUtils.getRuntime().continueDestroyBlockForMine(pos, Direction.DOWN, true);
        MineResultReporter.record(pos, result);
        this.toolSession.onTargetResolved(result, pos);
        if (result != BlockBreakResult.IN_PROGRESS) {
            this.activeMinePos = null;
            this.setBlockPosCooldown(pos, ConfigUtils.getBreakCooldown());
        }
    }

    private boolean canContinueActiveMineTarget(BlockPos pos) {
        return pos != null
                && this.canReachIterationPosition(pos)
                && InteractionUtils.canBreakBlock(pos)
                && mineRestriction(this.level.getBlockState(pos));
    }

    private boolean isMineScanCandidate(BlockPos pos, boolean checkReach) {
        if (pos == null || this.level == null || this.player == null || this.gameMode == null) {
            return false;
        }

        if (InteractionUtils.getRuntime().isRecentlyBroken(pos)
                || InteractionUtils.getRuntime().isPendingDelayedDestroy(pos)) {
            return false;
        }

        BlockState state = this.level.getBlockState(pos);
        if (state.isAir() || state.getBlock() instanceof LiquidBlock) {
            return false;
        }

        if (Configs.Break.BREAK_CHECK_HARDNESS.getBooleanValue()
                && state.getDestroySpeed(this.level, pos) < 0.0F) {
            return false;
        }

        if (checkReach && !this.canReachIterationPosition(pos)) {
            return false;
        }
        if (this.player.blockActionRestricted(this.level, pos, this.gameMode.getPlayerMode())
                || !mineRestriction(state)) {
            return false;
        }
        this.candidateStates.put(pos.immutable(), state);
        return true;
    }

    private void executeToolSession(MineBreakExecutor.Target firstTarget,
                                    List<MineBreakExecutor.Target> orderedCandidates) {
        this.toolSession.startSession(firstTarget);
        if (!this.toolSession.ensureHandToolProtected(this.player, firstTarget)) {
            return;
        }
        // Batch dispatch: the per-tick budget lives on the session (BREAK_BLOCKS_PER_TICK,
        // 0 = unlimited). The controller has no per-call fast budget — delta>=0.7 blocks always
        // take the same-tick START+STOP path, throttled only by the session budget and the
        // durability guard.
        BlockBreakResult result = this.executeSessionTarget(firstTarget, !this.analyzer.isCurrentToolEffective(firstTarget));
        if (this.toolSession.shouldStop(result, this.activeMinePos != null)) {
            return;
        }
        for (MineBreakExecutor.Target queuedTarget : orderedCandidates) {
            if (queuedTarget.pos().equals(firstTarget.pos())) {
                continue;
            }
            if (!this.toolSession.hasInstantBudget()) {
                break;
            }
            if (!this.toolSession.matchesSessionTool(this.analyzer, queuedTarget)) {
                continue;
            }
            if (!this.toolSession.ensureHandToolProtected(this.player, queuedTarget)) {
                break;
            }
            result = this.executeSessionTarget(queuedTarget, false);
            if (this.toolSession.shouldStop(result, this.activeMinePos != null)) {
                break;
            }
        }
    }

    private BlockBreakResult executeSessionTarget(MineBreakExecutor.Target target, boolean allowToolSwitch) {
        boolean switchForRecovery = this.player != null
                && target.shouldSwitchToRecoveryTool(this.player.getMainHandItem());
        BlockBreakResult result = this.executeMineTarget(target, allowToolSwitch || switchForRecovery);
        if (result != BlockBreakResult.FAILED) {
            this.setBlockPosCooldown(target.pos(), ConfigUtils.getBreakCooldown());
        }
        if (result == BlockBreakResult.COMPLETED || result == BlockBreakResult.COMPLETED_WAIT) {
            // Batch dispatch: each same-tick START+STOP is an independent server judgment, so the
            // budget ticks down per dispatched block, not per server slot.
            this.toolSession.consumeInstantBudget();
        }
        this.toolSession.onTargetResolved(result, target.pos());
        MineResultReporter.record(target.pos(), result);
        if (result != BlockBreakResult.IN_PROGRESS) {
            this.removeCandidate(target.pos());
        }
        return result;
    }

    private BlockBreakResult executeMineTarget(MineBreakExecutor.Target target, boolean allowToolSwitch) {
        BlockBreakResult result = InteractionUtils.getRuntime().continueDestroyBlockForMine(target.pos(), Direction.DOWN, allowToolSwitch);
        if (result == BlockBreakResult.IN_PROGRESS) {
            this.activeMinePos = target.pos();
        }
        return result;
    }

    private void removeCandidate(BlockPos pos) {
        if (pos == null) {
            return;
        }
        this.candidates.remove(pos);
    }

    private boolean hasTrenchFillWork() {
        return Configs.Mine.MINE_TRENCH_MODE.getBooleanValue() && this.trenchFill.hasWork();
    }

    private void refreshTrenchFillTargets() {
        if (!Configs.Mine.MINE_TRENCH_MODE.getBooleanValue()
                || this.level == null || this.playerInteractionBox == null) {
            this.trenchFill.clear();
            return;
        }
        PrinterBox interactionBox = this.playerInteractionBox.get();
        if (interactionBox == null) return;
        Set<BlockPos> discovered = new LinkedHashSet<>();
        Set<BlockPos> waterlogged = new LinkedHashSet<>();
        List<PrinterBox> sourceBoxes = this.getScanSourceBoxes(interactionBox);
        this.discoverTrenchBoundary(sourceBoxes, discovered, waterlogged);
        // The trench scope is the selected volume plus exactly one horizontal boundary block.
        // Do not retain old targets: those could be distant sea grass from the same water body.
        long now = this.level.getGameTime();
        this.trenchFill.updateTargets(sourceBoxes, discovered, waterlogged, now,
                pos -> this.isFluid(this.level.getBlockState(pos)));
    }

    private void refreshTrenchFillMaterials() {
        List<String> filters = Configs.Mine.MINE_TRENCH_FILL_BLOCK_LIST.getStrings();
        if (!filters.equals(this.trenchFillFilters)) {
            this.trenchFillFilters = new ArrayList<>(filters);
            this.trenchFillItems = RegistryFilterResolver.resolveItems(filters).stream()
                    .filter(item -> item instanceof BlockItem)
                    .distinct()
                    .toArray(Item[]::new);
        }
    }

    private void executeTrenchFill(BlockPos pos, AtomicReference<Boolean> skipIteration) {
        if (this.level == null || this.player == null || !this.isFluid(this.level.getBlockState(pos))) {
            this.trenchFill.removeFillTarget(pos);
            return;
        }
        if (!this.canReachTrenchTarget(pos)) return;
        long now = this.level.getGameTime();
        if (!this.trenchFill.canSend(now,
                this.placementRateController.effectiveIntervalTicks(),
                Configs.Placement.PLACE_BLOCKS_PER_TICK.getIntegerValue(),
                this.placementRateController.canSend(now))) {
            skipIteration.set(true);
            return;
        }
        this.refreshTrenchFillMaterials();
        if (this.trenchFillItems.length == 0) {
            this.hudStats.recordStatus(HudStatsManager.Mode.MINE,
                    this.trenchFillFilters.isEmpty() ? HudStatus.FILL_LIST_EMPTY : HudStatus.LIST_NO_MATCH);
            this.deferTrenchFill(pos, now + 20L);
            skipIteration.set(true);
            return;
        }
        if (!InventoryUtils.switchToItems(this.player, this.trenchFillItems)) {
            this.hudStats.recordStatus(HudStatsManager.Mode.MINE, HudStatus.MISSING_FILL_MATERIAL);
            this.missingMaterials.recordMissing(this.trenchFillItems, null, null, now);
            this.deferTrenchFill(pos, now + 20L);
            skipIteration.set(true);
            return;
        }
        this.missingMaterials.resolve(this.trenchFillItems, null);
        Direction supportDirection = this.findTrenchSupport(pos);
        if (supportDirection == null) {
            this.deferTrenchFill(pos, now + 10L);
            skipIteration.set(true);
            return;
        }
        if (!this.actionBroker.queueClick(
                pos.relative(supportDirection),
                supportDirection.getOpposite(),
                net.minecraft.world.phys.Vec3.ZERO,
                false,
                1,
                this.trenchFillItems,
                ActionPort.ActionSource.FILL)) {
            this.deferTrenchFill(pos, now + 2L);
            skipIteration.set(true);
            return;
        }
        this.actionBroker.setLook(new PlayerLook(supportDirection.getOpposite()));
        this.actionBroker.setWaitForHorizontalLook(false);
        ActionPort.SendResult result = this.actionBroker.sendQueue(this.player);
        if (result.isSent() || result.isWaiting()) {
            this.trenchFill.markInFlight(pos, now, result.isSent());
            if (result.isSent()) {
                this.placementRateController.recordSent(now);
            } else {
                skipIteration.set(true);
            }
        } else {
            this.deferTrenchFill(pos, now + 2L);
            skipIteration.set(true);
        }
    }

    private void deferTrenchFill(BlockPos pos, long retryAt) {
        this.trenchFill.defer(pos, retryAt);
    }

    private void executeTrenchWaterloggedBreak(BlockPos pos, AtomicReference<Boolean> skipIteration) {
        if (this.level == null || !this.isWaterlogged(this.level.getBlockState(pos))) {
            this.trenchFill.removeWaterloggedTarget(pos);
            return;
        }
        if (!this.canReachTrenchTarget(pos)) {
            skipIteration.set(true);
            return;
        }
        if (this.activeMinePos != null && !this.activeMinePos.equals(pos)) {
            skipIteration.set(true);
            return;
        }
        BlockBreakResult result = InteractionUtils.getRuntime()
                .continueDestroyBlockForMine(pos, Direction.DOWN, true);
        MineResultReporter.record(pos, result);
        if (result == BlockBreakResult.IN_PROGRESS) {
            this.activeMinePos = pos.immutable();
        } else {
            this.trenchFill.removeWaterloggedTarget(pos);
        }
        skipIteration.set(true);
    }

    private void discoverTrenchBoundary(
            List<PrinterBox> boxes,
            Set<BlockPos> discovered,
            Set<BlockPos> waterlogged
    ) {
        for (PrinterBox box : boxes) {
            for (BlockPos pos : box) {
                BlockState state = this.level.getBlockState(pos);
                if (this.isFluid(state)) {
                    discovered.add(pos.immutable());
                } else if (this.isWaterlogged(state)) {
                    waterlogged.add(pos.immutable());
                }
                for (Direction side : HORIZONTAL_SIDES) {
                    BlockPos outside = pos.relative(side);
                    if (!box.contains(outside)) {
                        BlockState outsideState = this.level.getBlockState(outside);
                        if (this.isFluid(outsideState)) {
                            discovered.add(outside.immutable());
                        } else if (this.isWaterlogged(outsideState)) {
                            waterlogged.add(outside.immutable());
                        }
                    }
                }
            }
        }
    }

    private Direction findTrenchSupport(BlockPos pos) {
        Direction[] order = {Direction.DOWN, Direction.NORTH, Direction.SOUTH,
                Direction.EAST, Direction.WEST, Direction.UP};
        for (Direction direction : order) {
            BlockPos support = pos.relative(direction);
            BlockState state = this.level.getBlockState(support);
            if (!BlockUtils.isReplaceable(state) && PrinterUtils.canBeClicked(this.level, support)
                    && this.canReachTrenchSupport(support, direction.getOpposite())) {
                return direction;
            }
        }
        return null;
    }

    private boolean canReachTrenchTarget(BlockPos pos) {
        return this.player != null && this.canReachIterationPosition(pos)
                && PlayerUtils.isWithinBlockInteractionRange(this.player, pos, 0.0D);
    }

    private boolean canReachTrenchSupport(BlockPos support, Direction clickedFace) {
        if (this.player == null) return false;
        Vec3 faceCenter = Vec3.atCenterOf(support).add(
                clickedFace.getStepX() * 0.5D,
                clickedFace.getStepY() * 0.5D,
                clickedFace.getStepZ() * 0.5D);
        double range = PlayerUtils.getPlayerBlockInteractionRange();
        return this.player.getEyePosition().distanceToSqr(faceCenter) <= range * range;
    }

    private boolean isFluid(BlockState state) {
        return state != null && state.getBlock() instanceof LiquidBlock;
    }

    private static final Direction[] HORIZONTAL_SIDES = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    private boolean isWaterlogged(BlockState state) {
        return state != null
                && !(state.getBlock() instanceof LiquidBlock)
                && !state.getFluidState().isEmpty();
    }

}
