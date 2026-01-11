package me.aleksilassila.litematica.printer.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.aleksilassila.litematica.printer.I18n;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Scanner;

public class UpdateCheckerUtils {
    public static final String version = getVersionFromModJson();

    public static String getPrinterVersion() {
        try {
            URI uri = URI.create("https://api.github.com/repos/BiliXWhite/litematica-printer/releases/latest");
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(20000);

            try (InputStream inputStream = conn.getInputStream();
                 Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
                scanner.useDelimiter("\\A");
                if (scanner.hasNext()) {
                    String response = scanner.next();
                    // 解析为 JsonObject，而不是 JsonArray
                    JsonObject release = JsonParser.parseString(response).getAsJsonObject();
                    // 直接获取 tag_name 字段
                    return release.get("tag_name").getAsString();
                }
            }
        } catch (Exception exception) {
            System.out.println("无法检查更新: " + exception.getMessage());
            MessageUtils.addMessage(I18n.UPDATE_FAILED.getComponent());
        }
        return null;
    }

    private static String getVersionFromModJson() {
        ModContainer container = FabricLoader.getInstance()
                .getModContainer("litematica-printer")
                .orElseThrow(() -> new IllegalStateException("未找到对应 mod"));
        Optional<Path> modPathOptional = container.findPath("fabric.mod.json");
        if (modPathOptional.isEmpty()) {
            System.out.println("无法找到 fabric.mod.json 文件");
            return "unknown";
        }
        Path modPath = modPathOptional.get();
        try (InputStream inputStream = Files.newInputStream(modPath);
             InputStreamReader reader = new InputStreamReader(inputStream)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return json.get("version").getAsString();
        } catch (Exception e) {
            System.out.println("无法读取 mod 版本: ");
            e.printStackTrace();
            return "unknown";
        }
    }
}