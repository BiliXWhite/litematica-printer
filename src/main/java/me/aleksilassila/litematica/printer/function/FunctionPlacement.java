package me.aleksilassila.litematica.printer.function;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.printer.Printer;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class FunctionPlacement extends Function {
    protected final Map<BlockPos, Integer> placeCooldownList = new HashMap<>();

    public void cooldownTick() {
        if (!placeCooldownList.isEmpty()) {
            Iterator<Map.Entry<BlockPos, Integer>> placeIterator = placeCooldownList.entrySet().iterator();
            while (placeIterator.hasNext()) {
                Map.Entry<BlockPos, Integer> entry = placeIterator.next();
                int newValue = entry.getValue() - 1;
                if (newValue <= 0) {
                    placeIterator.remove();
                } else {
                    entry.setValue(newValue);
                }
            }
        }
    }

    @Override
    public boolean isConfigAllowExecute(Printer printer) {
        if (printer.placeSpeed != 0 && (printer.tickStartTime / 50) % printer.placeSpeed != 0) {
            return false;
        }
        return super.isConfigAllowExecute(printer);
    }

    public void setPlaceCooldown(BlockPos pos, int cooldown) {
        placeCooldownList.put(pos, cooldown);
    }

    public void setPlaceCooldown(BlockPos pos) {
        placeCooldownList.put(pos, Configs.Placement.PLACE_COOLDOWN.getIntegerValue());
    }

    public boolean isPlaceCooldown(BlockPos pos) {
        return placeCooldownList.containsKey(pos);
    }
}
