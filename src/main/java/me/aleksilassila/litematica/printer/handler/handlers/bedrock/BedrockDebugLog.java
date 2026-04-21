package me.aleksilassila.litematica.printer.handler.handlers.bedrock;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.piston.PistonBaseBlock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class BedrockDebugLog {
    private static final Path LOG_PATH = Minecraft.getInstance().gameDirectory.toPath().resolve("logs").resolve("bedrock-printer-debug.log");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private BedrockDebugLog() {
    }

    public static synchronized void write(String message) {
        if (!me.aleksilassila.litematica.printer.config.Configs.Core.DEBUG_OUTPUT.getBooleanValue()) {
            return;
        }

        try {
            Files.createDirectories(LOG_PATH.getParent());
            Files.writeString(
                    LOG_PATH,
                    "[" + LocalDateTime.now().format(TIME_FORMAT) + "] " + message + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
        }
    }

    public static Path getLogPath() {
        return LOG_PATH;
    }

    public static String pos(BlockPos pos) {
        if (pos == null) {
            return "null";
        }
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    public static String describeState(BlockState state) {
        return state.getBlock().toString() + " " + state.toString();
    }

    public static String describePistonState(BlockState state) {
        StringBuilder builder = new StringBuilder(describeState(state));
        if (state.hasProperty(PistonBaseBlock.FACING)) {
            builder.append(" facing=").append(state.getValue(PistonBaseBlock.FACING));
        }
        if (state.hasProperty(PistonBaseBlock.EXTENDED)) {
            builder.append(" extended=").append(state.getValue(PistonBaseBlock.EXTENDED));
        }
        return builder.toString();
    }
}
