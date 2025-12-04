package me.aleksilassila.litematica.printer;

import net.minecraft.network.chat.*;

import java.util.StringJoiner;

public class StringUtils {

    public static MutableComponent EMPTY = literal("");

    public static MutableComponent translatable(String key) {
        //#if MC > 11802
        return Component.translatable(key);
        //#else
        //$$ return new TranslatableComponent(key);
        //#endif
    }

    public static MutableComponent literal(String text) {
        //#if MC > 11802
        return Component.literal(text);
        //#else
        //$$ return new TextComponent(text);
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
