package me.aleksilassila.litematica.printer.mixin.jackf;
//#if MC >= 12001
//$$
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//$$ import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
//$$ import net.minecraft.client.gui.widget.ButtonWidget;
//$$ import org.spongepowered.asm.mixin.Final;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$ import red.jackf.chesttracker.impl.gui.screen.EditMemoryBankScreen;
//$$ import red.jackf.chesttracker.impl.gui.screen.MemoryBankView;
//$$ import red.jackf.chesttracker.impl.gui.widget.HoldToConfirmButton;
//$$ import red.jackf.chesttracker.impl.memory.metadata.Metadata;
//$$
//$$ import static me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils.PRINTER_MEMORY;
//$$
//$$ //同步打印机库存搜索距离等设置
//$$ @Mixin(EditMemoryBankScreen.class)
//$$ public class EditMemoryBankScreenMixin {
//$$     @Shadow(remap = false) @Final private MemoryBankView memoryBank;
//$$     @Inject(at = @At("TAIL"), method = "save",remap = false)
//$$     private void save(ButtonWidget button, CallbackInfo ci) {
//$$         if (memoryBank.id().contains("-printer")) {
//$$             Metadata metadata = memoryBank.metadata();
//$$             PRINTER_MEMORY.setMetadata(metadata);
//$$         }
//$$     }
//$$
//$$     //使用箱子追踪清除库存时需要处理已被缓存的打印机库存，否则会出现错误。 其次需要再次创建一个空的打印机库存，并且继承相关设置
//$$     @WrapOperation(at = @At(value = "INVOKE", target = "Lred/jackf/chesttracker/impl/storage/Storage;delete(Ljava/lang/String;)V"), method = "delete",remap = false)
//$$     private void delete(String id, Operation<Void> original) {
//$$         String[] split = id.split("-");
//$$         if("printer".equals(split[split.length-1])) {
//$$             MemoryUtils.deletePrinterMemory();
//$$         } else {
//$$             original.call(id);
//$$         }
//$$     }
//$$ }
//#endif