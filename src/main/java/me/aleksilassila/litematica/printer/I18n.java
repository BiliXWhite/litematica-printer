package me.aleksilassila.litematica.printer;

import me.aleksilassila.litematica.printer.utils.StringUtils;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

/**
 * 本地化翻译类，管理所有打印机模组的翻译键
 * 重构说明：从枚举改为普通类，支持new实例化，原有枚举值转为静态常量
 */
public class I18n {
    // @formatter:off


    public static final I18n MESSAGE_TOGGLED        = of("message.toggled");
    public static final I18n MESSAGE_VALUE_OFF      = of("message.value.off");
    public static final I18n MESSAGE_VALUE_ON       = of("message.value.on");

    public static final I18n AUTO_DISABLE_NOTICE    = of("auto_disable_notice");

    public static final I18n UPDATE_AVAILABLE       = config("update.available");
    public static final I18n UPDATE_DOWNLOAD        = config("update.download");
    public static final I18n UPDATE_FAILED          = config("update.failed");
    public static final I18n UPDATE_PASSWORD        = config("update.password");
    public static final I18n UPDATE_RECOMMENDATION  = config("update.recommendation");
    public static final I18n UPDATE_REPOSITORY      = config("update.repository");

    private static final String PREFIX_CONFIG = "config";
    private static final String PREFIX_NAME = "name";
    private static final String PREFIX_COMMENT = "comment";
    private static final String PREFIX_LIST = "list";

    // 原有成员变量保留
    private final @Nullable String prefix;
    private final String id;
    private final String configNameKey;
    private final String configCommentKey;
    private final String configListKey;

    private I18n(@Nullable String prefix, String id) {
        this.prefix = prefix;
        this.id = id;
        String configKey;
        if (prefix != null) {
            configKey = prefix + "." + PREFIX_CONFIG;
        } else {
            configKey = PREFIX_CONFIG;
        }
        this.configNameKey = configKey + "." + PREFIX_NAME + "." + id;
        this.configCommentKey = configKey + "." + PREFIX_COMMENT + "." + id;
        this.configListKey = configKey + "." + PREFIX_LIST + "." + id;
    }

    public static I18n of(@Nullable String prefix, String key) {
        return new I18n(prefix, key);
    }

    public static I18n of(String key) {
        return new I18n(Reference.MOD_ID, key);
    }

    public static I18n config(String key) {
        return new I18n(Reference.MOD_ID, key);
    }

    // 原始传入的键名
    public String getId() {
        return id;
    }

    public @Nullable String getPrefix() {
        return prefix;
    }

    public String getWithPrefixKey() {
        if (prefix != null) {
            return prefix + "." + id;
        }
        return id;
    }

    public String getConfigNameKey() {
        return configNameKey;
    }

    public String getConfigCommentKey() {
        return configCommentKey;
    }

    public String getConfigListKey() {
        return configListKey;
    }

    public MutableComponent getComponent() {
        return StringUtils.translatable(getWithPrefixKey());
    }

    public MutableComponent getComponent(Object... objects) {
        return StringUtils.translatable(getWithPrefixKey(), objects);
    }

    public MutableComponent getConfigNameComponent() {
        return StringUtils.translatable(getConfigNameKey());
    }

    public MutableComponent getConfigNameComponent(Object... objects) {
        return StringUtils.translatable(getConfigNameKey(), objects);
    }

    public MutableComponent getConfigCommentComponent() {
        return StringUtils.translatable(getConfigCommentKey());
    }

    public MutableComponent getConfigCommentComponent(Object... objects) {
        return StringUtils.translatable(getConfigCommentKey(), objects);
    }

    public MutableComponent getConfigListComponent() {
        return StringUtils.translatable(getConfigListKey());
    }

    public MutableComponent getConfigListComponent(Object... objects) {
        return StringUtils.translatable(getConfigListKey(), objects);
    }

    public String getSimpleKey() {
        if (id == null || id.isEmpty()) {
            return id == null ? "" : id;
        }
        int lastDotIndex = id.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return id;
        }
        if (lastDotIndex == id.length() - 1) {
            return "";
        }
        return id.substring(lastDotIndex + 1);
    }
}