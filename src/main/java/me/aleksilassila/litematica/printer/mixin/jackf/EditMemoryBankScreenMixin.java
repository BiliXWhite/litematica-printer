package me.aleksilassila.litematica.printer.mixin.jackf;
//#if MC > 12001

import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.jackf.chesttracker.impl.gui.screen.EditMemoryBankScreen;
import red.jackf.chesttracker.impl.gui.screen.MemoryBankView;
import red.jackf.chesttracker.impl.memory.metadata.Metadata;

import static me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils.PRINTER_MEMORY;

//同步打印机库存搜索距离等设置
@Mixin(EditMemoryBankScreen.class)
public class EditMemoryBankScreenMixin {
    @Shadow(remap = false) @Final private MemoryBankView memoryBank;
    @Inject(at = @At("TAIL"), method = "save",remap = false)
    private void save(ButtonWidget button, CallbackInfo ci) {
        if (memoryBank.id().contains("-printer")) {
            Metadata metadata = memoryBank.metadata();
            PRINTER_MEMORY.setMetadata(metadata);
        }
    }
}
//#endif