package me.aleksilassila.litematica.printer.handler.handlers;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.handler.ClientPlayerTickManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class MineDebugLog {
    private static final Path LOG_PATH = Minecraft.getInstance().gameDirectory.toPath().resolve("logs").resolve("mine-printer-debug.log");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static boolean initialized;

    private MineDebugLog() {
    }

    public static synchronized void write(String message) {
        if (!Configs.Core.DEBUG_OUTPUT.getBooleanValue()) {
            return;
        }

        try {
            initializeIfNeeded();
            String line = TIME_FORMAT.format(LocalDateTime.now())
                    + " tick=" + ClientPlayerTickManager.getCurrentHandlerTime()
                    + " " + message
                    + System.lineSeparator();
            Files.writeString(
                    LOG_PATH,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
        }
    }

    public static synchronized void reset() {
        initialized = false;
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
        return state.getBlock() + " " + state;
    }

    private static void initializeIfNeeded() throws IOException {
        if (initialized) {
            return;
        }

        Files.createDirectories(LOG_PATH.getParent());
        Files.writeString(
                LOG_PATH,
                "=== Mine debug session start " + TIME_FORMAT.format(LocalDateTime.now()) + " ===" + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
        initialized = true;
    }
}
