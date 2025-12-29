package me.aleksilassila.litematica.printer.utils;

import net.minecraft.resources.ResourceLocation;

public class ResourceLocationUtils {

    public static ResourceLocation of(String string) {
        //#if MC > 12006
        return ResourceLocation.parse(string);
        //#else
        //$$ return new ResourceLocation(string);
        //#endif
    }

    public static ResourceLocation of(String namespace, String path) {
        //#if MC > 12006
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
        //#else
        //$$ return new ResourceLocation(namespace, path);
        //#endif
    }
}

