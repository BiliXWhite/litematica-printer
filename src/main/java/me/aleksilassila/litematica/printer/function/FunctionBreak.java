package me.aleksilassila.litematica.printer.function;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.utils.InteractionUtils;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class FunctionBreak extends Function {
    protected final Map<BlockPos, Integer> breakCooldownList = new HashMap<>();

    public void cooldownTick() {
        if (!breakCooldownList.isEmpty()) {
            Iterator<Map.Entry<BlockPos, Integer>> breakIterator = breakCooldownList.entrySet().iterator();
            while (breakIterator.hasNext()) {
                Map.Entry<BlockPos, Integer> entry = breakIterator.next();
                int newValue = entry.getValue() - 1;
                if (newValue <= 0) {
                    breakIterator.remove();
                } else {
                    entry.setValue(newValue);
                }
            }
        }
    }

    @Override
    public boolean isConfigAllowExecute(Printer printer) {
        if (InteractionUtils.INSTANCE.isDestroying()){
            return false;
        }
        int breakSpeed = Configs.Break.BREAK_SPEED.getIntegerValue();
        if (breakSpeed != 0 && (printer.tickStartTime / 50) % breakSpeed != 0) {
            return false;
        }
        return super.isConfigAllowExecute(printer);
    }


    public void setBreakCooldown(BlockPos pos, int cooldown) {
        breakCooldownList.put(pos, cooldown);
    }

    public void setBreakCooldown(BlockPos pos) {
        breakCooldownList.put(pos, Configs.Break.BREAK_COOLDOWN.getIntegerValue());
    }

    public boolean isBreakCooldown(BlockPos pos) {
        return breakCooldownList.containsKey(pos);
    }
}
