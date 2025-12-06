package me.aleksilassila.litematica.printer;

import net.minecraft.network.chat.Component;

import java.util.StringJoiner;

public class StringUtils {

    public final static Component EMPTY = literal("");

    public static Component translatable(String key) {
        //#if MC > 11802
        return Component.translatable(key);
        //#else
        //$$ return new net.minecraft.network.chat.TranslatableComponent(key);
        //#endif
    }

    public static Component translatable(String key, Object... objects) {
        //#if MC > 11802
        return Component.translatable(key, objects);
        //#else
        //$$ return new net.minecraft.network.chat.TranslatableComponent(key, objects);
        //#endif
    }

    public static Component literal(String text) {
        //#if MC > 11802
        return Component.literal(text);
        //#else
        //$$ return new net.minecraft.network.chat.TextComponent(text);
        //#endif
    }

    public static String mergeComments(String delimiter, Component... customComments) {
        StringJoiner joiner = new StringJoiner(delimiter);
        for (Component comment : customComments) {
            if (comment != null) {
                joiner.add(comment.getString());
            }
        }
        return joiner.toString();
    }
}
