package me.aleksilassila.litematica.printer.mixin.masa.litematicaSetConfig;

import fi.dy.masa.litematica.config.Configs;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Configs.class, remap = false)
public class ConfigsMixin {
//	@Redirect(method = "loadFromFile", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Configs$Colors;OPTIONS:Lcom/google/common/collect/ImmutableList;"))
//    private static ImmutableList<IConfigBase> colorsOptions() {
//        return getColorsList();
//    }
//	@Redirect(method = "loadFromFile", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Configs$Generic;OPTIONS:Lcom/google/common/collect/ImmutableList;"))
//    private static ImmutableList<IConfigBase> moreOptions() {
//
//        return LitematicaMixinMod.getConfigList();
//    }
//
//    @Redirect(method = "saveToFile", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Configs$Colors;OPTIONS:Lcom/google/common/collect/ImmutableList;"))
//
//    private static ImmutableList<IConfigBase> colorssOptions() {
//        return getColorsList();
//    }
//    @Redirect(method = "saveToFile", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Configs$Generic;OPTIONS:Lcom/google/common/collect/ImmutableList;"))
//    private static ImmutableList<IConfigBase> moreeOptions() {
//        return LitematicaMixinMod.getConfigList();
//    }
//
//    @Redirect(method = "loadFromFile", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Hotkeys;HOTKEY_LIST:Ljava/util/List;"))
//    private static List<IConfigBase> moreHotkeys() {
//        return LitematicaMixinMod.getHotkeyList();
//    }
//
//    @Redirect(method = "saveToFile", at = @At(value = "FIELD", target = "Lfi/dy/masa/litematica/config/Hotkeys;HOTKEY_LIST:Ljava/util/List;"))
//    private static List<IConfigBase> moreeHotkeys() {
//        return LitematicaMixinMod.getHotkeyList();
//    }
}
