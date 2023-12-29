package me.aleksilassila.litematica.printer.mixin.jackf;

import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.jackf.whereisit.api.criteria.Criterion;
import red.jackf.whereisit.client.api.events.SearchRequestPopulator;

import java.util.function.Consumer;

@Mixin(value = SearchRequestPopulator.class)
public interface SearchRequestPopulatorMixin {
    @Inject(at = @At("HEAD"), method = "addItemStack")
    private static void addItemStack(Consumer<Criterion> consumer, ItemStack stack, SearchRequestPopulator.Context context, CallbackInfo ci) {
        MemoryUtils.itemStack = stack;
    }
}
