package me.aleksilassila.litematica.printer.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(HandledScreen.class)
public class MixinHandledScreen {
    @Shadow
    protected Slot focusedSlot;
//    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
//    private void QS$mousePressed(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
//        System.out.println(focusedSlot.id);
//    }
}
