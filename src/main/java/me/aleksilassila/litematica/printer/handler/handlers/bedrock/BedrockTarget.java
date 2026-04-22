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
    private static final int REPOWER_INTERVAL_TICKS = 2;
    private static final int POST_EXECUTE_SYNC_TIMEOUT_TICKS = 10;

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
    private final BlockPos bedrockPos;
    private final BlockPos pistonPos;
    private final boolean conservativeSync;
    private BlockPos torchSupportPos;
    private BlockPos slimePos;
    private int tickTimes;
    private boolean hasTried;
    private int stuckTicksCounter;
    private int lastRepowerTick = -1;
    private int executeTick = -1;
    private boolean executedThisTick;
    private Status status = Status.UNINITIALIZED;
    private Status lastLoggedStatus;
    public final Set<BlockPos> tempBlocks = new LinkedHashSet<>();

    public BedrockTarget(BlockPos bedrockPos, ClientLevel level) {
        this.bedrockPos = bedrockPos;
        this.level = level;
        this.pistonPos = bedrockPos.above();
        this.conservativeSync = BedrockTargetBlocks.requiresConservativeSync(level.getBlockState(bedrockPos));
        this.torchSupportPos = BedrockEnvironment.findTorchSupport(level, bedrockPos);
        if (this.conservativeSync) {
            BedrockDebugLog.write("target init conservative sync bedrock=" + BedrockDebugLog.pos(this.bedrockPos));
        }
        if (this.torchSupportPos == null) {
            this.slimePos = BedrockEnvironment.findPossibleSlimeSupport(level, bedrockPos);
            if (this.slimePos != null) {
                this.torchSupportPos = this.slimePos;
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

    public BlockPos getTorchSupportPos() {
        return torchSupportPos;
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
        return this.tick(true);
    }

    public Status tick(boolean allowExecute) {
        this.executedThisTick = false;
        
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
                // Initial placement doesn't count towards the 40-tick limit until it finishes.
                BedrockPlacer.placePiston(this.pistonPos, Direction.UP);
                if (this.torchSupportPos != null) {
                    BedrockPlacer.placeSimple(this.torchSupportPos, Direction.UP, Blocks.REDSTONE_TORCH.asItem());
                    recordTemp(this.torchSupportPos.above());
                }
                BedrockDebugLog.write("target initialize bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                        + " piston=" + BedrockDebugLog.pos(this.pistonPos)
                        + " torchSupport=" + BedrockDebugLog.pos(this.torchSupportPos));
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
                for (BlockPos torchPos : BedrockEnvironment.findNearbyRedstoneTorches(level, pistonPos)) {
                    BedrockBreaker.breakBlock(torchPos, !this.conservativeSync);
                }
                BedrockBreaker.breakBlock(this.pistonPos, !this.conservativeSync);
                for (int offset = 1; offset < 6; offset++) {
                    recordTemp(this.pistonPos.above(offset));
                }
                BedrockPlacer.placePiston(this.pistonPos, Direction.DOWN);
                this.hasTried = true;
                this.executeTick = this.tickTimes;
                this.executedThisTick = true;
                BedrockDebugLog.write("target execute bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                        + " piston=" + BedrockDebugLog.pos(this.pistonPos)
                        + " torches=" + BedrockEnvironment.findNearbyRedstoneTorches(level, pistonPos).size()
                        + " tick=" + this.tickTimes);
            }
            case UNEXTENDED_WITHOUT_POWER_SOURCE -> {
                if (this.lastRepowerTick >= 0 && this.tickTimes - this.lastRepowerTick < REPOWER_INTERVAL_TICKS) {
                    BedrockDebugLog.write("target repower delayed bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                            + " torchSupport=" + BedrockDebugLog.pos(this.torchSupportPos)
                            + " tick=" + this.tickTimes
                            + " cooldown=" + REPOWER_INTERVAL_TICKS);
                    break;
                }
                if (this.torchSupportPos != null) {
                    BedrockPlacer.placeSimple(this.torchSupportPos, Direction.UP, Blocks.REDSTONE_TORCH.asItem());
                    recordTemp(this.torchSupportPos.above());
                }
                this.lastRepowerTick = this.tickTimes;
                BedrockDebugLog.write("target repower bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                        + " torchSupport=" + BedrockDebugLog.pos(this.torchSupportPos)
                        + " tick=" + this.tickTimes);
            }
            case RETRACTED, FAILED, STUCK, NEEDS_WAITING, RETRACTING, UNEXTENDED_WITH_POWER_SOURCE -> {
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

    public Set<BlockPos> getCleanupPositions() {
        LinkedHashSet<BlockPos> positions = new LinkedHashSet<>();
        positions.add(this.pistonPos);
        positions.add(this.pistonPos.above());
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
        positions.add(this.pistonPos.above());
        if (this.torchSupportPos != null) {
            positions.add(this.torchSupportPos);
            positions.add(this.torchSupportPos.above());
        }
        if (this.slimePos != null) {
            positions.add(this.slimePos);
            positions.add(this.slimePos.above());
        }
        positions.addAll(this.tempBlocks);
        return positions;
    }

    public Set<BlockPos> getMachineFootprint() {
        LinkedHashSet<BlockPos> positions = new LinkedHashSet<>(getReservedPositions());
        positions.addAll(BedrockEnvironment.getTorchInfluencePositions(this.pistonPos));
        return positions;
    }

    private void recordTemp(BlockPos pos) {
        if (pos != null) {
            this.tempBlocks.add(pos);
        }
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
                + " slime=" + BedrockDebugLog.pos(this.slimePos)
                + " bedrockState=" + BedrockDebugLog.describeState(bedrockState)
                + " pistonState=" + BedrockDebugLog.describePistonState(pistonState));
    }

    private void updateStatus() {
        if (this.tickTimes > 40) {
            this.status = Status.FAILED;
            return;
        }
        if (!BedrockEnvironment.isTorchSupportUsable(level, this.torchSupportPos)) {
            if (BedrockEnvironment.isSlimeSupportUsable(level, this.slimePos)) {
                if (!level.getBlockState(this.slimePos).is(Blocks.SLIME_BLOCK)) {
                    BedrockPlacer.placeSimple(this.slimePos, Direction.UP, Blocks.SLIME_BLOCK.asItem());
                    recordTemp(this.slimePos);
                }
                this.torchSupportPos = this.slimePos;
            } else {
                BlockPos naturalSupport = BedrockEnvironment.findTorchSupport(level, bedrockPos);
                if (naturalSupport != null) {
                    this.torchSupportPos = naturalSupport;
                    if (this.slimePos != null && !level.getBlockState(this.slimePos).is(Blocks.SLIME_BLOCK)) {
                        this.slimePos = null;
                    }
                } else {
                    BlockPos newSlimePos = this.slimePos;
                    if (newSlimePos == null || !BedrockEnvironment.isSlimeSupportUsable(level, newSlimePos)) {
                        newSlimePos = BedrockEnvironment.findPossibleSlimeSupport(level, bedrockPos);
                    }
                    if (newSlimePos != null) {
                        this.slimePos = newSlimePos;
                        if (!level.getBlockState(this.slimePos).is(Blocks.SLIME_BLOCK)) {
                            BedrockPlacer.placeSimple(this.slimePos, Direction.UP, Blocks.SLIME_BLOCK.asItem());
                            recordTemp(this.slimePos);
                            BedrockDebugLog.write("target materialized slime support bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                                    + " slime=" + BedrockDebugLog.pos(this.slimePos)
                                    + " tick=" + this.tickTimes);
                        }
                        this.torchSupportPos = this.slimePos;
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
        if (hasExceededSyncWaitTimeout()) {
            this.status = Status.STUCK;
            BedrockDebugLog.write("target sync timeout bedrock=" + BedrockDebugLog.pos(this.bedrockPos)
                    + " tick=" + this.tickTimes
                    + " executeTick=" + this.executeTick
                    + " pistonState=" + BedrockDebugLog.describePistonState(level.getBlockState(this.pistonPos)));
            return;
        }
        if (this.hasTried && level.getBlockState(this.pistonPos).isAir() && level.getBlockState(this.pistonPos.above()).is(Blocks.PISTON_HEAD)) {
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
        if (this.hasTried && (level.getBlockState(this.pistonPos).is(Blocks.PISTON) || level.getBlockState(this.pistonPos).isAir()) && this.stuckTicksCounter < 15) {
            this.status = Status.NEEDS_WAITING;
            this.stuckTicksCounter++;
            return;
        }
        if (level.getBlockState(this.pistonPos).is(Blocks.PISTON)
                && !level.getBlockState(this.pistonPos).getValue(PistonBaseBlock.EXTENDED)
                && !BedrockEnvironment.findNearbyRedstoneTorches(this.level, this.pistonPos).isEmpty()
                && BedrockTargetBlocks.isTargetBlock(level.getBlockState(this.bedrockPos))) {
            if (level.getBlockState(this.pistonPos).getValue(PistonBaseBlock.FACING) == Direction.DOWN) {
                this.status = Status.STUCK;
                this.hasTried = false;
                this.stuckTicksCounter = 0;
            } else {
                this.status = Status.UNEXTENDED_WITH_POWER_SOURCE;
            }
            return;
        }
        if (level.getBlockState(this.pistonPos).is(Blocks.PISTON)
                && !level.getBlockState(this.pistonPos).getValue(PistonBaseBlock.EXTENDED)
                && level.getBlockState(this.pistonPos).getValue(PistonBaseBlock.FACING) == Direction.UP
                && BedrockEnvironment.findNearbyRedstoneTorches(this.level, this.pistonPos).isEmpty()
                && BedrockTargetBlocks.isTargetBlock(level.getBlockState(this.bedrockPos))) {
            this.status = Status.UNEXTENDED_WITHOUT_POWER_SOURCE;
            return;
        }
        if (BedrockEnvironment.hasRoomForPiston(this.level, this.bedrockPos)) {
            this.status = Status.UNINITIALIZED;
            return;
        }
        if (level.getBlockState(this.pistonPos).is(Blocks.PISTON)
                && level.getBlockState(this.pistonPos).getValue(PistonBaseBlock.FACING) != Direction.UP
                && level.getBlockState(this.pistonPos).getValue(PistonBaseBlock.FACING) != Direction.DOWN) {
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
        if (hasExceededSyncWaitTimeout()) {
            return Status.STUCK;
        }
        if (this.hasTried && level.getBlockState(this.pistonPos).isAir() && level.getBlockState(this.pistonPos.above()).is(Blocks.PISTON_HEAD)) {
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
        if (this.hasTried && (level.getBlockState(this.pistonPos).is(Blocks.PISTON) || level.getBlockState(this.pistonPos).isAir()) && this.stuckTicksCounter < 15) {
            return Status.NEEDS_WAITING;
        }
        if (level.getBlockState(this.pistonPos).is(Blocks.PISTON)
                && !level.getBlockState(this.pistonPos).getValue(PistonBaseBlock.EXTENDED)
                && !BedrockEnvironment.findNearbyRedstoneTorches(this.level, this.pistonPos).isEmpty()
                && BedrockTargetBlocks.isTargetBlock(level.getBlockState(this.bedrockPos))) {
            if (level.getBlockState(this.pistonPos).getValue(PistonBaseBlock.FACING) == Direction.DOWN) {
                return Status.STUCK;
            }
            return Status.UNEXTENDED_WITH_POWER_SOURCE;
        }
        if (level.getBlockState(this.pistonPos).is(Blocks.PISTON)
                && !level.getBlockState(this.pistonPos).getValue(PistonBaseBlock.EXTENDED)
                && level.getBlockState(this.pistonPos).getValue(PistonBaseBlock.FACING) == Direction.UP
                && BedrockEnvironment.findNearbyRedstoneTorches(this.level, this.pistonPos).isEmpty()
                && BedrockTargetBlocks.isTargetBlock(level.getBlockState(this.bedrockPos))) {
            return Status.UNEXTENDED_WITHOUT_POWER_SOURCE;
        }
        if (BedrockEnvironment.hasRoomForPiston(this.level, this.bedrockPos)) {
            return Status.UNINITIALIZED;
        }
        if (level.getBlockState(this.pistonPos).is(Blocks.PISTON)
                && level.getBlockState(this.pistonPos).getValue(PistonBaseBlock.FACING) != Direction.UP
                && level.getBlockState(this.pistonPos).getValue(PistonBaseBlock.FACING) != Direction.DOWN) {
            return Status.UNINITIALIZED;
        }
        return this.status;
    }

    private boolean hasExceededSyncWaitTimeout() {
        return this.hasTried
                && this.executeTick >= 0
                && this.tickTimes - this.executeTick >= POST_EXECUTE_SYNC_TIMEOUT_TICKS
                && (level.getBlockState(this.pistonPos).is(Blocks.PISTON)
                || level.getBlockState(this.pistonPos).isAir()
                || level.getBlockState(this.pistonPos).is(Blocks.MOVING_PISTON));
    }
}
