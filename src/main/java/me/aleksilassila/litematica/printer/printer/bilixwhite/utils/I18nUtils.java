package me.aleksilassila.litematica.printer.printer.bilixwhite.utils;

import me.aleksilassila.litematica.printer.LitematicaMixinMod;
//#if MC > 11802
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
//#else
//$$ import net.minecraft.text.TranslatableText;
//#endif

public class I18nUtils {
    public static String getName(String key) {
        //#if MC > 11802
        return Text.translatable(LitematicaMixinMod.MOD_ID + ".config.name." + key).getString();
        //#else
        //$$ return new TranslatableText(LitematicaMixinMod.MOD_ID + ".config.name." + key).getString();
        //#endif
    }
    public static String getComment(String key) {
        //#if MC > 11802
        return Text.translatable(LitematicaMixinMod.MOD_ID + ".config.comment." + key).getString();
        //#else
        //$$ return new TranslatableText(LitematicaMixinMod.MOD_ID + ".config.comment." + key).getString();
        //#endif
    }

    public static String get(String key) {
        //#if MC > 11802
        return Text.translatable(LitematicaMixinMod.MOD_ID + "." + key).getString();
        //#else
        //$$ return new TranslatableText(LitematicaMixinMod.MOD_ID + "." + key).getString();
        //#endif
    }
}
