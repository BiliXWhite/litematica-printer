package me.aleksilassila.litematica.printer.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.aleksilassila.litematica.printer.Debug;
import me.aleksilassila.litematica.printer.I18n;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class UpdateCheckerUtils {
    // 本地版本（从fabric.mod.json读取）
    public static final String LOCAL_VERSION = getVersionFromModJson();

    // 语义化版本号正则：匹配v1.2.3、1.2、5等格式，提取数字部分
    public static final Pattern SEM_VER_PATTERN = Pattern.compile("^v?(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?.*$");

    public static void checkForUpdates() {
        CompletableFuture.runAsync(() -> {
            // 获取GitHub最新正式版版本号（已过滤预发布版）
            String latestOfficialVersion = getLatestOfficialPrinterVersion();
            if (latestOfficialVersion == null) {
                return;
            }
            // 解析本地版本和最新正式版为语义化版本对象
            SemanticVersion localSemVer = SemanticVersion.parse(LOCAL_VERSION);
            SemanticVersion latestSemVer = SemanticVersion.parse(latestOfficialVersion);
            // 版本解析失败则跳过
            if (localSemVer == null || latestSemVer == null) {
                Debug.alwaysWrite("版本号解析失败，本地版本：" + LOCAL_VERSION + "，最新版本：" + latestOfficialVersion);
                return;
            }
            // 仅当最新正式版 > 本地版本时，触发更新提示
            if (latestSemVer.isHigherThan(localSemVer)) {
                Minecraft.getInstance().execute(() -> {
                    MessageUtils.addMessage(I18n.UPDATE_AVAILABLE.getName(LOCAL_VERSION, latestOfficialVersion)
                            .withStyle(ChatFormatting.YELLOW));
                    MessageUtils.addMessage(I18n.UPDATE_RECOMMENDATION.getName()
                            .withStyle(ChatFormatting.RED));
                    MessageUtils.addMessage(I18n.UPDATE_REPOSITORY.getName()
                            .withStyle(ChatFormatting.WHITE));
                    MessageUtils.addMessage(StringUtils.literal("https://github.com/BiliXWhite/litematica-printer")
                            .setStyle(Style.EMPTY
                                    //#if MC >= 12105
                                    .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://github.com/BiliXWhite/litematica-printer")))
                                    //#else
                                    //$$ .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/BiliXWhite/litematica-printer"))
                                    //#endif
                                    .withUnderlined(true)
                                    .withColor(ChatFormatting.BLUE)));
                    MessageUtils.addMessage(I18n.UPDATE_DOWNLOAD.getName()
                            .setStyle(Style.EMPTY
                                    //#if MC >= 12105
                                    .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://xeno.lanzoue.com/b00l1v20vi")))
                                    //#else
                                    //$$ .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://xeno.lanzoue.com/b00l1v20vi"))
                                    //#endif
                                    .withBold(true)
                                    .withColor(ChatFormatting.GREEN)));
                    MessageUtils.addMessage(I18n.UPDATE_PASSWORD.getName("cgxw")
                            .withStyle(ChatFormatting.WHITE));
                    MessageUtils.addMessage(
                            StringUtils.literal("------------------------").withStyle(ChatFormatting.GRAY));
                });
            }
        });
    }

    /**
     * 获取GitHub最新**正式版**版本号（过滤预发布版/dev/beta/alpha）
     *
     * @return 最新正式版tag_name，无则返回null
     */
    public static String getLatestOfficialPrinterVersion() {
        try {
            URI uri = URI.create("https://api.github.com/repos/BiliXWhite/litematica-printer/releases/latest");
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(20000);
            // 模拟浏览器请求，避免GitHub API拒绝
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

            try (InputStream inputStream = conn.getInputStream();
                 Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
                scanner.useDelimiter("\\A");
                if (scanner.hasNext()) {
                    String response = scanner.next();
                    JsonObject release = JsonParser.parseString(response).getAsJsonObject();
                    return release.get("tag_name").getAsString();
                }
            }
        } catch (Exception exception) {
            Debug.alwaysWrite("无法检查更新: " + exception.getMessage());
            Minecraft.getInstance().execute(() -> MessageUtils.addMessage(I18n.UPDATE_FAILED.getName()));
            exception.printStackTrace();
        }
        return null;
    }

    /**
     * 从fabric.mod.json读取本地Mod版本号（保留原逻辑）
     */
    private static String getVersionFromModJson() {
        ModContainer container = FabricLoader.getInstance()
                .getModContainer("litematica-printer")
                .orElseThrow(() -> new IllegalStateException("未找到对应 mod: litematica-printer"));
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

    /**
     * 判断是否为快照版本（dev/beta/alpha/snapshot）
     */
    @SuppressWarnings("SameParameterValue")
    private static boolean isSnapshotVersion(String version) {
        if (version == null || "unknown".equals(version)) {
            return true;
        }
        String lowerVersion = version.toLowerCase();
        return lowerVersion.contains("dev") || lowerVersion.contains("beta")
                || lowerVersion.contains("alpha") || lowerVersion.contains("snapshot");
    }

    /**
     * 语义化版本号工具类：解析、比较（支持x.y.z / x.y / x格式，忽略v前缀和后缀）
     *
     * @param major 主版本
     * @param minor 次版本
     * @param patch 补丁版本
     */
    private record SemanticVersion(int major, int minor, int patch) {
        /**
         * 解析版本号字符串为SemanticVersion对象
         * 支持：v1.2.3、1.2、5、1.3.0-dev、v2.0-beta等格式
         */
        public static SemanticVersion parse(String versionStr) {
            if (versionStr == null || versionStr.isBlank()) {
                return null;
            }
            var matcher = SEM_VER_PATTERN.matcher(versionStr);
            if (!matcher.matches()) {
                return null;
            }
            // 解析主、次、补丁版本，未指定则为0
            int major = Integer.parseInt(matcher.group(1));
            int minor = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
            int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            return new SemanticVersion(major, minor, patch);
        }

        /**
         * 判断当前版本是否高于目标版本
         * 比较规则：主版本>次版本>补丁版本，依次比较
         */
        public boolean isHigherThan(SemanticVersion target) {
            if (target == null) {
                return false;
            }
            if (this.major > target.major) {
                return true;
            } else if (this.major == target.major) {
                if (this.minor > target.minor) {
                    return true;
                } else if (this.minor == target.minor) {
                    return this.patch > target.patch;
                }
            }
            return false;
        }
    }
}