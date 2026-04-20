package me.aleksilassila.litematica.printer.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
    public static final Pattern SEM_VER_PATTERN = Pattern.compile("^v?(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:-(\\w+)(?:\\.(\\d+))?)?.*$");

    public static void checkForUpdates() {
        CompletableFuture.runAsync(() -> {
            boolean isLocalBeta = isBetaVersion(LOCAL_VERSION);
            String latestVersion = getLatestPrinterVersion(isLocalBeta);
            if (latestVersion == null) {
                return;
            }
            SemanticVersion localSemVer = SemanticVersion.parse(LOCAL_VERSION);
            SemanticVersion latestSemVer = SemanticVersion.parse(latestVersion);
            if (localSemVer == null || latestSemVer == null) {
                MessageUtils.addMessage("版本号解析失败，本地版本：" + LOCAL_VERSION + "，最新版本：" + latestVersion);
                return;
            }
            if (latestSemVer.isHigherThan(localSemVer)) {
                Minecraft.getInstance().execute(() -> MessageUtils.addMessage(MessageUtils.translatable("litematica_printer.update.available", LOCAL_VERSION, latestVersion)
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW))));
            }
        });
    }

    /**
     * 判断版本字符串是否为 beta 版本
     * @param version 版本字符串，如 "1.3-beta.1" 或 "1.2.3"
     * @return 如果是 beta 版本则返回 true
     */
    private static boolean isBetaVersion(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }
        return version.toLowerCase().contains("-beta");
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
     * 获取GitHub上最新的版本号
     * @param isBeta 是否获取 beta 版本
     * @return 最新版本号，如果获取失败则返回null
     */
    private static String getLatestPrinterVersion(boolean isBeta) {
        try {
            URI uri = URI.create("https://api.github.com/repos/BiliXWhite/litematica-printer/releases");
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8)) {
                    String response = scanner.useDelimiter("\\A").next();
                    com.google.gson.JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JsonObject release = jsonArray.get(i).getAsJsonObject();
                        String tagName = release.get("tag_name").getAsString();
                        boolean isPrerelease = release.get("prerelease").getAsBoolean();
                        boolean isBetaTag = tagName.toLowerCase().contains("-beta");
                        
                        if (isBeta) {
                            if (isBetaTag) {
                                return tagName;
                            }
                        } else {
                            if (!isPrerelease && !isBetaTag) {
                                return tagName;
                            }
                        }
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
        private final String prereleaseType;
        private final int prereleaseNumber;

        public SemanticVersion(int major, int minor, int patch) {
            this(major, minor, patch, null, 0);
        }

        public SemanticVersion(int major, int minor, int patch, String prereleaseType, int prereleaseNumber) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.prereleaseType = prereleaseType;
            this.prereleaseNumber = prereleaseNumber;
        }

        /**
         * 解析版本字符串为语义化版本对象
         * @param version 版本字符串，如 "1.2.3" 或 "v1.2" 或 "1.3-beta.1"
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
                    String prereleaseType = matcher.group(4);
                    int prereleaseNumber = matcher.group(5) != null ? Integer.parseInt(matcher.group(5)) : 0;
                    return new SemanticVersion(major, minor, patch, prereleaseType, prereleaseNumber);
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
            // 比较主版本号
            int majorCompare = Integer.compare(this.major, other.major);
            if (majorCompare != 0) {
                return majorCompare;
            }
            // 比较次版本号
            int minorCompare = Integer.compare(this.minor, other.minor);
            if (minorCompare != 0) {
                return minorCompare;
            }
            // 比较补丁版本号
            int patchCompare = Integer.compare(this.patch, other.patch);
            if (patchCompare != 0) {
                return patchCompare;
            }
            // 比较预发布类型
            if (this.prereleaseType == null && other.prereleaseType == null) {
                return 0;
            }
            if (this.prereleaseType == null) {
                return 1;
            }
            if (other.prereleaseType == null) {
                return -1;
            }
            // 都是预发布版本，比较类型
            int typeCompare = this.prereleaseType.compareToIgnoreCase(other.prereleaseType);
            if (typeCompare != 0) {
                return typeCompare;
            }
            // 相同类型，比较序号
            return Integer.compare(this.prereleaseNumber, other.prereleaseNumber);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(major).append(".").append(minor).append(".").append(patch);
            if (prereleaseType != null) {
                sb.append("-").append(prereleaseType);
                if (prereleaseNumber > 0) {
                    sb.append(".").append(prereleaseNumber);
                }
            }
            return sb.toString();
        }
    }
}