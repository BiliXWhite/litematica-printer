package me.aleksilassila.litematica.printer.runtime;

import me.aleksilassila.litematica.printer.core.runtime.RuntimeComponent;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeEvent;
import me.aleksilassila.litematica.printer.utils.InventorySwitchGuard;

/** Epoch boundary for the printer's local inventory-switch confirmation window. */
final class InventorySwitchRuntime implements RuntimeComponent {
    private final InventorySwitchGuard guard;

    InventorySwitchRuntime(InventorySwitchGuard guard) {
        this.guard = guard;
    }

    @Override
    public void onEpochChanged(RuntimeEvent.EpochChanged event) {
        this.guard.reset();
    }
}
