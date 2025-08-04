package me.aleksilassila.litematica.printer.printer.bilixwhite.utils;

import me.aleksilassila.litematica.printer.LitematicaMixinMod;
//#if MC > 11802
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
//#else
//$$ import net.minecraft.text.TranslatableText;
//#endif

public class I18nUtils {
    public static final String PREFIX = LitematicaMixinMod.MOD_ID + ".config";
    public static String getName(String key) {
        //#if MC > 11802
        return Text.translatable(PREFIX + ".name." + key).getString();
        //#else
        //$$ return new TranslatableText(PREFIX + ".name." + key).getString();
        //#endif
    }
    public static String getComment(String key) {
        //#if MC > 11802
        return Text.translatable(PREFIX + ".comment." + key).getString();
        //#else
        //$$ return new TranslatableText(PREFIX + ".comment." + key).getString();
        //#endif
    }
}
