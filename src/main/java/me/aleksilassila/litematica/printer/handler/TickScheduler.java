package me.aleksilassila.litematica.printer.handler;

import com.google.common.collect.ImmutableList;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeComponent;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeEvent;
import me.aleksilassila.litematica.printer.handler.handlers.GuiHandler;
import me.aleksilassila.litematica.printer.handler.handlers.MineDebugLog;
import me.aleksilassila.litematica.printer.core.action.ResourceLease;
import net.minecraft.client.Minecraft;
import me.aleksilassila.litematica.printer.runtime.PrinterRuntime;


final class TickScheduler implements RuntimeComponent {
    private final ImmutableList<FeatureModuleBase> modules;
    private final PrinterRuntime runtime;
    private int packetTick;
    private String lastPauseReason;
    private boolean runtimeActive;
    private int executionScopeHash = Integer.MIN_VALUE;
    private int roundRobinOffset;

    TickScheduler(ImmutableList<FeatureModuleBase> modules, PrinterRuntime runtime) {
        this.modules = modules;
        this.runtime = runtime;
    }

    void tick() {
        Minecraft mc = this.runtime.client();
        this.runtime.inventoryAvailability().tick(mc.player);
        this.runtime.hudStats().tick();
        this.runtime.missingMaterials().tick(
                mc.player,
                mc.level != null ? mc.level.getGameTime() : 0L
        );
        if (!Configs.Core.WORK_SWITCH.getBooleanValue()) {
            if (this.runtimeActive) {
                this.runtime.reset("work_switch_disabled");
            }
            this.runtimeActive = false;
            this.runtime.hudStats().resetAll();
            this.lastPauseReason = null;
            return;
        }
        int currentScopeHash = this.currentExecutionScopeHash();
        if (!this.runtimeActive) {
            this.runtimeActive = true;
            this.executionScopeHash = currentScopeHash;
        } else if (this.executionScopeHash != currentScopeHash) {
            this.runtime.reset("execution_scope_changed");
            this.runtimeActive = true;
            this.executionScopeHash = currentScopeHash;
            return;
        }
        boolean lagPaused = this.pauseForLagCheck();
        boolean inventoryBusy = !lagPaused && this.pauseForInventoryState("shared_precheck");
        if (!lagPaused && !inventoryBusy) {
            this.advancePendingLookQueue(mc);
        }
        TickContext context = TickContext.capture();
        if (!inventoryBusy && !lagPaused) {
            this.resume();
        }
        for (FeatureModuleBase handler : this.modules) {
            if (handler instanceof GuiHandler) {
                handler.tick(context);
            }
        }
        if (lagPaused || inventoryBusy) {
            return;
        }
        int actionableCount = Math.max(0, this.modules.size() - 1);
        if (actionableCount == 0) {
            return;
        }
        int startIndex = this.roundRobinOffset % actionableCount;
        this.roundRobinOffset = (this.roundRobinOffset + 1) % actionableCount;
        for (int offset = 0; offset < actionableCount; offset++) {
            FeatureModuleBase handler = this.modules.get(1 + (startIndex + offset) % actionableCount);
            if (this.pauseForHandlerPrecheck(handler)) {
                return;
            }
            handler.tick(context);
        }
    }

    int getPacketTick() {
        return this.packetTick;
    }

    void setPacketTick(int packetTick) {
        this.packetTick = packetTick;
    }

    void recordInboundPacket() {
        this.packetTick = 0;
    }

    void resetRuntime() {
        this.packetTick = 0;
        this.lastPauseReason = null;
        this.runtimeActive = false;
        this.executionScopeHash = Integer.MIN_VALUE;
        this.roundRobinOffset = 0;
    }

    @Override
    public void onEpochChanged(RuntimeEvent.EpochChanged event) {
        this.resetRuntime();
    }

    String getLastPauseReason() {
        return this.lastPauseReason;
    }

    private void pause(String reason) {
        if (!reason.equals(this.lastPauseReason)) {
            MineDebugLog.write("scheduler pause reason=" + reason + " packetTick=" + this.packetTick);
            this.lastPauseReason = reason;
        }
    }

    private void resume() {
        if (this.lastPauseReason != null) {
            MineDebugLog.write("scheduler resume after=" + this.lastPauseReason + " packetTick=" + this.packetTick);
            this.lastPauseReason = null;
        }
    }

    private boolean pauseForInventoryState(String reasonPrefix) {
        boolean inventoryLease = this.runtime.actionBroker().isResourceHeld(ResourceLease.INVENTORY);
        boolean containerLease = this.runtime.actionBroker().isResourceHeld(ResourceLease.CONTAINER);
        boolean inventorySwitchPending = this.runtime.inventorySwitchGuard().isWaiting();
        boolean carrying = Minecraft.getInstance().player != null
                && !Minecraft.getInstance().player.containerMenu.getCarried().isEmpty();
        if (shouldPauseActions(inventoryLease || inventorySwitchPending || carrying, containerLease)) {
            this.pause(reasonPrefix + " inventoryLease=" + inventoryLease
                    + " containerLease=" + containerLease
                    + " inventorySwitchPending=" + inventorySwitchPending);
            return true;
        }
        return false;
    }

    static boolean shouldPauseActions(boolean inventoryBusy, boolean containerLease) {
        return inventoryBusy || containerLease;
    }

    private void advancePendingLookQueue(Minecraft mc) {
        if (!this.runtime.actionBroker().isWaitingForLook()) {
            return;
        }
        this.pause("send_queue_wait_modify_look");
        this.runtime.actionBroker().sendQueue(mc.player);
    }

    private boolean pauseForLagCheck() {
        if (!Configs.Core.LAG_CHECK.getBooleanValue()) {
            return false;
        }
        if (this.packetTick > Configs.Core.LAG_CHECK_MAX.getIntegerValue()) {
            this.pause("lag_check packetTick=" + this.packetTick + " max=" + Configs.Core.LAG_CHECK_MAX.getIntegerValue());
            return true;
        }
        this.packetTick++;
        return false;
    }

    private boolean pauseForHandlerPrecheck(FeatureModuleBase handler) {
        if (this.runtime.actionBroker().isResourceHeldByOther(ResourceLease.LOOK, handler.getId())) {
            this.pause("action_wait_modify_look handler=" + handler.getId());
        }
        return false;
    }

    private int currentExecutionScopeHash() {
        int result = Configs.Core.WORK_MODE.getOptionListValue().hashCode();
        result = 31 * result + Configs.Core.WORK_MODE_TYPE.getOptionListValue().hashCode();
        result = 31 * result + Boolean.hashCode(Configs.Core.PRINT.getBooleanValue());
        result = 31 * result + Boolean.hashCode(Configs.Core.MINE.getBooleanValue());
        result = 31 * result + Boolean.hashCode(Configs.Core.FILL.getBooleanValue());
        result = 31 * result + Boolean.hashCode(Configs.Core.FLUID.getBooleanValue());
        result = 31 * result + Boolean.hashCode(Configs.Core.COVER.getBooleanValue());
        result = 31 * result + Boolean.hashCode(Configs.Hotkeys.BEDROCK.getBooleanValue());
        return result;
    }
}
