package me.aleksilassila.litematica.printer.handler.handlers.bedrock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;

import java.util.LinkedHashSet;
import java.util.Set;

public class BedrockTarget {
    private static final int BASE_REPOWER_INTERVAL_TICKS = 2;
    private static final int FAST_REPOWER_INTERVAL_TICKS = 1;
    private static final int BASE_POWERED_STALL_RECOVERY_TICKS = 2;
    private static final int FAST_POWERED_STALL_RECOVERY_TICKS = 1;
    private static final int BASE_POST_EXECUTE_SYNC_TIMEOUT_TICKS = 16;
    private static final int FAST_POST_EXECUTE_SYNC_TIMEOUT_TICKS = 12;

    public enum Status {
        FAILED,
        UNINITIALIZED,
        UNEXTENDED_WITH_POWER_SOURCE,
        UNEXTENDED_WITHOUT_POWER_SOURCE,
        EXTENDED,
        NEEDS_WAITING,
        RETRACTING,
        RETRACTED,
        STUCK
    }

    private final ClientLevel level;
    private final BedrockMachineLayout layout;
    private final BlockPos bedrockPos;
    private final BlockPos pistonPos;
    private final BlockPos headPos;
    private final boolean conservativeSync;
    private BedrockTorchPlacement torchPlacement;
    private BlockPos torchSupportPos;
    private BlockPos slimePos;
    private int tickTimes;
    private boolean hasTried;
    private int stuckTicksCounter;
    private int lastRepowerTick = -1;
    private int executeTick = -1;
    private boolean executedThisTick;
    private boolean initializedThisTick;
    private Status status = Status.UNINITIALIZED;
    private Status lastLoggedStatus;
    public final Set<BlockPos> tempBlocks = new LinkedHashSet<>();

    public BedrockTarget(BlockPos bedrockPos, ClientLevel level) {
        this.bedrockPos = bedrockPos;
        this.level = level;
        this.layout = BedrockMachineLayout.find(level, bedrockPos);
        if (this.layout == null) {
            this.pistonPos = bedrockPos.above();
            this.headPos = this.pistonPos.above();
            this.status = Status.FAILED;
            BedrockDebugLog.write("target init failed bedrock=" + BedrockDebugLog.pos(this.bedrockPos) + " reason=no_machine_layout");
            this.conservativeSync = BedrockTargetBlocks.requiresConservativeSync(level.getBlockState(bedrockPos));
            return;
        }
        this.pistonPos = this.layout.getPistonPos();
        this.headPos = this.layout.getHeadPos();
        this.conservativeSync = BedrockTargetBlocks.requiresConservativeSync(level.getBlockState(bedrockPos));
        this.torchPlacement = BedrockEnvironment.findTorchPlacement(level, this.pistonPos, this.layout.getPistonOffset().getOpposite(), this.bedrockPos, this.pistonPos, this.headPos);
        this.torchSupportPos = getTorchSupportFromPlacement();
        if (this.conservativeSync) {
            BedrockDebugLog.write("target init conservative sync bedrock=" + BedrockDebugLog.pos(this.bedrockPos));
        }
        if (this.torchPlacement == null) {
            BedrockTorchPlacement slimePlacement = BedrockEnvironment.findPossibleSlimeTorchPlacement(level, this.pistonPos, this.layout.getPistonOffset().getOpposite(), this.bedrockPos, this.pistonPos, this.headPos);
            if (slimePlacement != null) {
                this.slimePos = slimePlacement.getSupportPos();
                this.torchPlacement = slimePlacement;
                this.torchSupportPos = getTorchSupportFromPlacement();
                BedrockDebugLog.write("target init reserved slime support bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                        + " slime=" + BedrockDebugLog.pos(this.slimePos));
            } else {
                this.status = Status.FAILED;
                BedrockDebugLog.write("target init failed bedrock=" + BedrockDebugLog.pos(this.bedrockPos) + " reason=no_torch_support");
            }
        }
    }

    public BlockPos getBedrockPos() {
        return bedrockPos;
    }

    public BlockPos getPistonPos() {
        return pistonPos;
    }

    public BlockPos getHeadPos() {
        return headPos;
    }

    public BlockPos getTorchSupportPos() {
        return torchSupportPos;
    }

    public BlockPos getTorchPos() {
        return this.torchPlacement == null ? null : this.torchPlacement.getTorchPos();
    }

    public BlockPos getSlimePos() {
        return slimePos;
    }

    public Status getStatus() {
        return status;
    }

    public boolean usesConservativeSync() {
        return this.conservativeSync;
    }

    public Status tick() {
        return this.tick(true, true);
    }

    public Status tick(boolean allowExecute) {
        return this.tick(allowExecute, true);
    }

    public Status tick(boolean allowExecute, boolean allowInitialize) {
        this.executedThisTick = false;
        this.initializedThisTick = false;
        
        // Only increment tick counter if we are actually doing something or waiting for sync.
        // This prevents tasks from failing while they are just queued in the controller.
        if (this.status != Status.UNINITIALIZED && this.status != Status.EXTENDED) {
            this.tickTimes++;
        } else if (this.status == Status.EXTENDED && allowExecute) {
            this.tickTimes++;
        }

        updateStatus();
        logStatus();
        switch (this.status) {
            case UNINITIALIZED -> {
                if (!allowInitialize) {
                    BedrockDebugLog.write("target initialize delayed bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                            + " tick=" + this.tickTimes
                            + " reason=init_budget");
                    break;
                }
                if (!canBuildInitialMachine()) {
                    BedrockDebugLog.write("target initialize deferred bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                            + " reason=missing_required_items");
                    break;
                }
                // Initial placement doesn't count towards the 40-tick limit until it finishes.
                if (!BedrockPlacer.placePiston(this.pistonPos, this.layout.getPrimingFacing())) {
                    BedrockDebugLog.write("target initialize deferred bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                            + " reason=place_piston_failed");
                    break;
                }
                if (this.torchSupportPos != null) {
                    if (!placeTorch()) {
                        BedrockDebugLog.write("target initialize deferred bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                                + " reason=place_torch_failed");
                        break;
                    }
                }
                BedrockDebugLog.write("target initialize bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                        + " piston=" + BedrockDebugLog.pos(this.pistonPos)
                        + " torchSupport=" + BedrockDebugLog.pos(this.torchSupportPos)
                        + " torch=" + BedrockDebugLog.pos(getTorchPos()));
                this.initializedThisTick = true;
            }
            case EXTENDED -> {
                if (!allowExecute) {
                    BedrockDebugLog.write("target execute delayed bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                            + " tick=" + this.tickTimes);
                    break;
                }
                if (this.hasTried) {
                    BedrockDebugLog.write("target execute waiting sync bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                            + " tick=" + this.tickTimes);
                    break;
                }
                for (BlockPos torchPos : getOwnedTorchPositions()) {
                    BedrockBreaker.breakBlock(torchPos, Direction.DOWN, !this.conservativeSync);
                }
                BedrockBreaker.breakBlock(this.pistonPos, this.layout.getExecuteFacing(), !this.conservativeSync);
                for (int offset = 1; offset < 6; offset++) {
                    recordTemp(this.pistonPos.relative(this.layout.getPistonOffset(), offset));
                }
                BedrockPlacer.placePiston(this.pistonPos, this.layout.getExecuteFacing());
                this.hasTried = true;
                this.executeTick = this.tickTimes;
                this.executedThisTick = true;
                BedrockDebugLog.write("target execute bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                        + " piston=" + BedrockDebugLog.pos(this.pistonPos)
                        + " torches=" + BedrockEnvironment.findNearbyRedstoneTorches(level, pistonPos).size()
                        + " tick=" + this.tickTimes);
            }
            case UNEXTENDED_WITHOUT_POWER_SOURCE -> {
                if (!tryRepowerTorch("missing_power_source")) {
                    break;
                }
            }
            case UNEXTENDED_WITH_POWER_SOURCE -> {
                int poweredStallRecoveryTicks = getPoweredStallRecoveryTicks();
                if (this.tickTimes < poweredStallRecoveryTicks) {
                    BedrockDebugLog.write("target powered stall waiting bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                            + " tick=" + this.tickTimes
                            + " torchSupport=" + BedrockDebugLog.pos(this.torchSupportPos)
                            + " recoveryTicks=" + poweredStallRecoveryTicks);
                    break;
                }
                if (!BedrockInventory.hasAtLeast(Blocks.PISTON.asItem(), 1)) {
                    BedrockDebugLog.write("target powered stall deferred bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                            + " tick=" + this.tickTimes
                            + " reason=missing_piston");
                    break;
                }
                if (!hasOwnedTorchPowerSource()) {
                    if (!tryRepowerTorch("owned_torch_missing")) {
                        break;
                    }
                    break;
                }

                int repowerIntervalTicks = getRepowerIntervalTicks();
                if (this.lastRepowerTick >= 0 && this.tickTimes - this.lastRepowerTick < repowerIntervalTicks) {
                    BedrockDebugLog.write("target powered stall delayed bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                            + " tick=" + this.tickTimes
                            + " cooldown=" + repowerIntervalTicks);
                    break;
                }

                for (BlockPos torchPos : getOwnedTorchPositions()) {
                    BedrockBreaker.breakBlock(torchPos, Direction.DOWN, !this.conservativeSync);
                }
                if (!level.getBlockState(this.pistonPos).isAir()) {
                    BedrockBreaker.breakBlock(this.pistonPos, this.layout.getPrimingFacing(), !this.conservativeSync);
                }
                if (!BedrockPlacer.placePiston(this.pistonPos, this.layout.getPrimingFacing())) {
                    BedrockDebugLog.write("target powered stall deferred bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                            + " tick=" + this.tickTimes
                            + " reason=replace_piston_failed");
                    break;
                }
                if (!tryRepowerTorch("powered_stall_rebuild")) {
                    break;
                }

                this.lastRepowerTick = this.tickTimes;
                BedrockDebugLog.write("target powered stall rebuild bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                        + " piston=" + BedrockDebugLog.pos(this.pistonPos)
                        + " torchSupport=" + BedrockDebugLog.pos(this.torchSupportPos)
                        + " tick=" + this.tickTimes);
            }
            case RETRACTED, FAILED, STUCK, NEEDS_WAITING, RETRACTING -> {
            }
        }
        return this.status;
    }

    public Status refreshStatusOnly() {
        this.status = observeStatus();
        logStatus();
        return this.status;
    }

    public Status refreshStatusOnlyAndAdvance() {
        this.tickTimes++;
        updateStatus();
        logStatus();
        return this.status;
    }

    public boolean executedThisTick() {
        return this.executedThisTick;
    }

    public boolean initializedThisTick() {
        return this.initializedThisTick;
    }

    public Set<BlockPos> getCleanupPositions() {
        LinkedHashSet<BlockPos> positions = new LinkedHashSet<>();
        positions.add(this.pistonPos);
        positions.add(this.headPos);
        if (this.slimePos != null) {
            positions.add(this.slimePos);
        }
        positions.addAll(this.tempBlocks);
        return positions;
    }

    public Set<BlockPos> getReservedPositions() {
        LinkedHashSet<BlockPos> positions = new LinkedHashSet<>();
        positions.add(this.bedrockPos);
        positions.add(this.pistonPos);
        positions.add(this.headPos);
        if (this.torchSupportPos != null) {
            positions.add(this.torchSupportPos);
        }
        if (getTorchPos() != null) {
            positions.add(getTorchPos());
        }
        if (this.slimePos != null) {
            positions.add(this.slimePos);
        }
        positions.addAll(this.tempBlocks);
        return positions;
    }

    public Set<BlockPos> getMachineFootprint() {
        LinkedHashSet<BlockPos> positions = new LinkedHashSet<>(getReservedPositions());
        positions.addAll(BedrockEnvironment.getTorchInfluencePositions(this.pistonPos));
        return positions;
    }

    public Set<BlockPos> getOwnedTorchPositions() {
        LinkedHashSet<BlockPos> positions = new LinkedHashSet<>();
        BlockPos torchPos = getTorchPos();
        if (torchPos != null && BedrockEnvironment.isRedstoneTorchAt(this.level, torchPos)) {
            positions.add(torchPos);
        }
        return positions;
    }

    private void recordTemp(BlockPos pos) {
        if (pos != null) {
            this.tempBlocks.add(pos);
        }
    }

    private boolean canBuildInitialMachine() {
        if (!BedrockInventory.hasAtLeast(Blocks.PISTON.asItem(), 1)) {
            return false;
        }
        if (!BedrockInventory.hasAtLeast(Blocks.REDSTONE_TORCH.asItem(), 1)) {
            return false;
        }
        if (this.slimePos != null && !level.getBlockState(this.slimePos).is(Blocks.SLIME_BLOCK)
                && !BedrockInventory.hasAtLeast(Blocks.SLIME_BLOCK.asItem(), 1)) {
            return false;
        }
        return true;
    }

    private boolean hasOwnedTorchPowerSource() {
        return !getOwnedTorchPositions().isEmpty();
    }

    private boolean tryRepowerTorch(String reason) {
        if (!BedrockInventory.hasAtLeast(Blocks.REDSTONE_TORCH.asItem(), 1)) {
            BedrockDebugLog.write("target repower deferred bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                    + " torchSupport=" + BedrockDebugLog.pos(this.torchSupportPos)
                    + " tick=" + this.tickTimes
                    + " reason=missing_redstone_torch"
                    + " context=" + reason);
            return false;
        }
        int repowerIntervalTicks = getRepowerIntervalTicks();
        if (this.lastRepowerTick >= 0 && this.tickTimes - this.lastRepowerTick < repowerIntervalTicks) {
            BedrockDebugLog.write("target repower delayed bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                    + " torchSupport=" + BedrockDebugLog.pos(this.torchSupportPos)
                    + " tick=" + this.tickTimes
                    + " cooldown=" + repowerIntervalTicks
                    + " context=" + reason);
            return false;
        }
        if (this.torchSupportPos == null) {
            BedrockDebugLog.write("target repower deferred bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                    + " tick=" + this.tickTimes
                    + " reason=no_torch_support"
                    + " context=" + reason);
            return false;
        }
        if (!placeTorch()) {
            BedrockDebugLog.write("target repower deferred bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                    + " torchSupport=" + BedrockDebugLog.pos(this.torchSupportPos)
                    + " tick=" + this.tickTimes
                    + " reason=place_torch_failed"
                    + " context=" + reason);
            return false;
        }
        this.lastRepowerTick = this.tickTimes;
        BedrockDebugLog.write("target repower bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                + " torchSupport=" + BedrockDebugLog.pos(this.torchSupportPos)
                + " torch=" + BedrockDebugLog.pos(getTorchPos())
                + " tick=" + this.tickTimes
                + " context=" + reason);
        return true;
    }

    private void logStatus() {
        if (this.lastLoggedStatus == this.status) {
            return;
        }
        this.lastLoggedStatus = this.status;
        var bedrockState = this.level.getBlockState(this.bedrockPos);
        var pistonState = this.level.getBlockState(this.pistonPos);
        int torchCount = BedrockEnvironment.findNearbyRedstoneTorches(this.level, this.pistonPos).size();
        BedrockDebugLog.write("target status bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                + " tick=" + this.tickTimes
                + " status=" + this.status
                + " hasTried=" + this.hasTried
                + " stuckTicks=" + this.stuckTicksCounter
                + " torchCount=" + torchCount
                + " conservativeSync=" + this.conservativeSync
                + " torchSupport=" + BedrockDebugLog.pos(this.torchSupportPos)
                + " torch=" + BedrockDebugLog.pos(getTorchPos())
                + " slime=" + BedrockDebugLog.pos(this.slimePos)
                + " bedrockState=" + BedrockDebugLog.describeState(bedrockState)
                + " pistonState=" + BedrockDebugLog.describePistonState(pistonState));
    }

    private void updateStatus() {
        if (this.tickTimes > 40) {
            this.status = Status.FAILED;
            return;
        }
        
        if (!BedrockEnvironment.isTorchPlacementUsable(level, this.torchPlacement)) {
            if (this.slimePos != null && this.torchPlacement != null && this.slimePos.equals(this.torchPlacement.getSupportPos())
                    && BedrockEnvironment.isSlimePlacementUsable(level, this.torchPlacement)) {
                if (!level.getBlockState(this.slimePos).is(Blocks.SLIME_BLOCK)) {
                    BedrockPlacer.placeSimple(this.slimePos, Direction.UP, Blocks.SLIME_BLOCK.asItem());
                    recordTemp(this.slimePos);
                }
                this.torchSupportPos = getTorchSupportFromPlacement();
            } else {
                BedrockTorchPlacement naturalPlacement = BedrockEnvironment.findTorchPlacement(level, this.pistonPos, this.layout.getPistonOffset().getOpposite(), this.bedrockPos, this.pistonPos, this.headPos);
                if (naturalPlacement != null) {
                    this.torchPlacement = naturalPlacement;
                    this.torchSupportPos = getTorchSupportFromPlacement();
                    if (this.slimePos != null && !level.getBlockState(this.slimePos).is(Blocks.SLIME_BLOCK) && !this.slimePos.equals(this.torchSupportPos)) {
                        this.slimePos = null;
                    }
                } else {
                    BedrockTorchPlacement slimePlacement = this.torchPlacement;
                    if (slimePlacement == null || !BedrockEnvironment.isSlimePlacementUsable(level, slimePlacement) || !slimePlacement.getSupportPos().equals(this.slimePos)) {
                        slimePlacement = BedrockEnvironment.findPossibleSlimeTorchPlacement(level, this.pistonPos, this.layout.getPistonOffset().getOpposite(), this.bedrockPos, this.pistonPos, this.headPos);
                    }
                    if (slimePlacement != null) {
                        this.torchPlacement = slimePlacement;
                        this.slimePos = slimePlacement.getSupportPos();
                        if (!level.getBlockState(this.slimePos).is(Blocks.SLIME_BLOCK)) {
                            BedrockPlacer.placeSimple(this.slimePos, Direction.UP, Blocks.SLIME_BLOCK.asItem());
                            recordTemp(this.slimePos);
                            BedrockDebugLog.write("target materialized slime support bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                                    + " slime=" + BedrockDebugLog.pos(this.slimePos)
                                    + " tick=" + this.tickTimes);
                        }
                        this.torchSupportPos = getTorchSupportFromPlacement();
                    } else {
                        this.status = Status.FAILED;
                        BedrockMessages.actionBar("bedrockminer.fail.place.redstonetorch");
                        return;
                    }
                }
            }
        }
        if (!BedrockTargetBlocks.isTargetBlock(level.getBlockState(this.bedrockPos))
                && level.getBlockState(this.pistonPos).is(Blocks.PISTON)) {
            this.status = Status.RETRACTED;
            return;
        }
        if (level.getBlockState(this.pistonPos).is(Blocks.MOVING_PISTON)) {
            this.status = Status.RETRACTING;
            // Immediate retirement if bedrock is gone to speed up throughput
            if (!BedrockTargetBlocks.isTargetBlock(level.getBlockState(this.bedrockPos))) {
                this.status = Status.RETRACTED;
            }
            return;
        }
        if (!BedrockTargetBlocks.isTargetBlock(level.getBlockState(this.bedrockPos))
                && hasMachineCleanupResidue()) {
            this.status = Status.RETRACTED;
            return;
        }
        if (hasExceededSyncWaitTimeout()) {
            Status recoveryStatus = getRecoverablePostExecuteStatus();
            if (recoveryStatus != null) {
                resetPostExecuteAttempt("sync_timeout_recover", recoveryStatus);
                this.status = recoveryStatus;
            } else {
                this.status = Status.STUCK;
                BedrockDebugLog.write("target sync timeout bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                        + " tick=" + this.tickTimes
                        + " executeTick=" + this.executeTick
                        + " pistonState=" + BedrockDebugLog.describePistonState(level.getBlockState(this.pistonPos)));
            }
            return;
        }
        if (this.hasTried && level.getBlockState(this.pistonPos).isAir() && level.getBlockState(this.headPos).is(Blocks.PISTON_HEAD)) {
            this.status = Status.NEEDS_WAITING;
            this.stuckTicksCounter++;
            return;
        }
        if (this.hasTried
                && level.getBlockState(this.pistonPos).is(Blocks.PISTON)
                && level.getBlockState(this.pistonPos).getValue(PistonBaseBlock.EXTENDED)) {
            this.status = Status.NEEDS_WAITING;
            this.stuckTicksCounter++;
            return;
        }
        if (level.getBlockState(this.pistonPos).is(Blocks.PISTON) && level.getBlockState(this.pistonPos).getValue(PistonBaseBlock.EXTENDED)) {
            this.status = Status.EXTENDED;
            return;
        }
        if (this.hasTried && level.getBlockState(this.pistonPos).isAir() && !level.getBlockState(this.headPos).is(Blocks.PISTON_HEAD)) {
            this.status = Status.UNINITIALIZED;
            this.hasTried = false;
            this.stuckTicksCounter = 0;
            this.executeTick = -1;
            return;
        }
        Status recoverableUnextendedStatus = getRecoverableUnextendedStatus();
        if (recoverableUnextendedStatus != null) {
            this.status = recoverableUnextendedStatus;
            if (this.hasTried) {
                this.hasTried = false;
                this.stuckTicksCounter = 0;
                this.executeTick = -1;
            }
            return;
        }
        if (this.hasTried && (level.getBlockState(this.pistonPos).is(Blocks.PISTON) || level.getBlockState(this.pistonPos).isAir()) && this.stuckTicksCounter < 15) {
            this.status = Status.NEEDS_WAITING;
            this.stuckTicksCounter++;
            return;
        }
        if (this.hasTried && hasPostExecuteSyncResidue()) {
            this.status = Status.NEEDS_WAITING;
            this.stuckTicksCounter++;
            return;
        }
        if (BedrockEnvironment.hasRoomForPiston(this.level, this.pistonPos, this.layout.getPistonOffset())) {
            this.status = Status.UNINITIALIZED;
            return;
        }
        if (level.getBlockState(this.pistonPos).is(Blocks.PISTON)
                && level.getBlockState(this.pistonPos).getValue(PistonBaseBlock.FACING) != this.layout.getPrimingFacing()
                && level.getBlockState(this.pistonPos).getValue(PistonBaseBlock.FACING) != this.layout.getExecuteFacing()) {
            this.status = Status.UNINITIALIZED;
            return;
        }
        this.status = Status.FAILED;
        BedrockMessages.actionBar("bedrockminer.fail.place.piston");
    }

    private Status observeStatus() {
        if (!BedrockTargetBlocks.isTargetBlock(level.getBlockState(this.bedrockPos))
                && level.getBlockState(this.pistonPos).is(Blocks.PISTON)) {
            return Status.RETRACTED;
        }
        if (level.getBlockState(this.pistonPos).is(Blocks.MOVING_PISTON)) {
            return Status.RETRACTING;
        }
        if (!BedrockTargetBlocks.isTargetBlock(level.getBlockState(this.bedrockPos))
                && hasMachineCleanupResidue()) {
            return Status.RETRACTED;
        }
        if (hasExceededSyncWaitTimeout()) {
            Status recoveryStatus = getRecoverablePostExecuteStatus();
            if (recoveryStatus != null) {
                return recoveryStatus;
            }
            return Status.STUCK;
        }
        if (this.hasTried && level.getBlockState(this.pistonPos).isAir() && level.getBlockState(this.headPos).is(Blocks.PISTON_HEAD)) {
            return Status.NEEDS_WAITING;
        }
        if (this.hasTried
                && level.getBlockState(this.pistonPos).is(Blocks.PISTON)
                && level.getBlockState(this.pistonPos).getValue(PistonBaseBlock.EXTENDED)) {
            return Status.NEEDS_WAITING;
        }
        if (level.getBlockState(this.pistonPos).is(Blocks.PISTON) && level.getBlockState(this.pistonPos).getValue(PistonBaseBlock.EXTENDED)) {
            return Status.EXTENDED;
        }
        if (this.hasTried && level.getBlockState(this.pistonPos).isAir() && !level.getBlockState(this.headPos).is(Blocks.PISTON_HEAD)) {
            return Status.UNINITIALIZED;
        }
        Status recoverableUnextendedStatus = getRecoverableUnextendedStatus();
        if (recoverableUnextendedStatus != null) {
            return recoverableUnextendedStatus;
        }
        if (this.hasTried && (level.getBlockState(this.pistonPos).is(Blocks.PISTON) || level.getBlockState(this.pistonPos).isAir()) && this.stuckTicksCounter < 15) {
            return Status.NEEDS_WAITING;
        }
        if (this.hasTried && hasPostExecuteSyncResidue()) {
            return Status.NEEDS_WAITING;
        }
        if (BedrockEnvironment.hasRoomForPiston(this.level, this.pistonPos, this.layout.getPistonOffset())) {
            return Status.UNINITIALIZED;
        }
        if (level.getBlockState(this.pistonPos).is(Blocks.PISTON)
                && level.getBlockState(this.pistonPos).getValue(PistonBaseBlock.FACING) != this.layout.getPrimingFacing()
                && level.getBlockState(this.pistonPos).getValue(PistonBaseBlock.FACING) != this.layout.getExecuteFacing()) {
            return Status.UNINITIALIZED;
        }
        return this.status;
    }

    private boolean hasExceededSyncWaitTimeout() {
        int syncTimeoutTicks = getPostExecuteSyncTimeoutTicks();
        return this.hasTried
                && this.executeTick >= 0
                && this.tickTimes - this.executeTick >= syncTimeoutTicks
                && (level.getBlockState(this.pistonPos).isAir() || hasPostExecuteSyncResidue());
    }

    private int getRepowerIntervalTicks() {
        return BedrockInventory.shouldUseFastBreakProfile() ? FAST_REPOWER_INTERVAL_TICKS : BASE_REPOWER_INTERVAL_TICKS;
    }

    private int getPoweredStallRecoveryTicks() {
        return BedrockInventory.shouldUseFastBreakProfile() ? FAST_POWERED_STALL_RECOVERY_TICKS : BASE_POWERED_STALL_RECOVERY_TICKS;
    }

    private int getPostExecuteSyncTimeoutTicks() {
        return BedrockInventory.shouldUseFastBreakProfile() ? FAST_POST_EXECUTE_SYNC_TIMEOUT_TICKS : BASE_POST_EXECUTE_SYNC_TIMEOUT_TICKS;
    }

    private Status getRecoverablePostExecuteStatus() {
        if (!this.hasTried || this.executeTick < 0) {
            return null;
        }
        return getRecoverableUnextendedStatus();
    }

    private Status getRecoverableUnextendedStatus() {
        if (!BedrockTargetBlocks.isTargetBlock(level.getBlockState(this.bedrockPos))) {
            return null;
        }
        if (!level.getBlockState(this.pistonPos).is(Blocks.PISTON)
                || level.getBlockState(this.pistonPos).getValue(PistonBaseBlock.EXTENDED)) {
            return null;
        }

        Direction facing = level.getBlockState(this.pistonPos).getValue(PistonBaseBlock.FACING);
        if (facing != this.layout.getPrimingFacing() && facing != this.layout.getExecuteFacing()) {
            return null;
        }
        if (hasOwnedTorchPowerSource()) {
            return Status.UNEXTENDED_WITH_POWER_SOURCE;
        }
        return Status.UNEXTENDED_WITHOUT_POWER_SOURCE;
    }

    private void resetPostExecuteAttempt(String reason, Status recoveryStatus) {
        this.hasTried = false;
        this.stuckTicksCounter = 0;
        this.executeTick = -1;
        BedrockDebugLog.write("target recover bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                + " tick=" + this.tickTimes
                + " status=" + recoveryStatus
                + " reason=" + reason
                + " pistonState=" + BedrockDebugLog.describePistonState(level.getBlockState(this.pistonPos)));
    }

    private boolean hasMachineCleanupResidue() {
        return hasCleanupResidue(this.pistonPos)
                || hasCleanupResidue(this.headPos)
                || hasCleanupResidue(this.slimePos)
                || hasCleanupResidue(getTorchPos());
    }

    private boolean hasPostExecuteSyncResidue() {
        return hasCleanupResidue(this.pistonPos) || hasCleanupResidue(this.headPos);
    }

    private boolean hasCleanupResidue(BlockPos pos) {
        return pos != null && BedrockTargetBlocks.isCleanupResidue(level.getBlockState(pos));
    }

    private boolean placeTorch() {
        if (this.torchPlacement == null || this.torchSupportPos == null) {
            return false;
        }
        if (!BedrockPlacer.placeSimple(this.torchSupportPos, this.torchPlacement.getClickedFace(), Blocks.REDSTONE_TORCH.asItem())) {
            return false;
        }
        recordTemp(getTorchPos());
        return true;
    }

    private BlockPos getTorchSupportFromPlacement() {
        return this.torchPlacement == null ? null : this.torchPlacement.getSupportPos();
    }
}
