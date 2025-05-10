package me.aleksilassila.litematica.printer.printer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;

public class UpdateChecker {
    public static final String version = getVersionFromModJson();

    public static String getPrinterVersion() {
        try {
            URL url = new URL("https://api.github.com/repos/BiliXWhite/litematica-printer/tags");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(20000);

            try (InputStream inputStream = conn.getInputStream();
                 Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
                scanner.useDelimiter("\\A");
                if (scanner.hasNext()) {
                    String response = scanner.next();
                    JsonArray tags = JsonParser.parseString(response).getAsJsonArray();
                    if (!tags.isEmpty()) {
                        return ((JsonObject) tags.get(0)).get("name").getAsString();
                    }
                }
            }
        } catch (Exception exception) {
            System.out.println("无法检查更新: " + exception.getMessage());
        }
        return "";
    }

    private static String getVersionFromModJson() {
        ModContainer container = FabricLoader.getInstance()
                .getModContainer("litematica-printer")
                .orElseThrow(() -> new IllegalStateException("未找到对应 mod"));
        Path modPath = container.getRootPath().resolve("fabric.mod.json");
        try (InputStream inputStream = modPath.toUri().toURL().openStream();
             InputStreamReader reader = new InputStreamReader(inputStream)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return json.get("version").getAsString().substring(0, json.get("version").getAsString().length() - 7);
        } catch (Exception e) {
            System.out.println("无法读取 mod 版本: ");
            e.printStackTrace();
            return "unknown";
        }
    }
}