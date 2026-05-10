package me.aleksilassila.litematica.printer;

import lombok.Getter;
import me.aleksilassila.litematica.printer.utils.MessageUtils;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

@Getter
public class I18n {
    public static final I18n MESSAGE_TOGGLED = of("message.toggled");
    public static final I18n MESSAGE_VALUE_OFF = of("message.value.off");
    public static final I18n MESSAGE_VALUE_ON = of("message.value.on");

    public static final I18n AUTO_DISABLE_NOTICE = of("auto_disable_notice");
    public static final I18n FREE_NOTICE = of("free_notice");

    public static final I18n BEDROCK_CREATIVE_MODE = of("bedrock.creative_mode");
    public static final I18n BEDROCK_MOD_MISSING = of("bedrock.mod_missing");

    public static final I18n ICE_CREATIVE_MODE = of("ice.creative_mode");

    public static final I18n INVENTORY_SYNC_ADDED = of("inventory.sync.added");
    public static final I18n INVENTORY_SYNC_ADDING = of("inventory.sync.adding");
    public static final I18n INVENTORY_SYNC_CANCELLED = of("inventory.sync.cancelled");
    public static final I18n INVENTORY_SYNC_COMPLETE = of("inventory.sync.complete");
    public static final I18n INVENTORY_SYNC_CONTAINER_CANNOT_OPEN = of("inventory.sync.container_cannot_open");
    public static final I18n INVENTORY_SYNC_NOT_CONTAINER = of("inventory.sync.not_container");
    public static final I18n INVENTORY_SYNC_TOO_FAR = of("inventory.sync.too_far");
    public static final I18n INVENTORY_SYNC_REMAINING = of("inventory.sync.remaining");
    public static final I18n INVENTORY_SYNC_DISABLED = of("inventory.sync.disabled");
    public static final I18n INVENTORY_SYNC_ENABLED = of("inventory.sync.enabled");
    public static final I18n INVENTORY_SYNC_CLEARED = of("inventory.sync.cleared");

    public static final I18n INVENTORY_BACKPACK_FULL = of("inventory.backpack_full");
    public static final I18n INVENTORY_RESTORE_FAILED = of("inventory.restore_failed");
    public static final I18n INVENTORY_SHULKER_PRESELECT = of("inventory.shulker_preselect");
    public static final I18n INVENTORY_SYNC_OPEN_FAILED = of("inventory.sync.open_failed");

    public static final I18n BREWINGSTAND_LOWER = of("brewingstand.lower");
    public static final I18n BREWINGSTAND_RAISE = of("brewingstand.raise");

    public static final I18n BLOCK_NO_SUPPORT = of("block.no_support");
    public static final I18n BLOCK_MISMATCH = of("block.mismatch");

    public static final I18n UPDATE_AVAILABLE = of("update.available");
    public static final I18n UPDATE_DOWNLOAD = of("update.download");
    public static final I18n UPDATE_FAILED = of("update.failed");
    public static final I18n UPDATE_PASSWORD = of("update.password");
    public static final I18n UPDATE_RECOMMENDATION = of("update.recommendation");
    public static final I18n UPDATE_REPOSITORY = of("update.repository");

    private static final String PREFIX_CONFIG = "config";
    private static final String PREFIX_COMMENT = "desc";

    private final @Nullable String prefix;
    private final String nameKey;
    private final String withPrefixNameKey;
    private final String descKey;
    private final String configNameKey;
    private final String configDescKey;

    private I18n(@Nullable String prefix, String nameKey) {
        this.prefix = prefix;
        this.nameKey = nameKey;
        this.withPrefixNameKey = prefix == null ? nameKey : prefix + "." + nameKey;
        this.descKey = withPrefixNameKey + "." + PREFIX_COMMENT;
        String configNameKey = prefix == null ? PREFIX_CONFIG : prefix + "." + PREFIX_CONFIG;
        this.configNameKey = configNameKey + "." + nameKey;
        this.configDescKey = configNameKey + "." + nameKey + "." + PREFIX_COMMENT;
    }

    public static I18n of(@Nullable String prefix, String key) {
        return new I18n(prefix, key);
    }

    public static I18n of(String key) {
        return new I18n(Reference.MOD_ID, key);
    }

    /*** 获取键名 ***/
    public MutableComponent getName() {
        return MessageUtils.translatable(this.withPrefixNameKey);
    }

    /*** 获取键名(带参数) ***/
    public MutableComponent getName(Object... objects) {
        return MessageUtils.translatable(this.withPrefixNameKey, objects);
    }

    /*** 获取描述 ***/
    public MutableComponent getDesc() {
        return MessageUtils.translatable(this.descKey);
    }

    /*** 获取描述(带参数) ***/
    public MutableComponent getDesc(Object... objects) {
        return MessageUtils.translatable(this.descKey, objects);
    }

    /*** 获取配置键名 ***/
    public MutableComponent getConfigName() {
        return MessageUtils.translatable(this.configNameKey);
    }

    /*** 获取配置键名(带参数) ***/
    public MutableComponent getConfigName(Object... objects) {
        return MessageUtils.translatable(this.configNameKey, objects);
    }

    /*** 获取配置描述 ***/
    public MutableComponent getConfigDesc() {
        return MessageUtils.translatable(this.configDescKey);
    }

    /*** 获取配置描述(带参数) ***/
    public MutableComponent getConfigDesc(Object... objects) {
        return MessageUtils.translatable(this.configDescKey, objects);
    }

    /*** 获取简易键名(一般用于枚举, 会取 "." 最后的文本) ***/
    public String getSimpleKey() {
        if (nameKey == null || nameKey.isEmpty()) {
            return nameKey == null ? "" : nameKey;
        }
        int lastDotIndex = nameKey.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return nameKey;
        }
        if (lastDotIndex == nameKey.length() - 1) {
            return "";
        }
        return nameKey.substring(lastDotIndex + 1);
    }
}