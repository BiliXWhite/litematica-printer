package me.aleksilassila.litematica.printer.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.StringJoiner;

public class StringUtils {

    public final static MutableComponent EMPTY = literal("");

    public static MutableComponent translatable(String key) {
        //#if MC > 11802
        return Component.translatable(key);
        //#else
        //$$ return new net.minecraft.network.chat.TranslatableComponent(key);
        //#endif
    }

    public static MutableComponent translatable(String key, Object... objects) {
        //#if MC > 11802
        return Component.translatable(key, objects);
        //#else
        //$$ return new net.minecraft.network.chat.TranslatableComponent(key, objects);
        //#endif
    }

    public static MutableComponent literal(String text) {
        //#if MC > 11802
        return Component.literal(text);
        //#else
        //$$ return new net.minecraft.network.chat.TextComponent(text);
        //#endif
    }

    public static MutableComponent nullToEmpty(@Nullable String string) {
        return string != null ? literal(string) : EMPTY;
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

    @Nullable
    public static String getTranslatedOrFallback(String key, @Nullable String fallback) {
        String translated = translatable(key).getString();
        if (!key.equals(translated)) {
            return translated;
        }
        return fallback;
    }

    @Nullable
    public static String getTranslatedOrFallback(String key, @Nullable String fallback, Object... objects) {
        String translated = translatable(key, objects).getString();
        if (!key.equals(translated)) {
            return translated;
        }
        return fallback;
    }

}
