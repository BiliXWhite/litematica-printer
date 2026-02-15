package me.aleksilassila.litematica.printer.mixin.jackf.fix;


//#if MC > 11904
import me.aleksilassila.litematica.printer.printer.zxy.inventory.InventoryUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.jackf.chesttracker.api.ClientBlockSource;
import red.jackf.chesttracker.impl.providers.InteractionTrackerImpl;

//通过快捷盒子等方式非右键打开ui的情况会导致记录错误
@SuppressWarnings("SpellCheckingInspection")
@Mixin(value = InteractionTrackerImpl.class, remap = false)
public abstract class InteractionTrackerImplMixin {
    @Shadow(remap = false)
    public abstract void clear();

    @Inject(at = @At("TAIL"), method = "setLastBlockSource", remap = false)
    public void setLastBlockSource(ClientBlockSource source, CallbackInfo ci) {
        if (!InventoryUtils.isInventory(source.level(), source.pos())) {
            clear();
        }
    }
}
//#else
//$$ import me.aleksilassila.litematica.printer.mixin_extension.Pointless;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ @Mixin(value = Pointless.class)
//$$ public class InteractionTrackerImplMixin { }
//#endif