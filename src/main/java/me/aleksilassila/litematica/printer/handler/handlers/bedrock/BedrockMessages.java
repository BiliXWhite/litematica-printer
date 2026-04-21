package me.aleksilassila.litematica.printer.handler.handlers.bedrock;

import me.aleksilassila.litematica.printer.utils.minecraft.MessageUtils;
import me.aleksilassila.litematica.printer.utils.minecraft.StringUtils;

public final class BedrockMessages {
    private BedrockMessages() {
    }

    public static void actionBar(String key) {
        MessageUtils.setOverlayMessage(StringUtils.translatable(key), false);
    }
}
