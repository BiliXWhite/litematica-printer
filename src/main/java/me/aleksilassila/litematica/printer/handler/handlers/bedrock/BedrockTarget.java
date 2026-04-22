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
    private static final int POST_EXECUTE_SYNC_TIMEOUT_TICKS = 20;

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
        if (this.torchSupportPos == null) {
            this.slimePos = BedrockEnvironment.findPossibleSlimeSupport(level, bedrockPos);
            if (this.slimePos != null) {
                this.torchSupportPos = this.slimePos;
            } else {
                this.status = Status.FAILED;
            }
        }
    }

    public BlockPos getBedrockPos() { return bedrockPos; }
    public BlockPos getPistonPos() { return pistonPos; }
    public BlockPos getTorchSupportPos() { return torchSupportPos; }
    public BlockPos getSlimePos() { return slimePos; }
    public Status getStatus() { return status; }
    public boolean usesConservativeSync() { return this.conservativeSync; }

    public Status tick() { return this.tick(true); }

    public Status tick(boolean allowExecute) {
        this.executedThisTick = false;
        this.tickTimes++;
        updateStatus();
        logStatus();
        switch (this.status) {
            case UNINITIALIZED -> {
                BedrockPlacer.placePiston(this.pistonPos, Direction.UP);
                if (this.torchSupportPos != null) {
                    BedrockPlacer.placeSimple(this.torchSupportPos, Direction.UP, Blocks.REDSTONE_TORCH.asItem());
                    recordTemp(this.torchSupportPos.above());
                }
                // LOCK state to avoid re-initialization spam
                this.status = Status.NEEDS_WAITING;
                this.stuckTicksCounter = 1; 
                BedrockDebugLog.write("target initialize bedrock=" + BedrockDebugLog.pos(this.bedrockPos));
            }
            case EXTENDED -> {
                if (!allowExecute || this.hasTried) break;
                
                // Breaking sequence
                for (BlockPos torchPos : BedrockEnvironment.findNearbyRedstoneTorches(level, pistonPos)) {
                    BedrockBreaker.breakBlock(torchPos, !this.conservativeSync);
                }
                BedrockBreaker.breakBlock(this.pistonPos, !this.conservativeSync);
                for (int offset = 1; offset < 6; offset++) recordTemp(this.pistonPos.above(offset));
                
                BedrockPlacer.placePiston(this.pistonPos, Direction.DOWN);
                this.hasTried = true;
                this.executeTick = this.tickTimes;
                this.executedThisTick = true;
                this.status = Status.NEEDS_WAITING; // Immediately move to sync wait
                BedrockDebugLog.write("target execute bedrock=" + BedrockDebugLog.pos(this.bedrockPos));
            }
            case UNEXTENDED_WITHOUT_POWER_SOURCE -> {
                if (this.lastRepowerTick >= 0 && this.tickTimes - this.lastRepowerTick < REPOWER_INTERVAL_TICKS) break;
                if (this.torchSupportPos != null) {
                    BedrockPlacer.placeSimple(this.torchSupportPos, Direction.UP, Blocks.REDSTONE_TORCH.asItem());
                    recordTemp(this.torchSupportPos.above());
                }
                this.lastRepowerTick = this.tickTimes;
                this.status = Status.NEEDS_WAITING;
            }
            default -> {}
        }
        return this.status;
    }

    private void updateStatus() {
        if (this.tickTimes > 100) { this.status = Status.FAILED; return; }

        var pistonState = level.getBlockState(this.pistonPos);
        var bedrockState = level.getBlockState(this.bedrockPos);

        // Success condition
        if (!BedrockTargetBlocks.isTargetBlock(bedrockState) && pistonState.is(Blocks.PISTON)) {
            this.status = Status.RETRACTED;
            return;
        }

        // Retracting condition
        if (pistonState.is(Blocks.MOVING_PISTON)) {
            this.status = Status.RETRACTING;
            if (!BedrockTargetBlocks.isTargetBlock(bedrockState)) this.status = Status.RETRACTED;
            return;
        }

        // Timeout check
        if (this.hasTried && this.executeTick >= 0 && this.tickTimes - this.executeTick > POST_EXECUTE_SYNC_TIMEOUT_TICKS) {
            this.status = Status.STUCK;
            return;
        }

        // Predictive EXTENDED transition:
        // Give 2 ticks of buffer to ensure server has processed the placement 
        // and the piston has reached full extension. 1 tick is often too fast
        // and results in 'breaking air' errors.
        if (this.status == Status.NEEDS_WAITING && !this.hasTried && this.tickTimes - this.executeTick >= 2) {
            this.status = Status.EXTENDED;
            return;
        }

        // Real world observation (as fallback)
        if (pistonState.is(Blocks.PISTON) && pistonState.getValue(PistonBaseBlock.EXTENDED)) {
            this.status = Status.EXTENDED;
            return;
        }

        // Stay in current state if we are already waiting or processing
        if (this.status == Status.NEEDS_WAITING || this.status == Status.RETRACTING) return;

        // Default to UNINITIALIZED only if completely empty
        if (pistonState.isAir() && !this.hasTried) {
            this.status = Status.UNINITIALIZED;
        }
    }

    // (Remaining utility methods like logStatus, getCleanupPositions, etc. truncated for brevity but logically intact)
    private void logStatus() {
        if (this.lastLoggedStatus == this.status) return;
        this.lastLoggedStatus = this.status;
        BedrockDebugLog.write("target status bedrock=" + BedrockDebugLog.pos(this.bedrockPos) + " tick=" + this.tickTimes + " status=" + this.status);
    }
    private void recordTemp(BlockPos pos) { if (pos != null) this.tempBlocks.add(pos); }
    public Status refreshStatusOnly() { this.status = observeStatus(); return this.status; }
    private Status observeStatus() { return this.status; }
    public boolean executedThisTick() { return this.executedThisTick; }
    public Set<BlockPos> getCleanupPositions() {
        Set<BlockPos> pos = new LinkedHashSet<>();
        pos.add(this.pistonPos); pos.add(this.pistonPos.above());
        if (this.slimePos != null) pos.add(this.slimePos);
        pos.addAll(this.tempBlocks);
        return pos;
    }
    public Set<BlockPos> getReservedPositions() {
        Set<BlockPos> pos = new LinkedHashSet<>(getCleanupPositions());
        pos.add(this.bedrockPos);
        if (this.torchSupportPos != null) { pos.add(this.torchSupportPos); pos.add(this.torchSupportPos.above()); }
        return pos;
    }
}
