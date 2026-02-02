package me.aleksilassila.litematica.printer;

import lombok.Getter;
import me.aleksilassila.litematica.printer.utils.StringUtils;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

/*** 本地化翻译类，管理所有打印机模组的翻译键 ***/
@Getter
public class I18n {
    public static final I18n MESSAGE_TOGGLED = of("message.toggled");
    public static final I18n MESSAGE_VALUE_OFF = of("message.value.off");
    public static final I18n MESSAGE_VALUE_ON = of("message.value.on");

    public static final I18n AUTO_DISABLE_NOTICE = of("auto_disable_notice");
    public static final I18n FREE_NOTICE = of("free_notice");

    public static final I18n UPDATE_AVAILABLE = of("update.available");
    public static final I18n UPDATE_DOWNLOAD = of("update.download");
    public static final I18n UPDATE_FAILED = of("update.failed");
    public static final I18n UPDATE_PASSWORD = of("update.password");
    public static final I18n UPDATE_RECOMMENDATION = of("update.recommendation");
    public static final I18n UPDATE_REPOSITORY = of("update.repository");

    private static final String PREFIX_CONFIG = "config";
    private static final String PREFIX_COMMENT = "comment";

    // 原有成员变量保留
    private final @Nullable String prefix;
    private final String id;
    private final String configKey;
    private final String configCommentKey;

    private I18n(@Nullable String prefix, String id) {
        this.prefix = prefix;
        this.id = id;
        String configKey;
        if (prefix != null) {
            configKey = prefix + "." + PREFIX_CONFIG;
        } else {
            configKey = PREFIX_CONFIG;
        }
        this.configKey = configKey + "." + id;
        this.configCommentKey = configKey + "." + id + "." + PREFIX_COMMENT;
    }

    public static I18n of(@Nullable String prefix, String key) {
        return new I18n(prefix, key);
    }

    public static I18n of(String key) {
        return new I18n(Reference.MOD_ID, key);
    }

    public String getWithPrefixKey() {
        if (prefix != null) {
            return prefix + "." + id;
        }
        return id;
    }

    public MutableComponent getComponent() {
        return StringUtils.translatable(getWithPrefixKey());
    }

    public MutableComponent getComponent(Object... objects) {
        return StringUtils.translatable(getWithPrefixKey(), objects);
    }

    public MutableComponent getConfigComponent() {
        return StringUtils.translatable(getConfigKey());
    }

    public MutableComponent getConfigComponent(Object... objects) {
        return StringUtils.translatable(getConfigKey(), objects);
    }

    public MutableComponent getConfigCommentComponent() {
        return StringUtils.translatable(getConfigCommentKey());
    }

    public MutableComponent getConfigCommentComponent(Object... objects) {
        return StringUtils.translatable(getConfigCommentKey(), objects);
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