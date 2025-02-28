package me.aleksilassila.litematica.printer.mixin.jackf.fix;

import me.aleksilassila.litematica.printer.config.Pointless;
import org.spongepowered.asm.mixin.Mixin;


//通过快捷盒子等方式非右键打开ui的情况会导致记录错误
@Mixin(Pointless.class)
public class InteractionTrackerImplMixin {
}
