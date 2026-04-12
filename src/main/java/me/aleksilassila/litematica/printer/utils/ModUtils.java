package me.aleksilassila.litematica.printer.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.aleksilassila.litematica.printer.Debug;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class ModUtils {
    // 阻止 UI 显示 如果此时已经在 UI 中 请设置为 2 因为关闭 UI 也会调用一次
    public static int closeScreen = 0;

    public static boolean isLoadMod(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static boolean isChestTrackerLoaded(){
        return isLoadMod("chesttracker");
    }

    public static boolean isQuickShulkerLoaded(){
        return isLoadMod("quickshulker");
    }

    public static boolean isBedrockMinerLoaded() {
        //#if MC >= 11900
        return isLoadMod("bedrockminer");
        //#else
        //$$ return false;
        //#endif
    }

    public static boolean isBlockMinerLoaded() {
        //#if MC >= 11605
        return isLoadMod("blockminer");
        //#else
        //$$ return false;
        //#endif
    }

    public static boolean isTweakerooLoaded() {
        return isLoadMod("tweakeroo");
    }

    private static @Nullable Object tweakToolSwitchEnum;
    private static @Nullable Method trySwitchToEffectiveToolMethod;
    private static @Nullable Method getBooleanValueMethod;

    static {
        if (FabricLoader.getInstance().isModLoaded("tweakeroo")) {
            try {
                Class<?> featureToggleClass = Class.forName("fi.dy.masa.tweakeroo.config.FeatureToggle");
                tweakToolSwitchEnum = featureToggleClass.getField("TWEAK_TOOL_SWITCH").get(null);

                Class<?> iConfigBooleanClass = Class.forName("fi.dy.masa.malilib.config.IConfigBoolean");
                getBooleanValueMethod = iConfigBooleanClass.getDeclaredMethod("getBooleanValue");

                Class<?> inventoryUtilsClass = Class.forName("fi.dy.masa.tweakeroo.util.InventoryUtils");
                trySwitchToEffectiveToolMethod = inventoryUtilsClass.getDeclaredMethod("trySwitchToEffectiveTool", BlockPos.class);

            } catch (Exception e) {
                tweakToolSwitchEnum = null;
                trySwitchToEffectiveToolMethod = null;
                getBooleanValueMethod = null;
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查 Tweakeroo 的 TWEAK_TOOL_SWITCH 选项是否启用。
     * @return 如果 Tweakeroo 存在且选项启用，则返回 true，否则返回 false。
     */
    public static boolean isToolSwitchEnabled() {
        if (getBooleanValueMethod == null || tweakToolSwitchEnum == null) {
            return false;
        }
        try {
            return (boolean) getBooleanValueMethod.invoke(tweakToolSwitchEnum);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 调用 Tweakeroo 的 InventoryUtils.trySwitchToEffectiveTool(BlockPos pos) 静态方法。
     * 只有在 Tweakeroo 存在且方法被成功加载时才执行。
     * @param pos 要挖掘的方块位置
     */
    public static void trySwitchToEffectiveTool(BlockPos pos) {
        if (trySwitchToEffectiveToolMethod == null) {
            return;
        }
        try {
            trySwitchToEffectiveToolMethod.invoke(null, pos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
                    MessageUtils.addMessage(MessageUtils.translatable("litematica_printer.update.available", LOCAL_VERSION, latestOfficialVersion)
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
                });
            }
        });
    }

    /**
     * 从 fabric.mod.json 读取版本号
     * @return 版本号字符串，如果读取失败则返回 "unknown"
     */
    private static String getVersionFromModJson() {
        try {
            Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer("litematica-printer");
            if (modContainer.isPresent()) {
                Optional<Path> modJsonPath = modContainer.get().findPath("fabric.mod.json");
                if (modJsonPath.isPresent() && Files.exists(modJsonPath.get())) {
                    try (InputStream inputStream = Files.newInputStream(modJsonPath.get());
                         InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                        JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
                        return jsonObject.get("version").getAsString();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "unknown";
    }

    /**
     * 获取GitHub上最新的正式版版本号（过滤预发布版）
     * @return 最新正式版版本号，如果获取失败则返回null
     */
    private static String getLatestOfficialPrinterVersion() {
        try {
            URI uri = URI.create("https://api.github.com/repos/aleksilassila/litematica-printer/releases/latest");
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8)) {
                    String response = scanner.useDelimiter("\\A").next();
                    JsonObject jsonObject = JsonParser.parseString(response).getAsJsonObject();
                    boolean isPrerelease = jsonObject.get("prerelease").getAsBoolean();
                    if (!isPrerelease) {
                        return jsonObject.get("tag_name").getAsString();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 语义化版本号类，用于版本比较
     */
    public static class SemanticVersion implements Comparable<SemanticVersion> {
        private final int major;
        private final int minor;
        private final int patch;

        public SemanticVersion(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        /**
         * 解析版本字符串为语义化版本对象
         * @param version 版本字符串，如 "1.2.3" 或 "v1.2"
         * @return 语义化版本对象，如果解析失败则返回null
         */
        public static SemanticVersion parse(String version) {
            if (version == null || version.isEmpty()) {
                return null;
            }
            java.util.regex.Matcher matcher = SEM_VER_PATTERN.matcher(version);
            if (matcher.matches()) {
                try {
                    int major = Integer.parseInt(matcher.group(1));
                    int minor = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
                    int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
                    return new SemanticVersion(major, minor, patch);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        }

        /**
         * 检查当前版本是否高于指定版本
         * @param other 另一个语义化版本对象
         * @return 如果当前版本更高，则返回true
         */
        public boolean isHigherThan(SemanticVersion other) {
            if (other == null) {
                return true;
            }
            return this.compareTo(other) > 0;
        }

        @Override
        public int compareTo(SemanticVersion other) {
            if (other == null) {
                return 1;
            }
            int majorCompare = Integer.compare(this.major, other.major);
            if (majorCompare != 0) {
                return majorCompare;
            }
            int minorCompare = Integer.compare(this.minor, other.minor);
            if (minorCompare != 0) {
                return minorCompare;
            }
            return Integer.compare(this.patch, other.patch);
        }

        @Override
        public String toString() {
            return major + "." + minor + "." + patch;
        }
    }
}