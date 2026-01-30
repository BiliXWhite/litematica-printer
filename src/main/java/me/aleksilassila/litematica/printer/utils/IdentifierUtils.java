package me.aleksilassila.litematica.printer.utils;

import net.minecraft.resources.Identifier;

public class IdentifierUtils {

    public static Identifier of(String string) {
        //#if MC > 12006
        return Identifier.parse(string);
        //#else
        //$$ return new ResourceLocation(string);
        //#endif
    }

    public static Identifier of(String namespace, String path) {
        //#if MC > 12006
        return Identifier.fromNamespaceAndPath(namespace, path);
        //#else
        //$$ return new ResourceLocation(namespace, path);
        //#endif
    }
}

