package me.aleksilassila.litematica.printer.mixin.jackf;

import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import red.jackf.chesttracker.gui.screen.ChestTrackerScreen;
import red.jackf.whereisit.api.criteria.Criterion;
import red.jackf.whereisit.api.criteria.CriterionType;
import red.jackf.whereisit.api.criteria.builtin.NameCriterion;

import java.util.Locale;

@Mixin(NameCriterion.class)
public class NameCriterionMixin{
    @Shadow(remap = false)
    private String name;
    ChestTrackerScreen s;
    @Inject(at = @At(value = "INVOKE",target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"),method = "test",remap = false,cancellable = true)
    private void test(ItemStack stack, CallbackInfoReturnable<Boolean> cir){
        System.out.println(stack.getName().getString());
        if(!stack.getName().getString().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)))
            ;
    }

}
