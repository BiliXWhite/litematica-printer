package me.aleksilassila.litematica.printer.printer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

public class UpdateChecker {
    public static final String version = "1.0.9";

    public static String getPrinterVersion() {
        try {
            URL url = new URL("https://api.github.com/repos/BiliXWhite/litematica-printer/tags");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(20000);

            try (InputStream inputStream = conn.getInputStream();
                 Scanner scanner = new Scanner(inputStream, "UTF-8")) {
                scanner.useDelimiter("\\A");
                if (scanner.hasNext()) {
                    String response = scanner.next();
                    JsonArray tags = JsonParser.parseString(response).getAsJsonArray();
                    if (tags.size() > 0) {
                        return ((JsonObject) tags.get(0)).get("name").getAsString();
                    }
                }
            }
        } catch (Exception exception) {
            System.out.println("无法检查更新: " + exception.getMessage());
        }
        return "";
    }
}