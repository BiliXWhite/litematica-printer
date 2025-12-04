package me.aleksilassila.litematica.printer;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * 本地化翻译枚举，管理所有打印机模组的翻译键
 */
public enum I18n {

    // 基础提示（非配置项）
    AUTO_DISABLE_NOTICE("auto_disable_notice", Type.BASE),
    PRINT_COMPLETE_NOTICE("print_complete_notice", Type.BASE), // 新增：打印完成通知（基础提示）

    // 配置项（核心，可生成name/comment）
    BLOCKLIST("blocklist", Type.CONFIG),
    DEBUG_OUTPUT("debugOutput", Type.CONFIG),
    EASY_PLACE_PROTOCOL("easyPlaceProtocol", Type.CONFIG),
    FILL_MODE_FACING("fillModeFacing", Type.CONFIG),
    FLUID_MODE_FILL_FLOWING("fluidModeFillFlowing", Type.CONFIG),
    PRINT_BREAK_WRONG_BLOCK("printBreakWrongBlock", Type.CONFIG),
    PRINT_WATER("printWater", Type.CONFIG),
    PRINTER_AUTO_DISABLE("printerAutoDisable", Type.CONFIG),
    PRINTER_AUTO_FILL_COMPOSTER("printerAutoFillComposter", Type.CONFIG),
    PRINTER_AUTO_STRIP_LOGS("printerAutoStripLogs", Type.CONFIG),
    PRINTER_AUTO_TUNING("printerAutoTuning", Type.CONFIG),
    PRINTER_BLOCKS_PER_TICK("printerBlocksPerTick", Type.CONFIG),
    PRINTER_BREAK_EXTRA_BLOCK("printerBreakExtraBlock", Type.CONFIG),
    PRINTER_BREAK_WRONG_STATE_BLOCK("printerBreakWrongStateBlock", Type.CONFIG),
    PRINTER_FALLING_BLOCK_CHECK("printerFallingBlockCheck", Type.CONFIG),
    PRINTER_ITERATOR_MODE("printerIteratorMode", Type.CONFIG),
    PRINTER_ITERATOR_SHAPE("printerIteratorShape", Type.CONFIG),
    PRINTER_ITERATOR_USE_TIME("printerIteratorUseTime", Type.CONFIG),
    PRINTER_LAG_CHECK("printerLagCheck", Type.CONFIG),
    PRINTER_PLACE_COOLDOWN("printerPlaceCooldown", Type.CONFIG),
    PRINTER_QUICK_SHULKER("printerQuickShulker", Type.CONFIG),
    PRINTER_QUICK_SHULKER_COOLDOWN("printerQuickShulkerCooldown", Type.CONFIG),
    PRINTER_QUICK_SHULKER_MODE("printerQuickShulkerMode", Type.CONFIG),
    PRINTER_RANGE("printerRange", Type.CONFIG),
    PRINTER_SAFELY_OBSERVER("printerSafelyObserver", Type.CONFIG),
    PRINTER_SKIP_WATERLOGGED("printerSkipWaterlogged", Type.CONFIG),
    PRINTER_SPEED("printerSpeed", Type.CONFIG),
    PRINTER_USE_PACKET("printerUsePacket", Type.CONFIG),
    PRINTER_WORKING_COUNT_PER_TICK("printerWorkingCountPerTick", Type.CONFIG),
    PRINTER_X_AXIS_REVERSE("printerXAxisReverse", Type.CONFIG),
    PRINTER_Y_AXIS_REVERSE("printerYAxisReverse", Type.CONFIG),
    PRINTER_Z_AXIS_REVERSE("printerZAxisReverse", Type.CONFIG),
    UPDATE_CHECK("updateCheck", Type.CONFIG),

    MODE_SWITCH("modeSwitch", Type.CONFIG),
    PRINTER_MODE("printerMode", Type.CONFIG),
    MULTI_BREAK("multiBreak", Type.CONFIG),
    RENDER_LAYER_LIMIT("renderLayerLimit", Type.CONFIG),
    PRINT_IN_AIR("printInAir", Type.CONFIG),
    PRINT_SWITCH("printSwitch", Type.CONFIG),
    USE_EASYPLACE("useEasyplace", Type.CONFIG),
    FORCED_SNEAK("forcedSneak", Type.CONFIG),
    REPLACE("replace", Type.CONFIG),
    SWITCH_PRINTER_MODE("switchPrinterMode", Type.CONFIG),
    MINE("mine", Type.CONFIG),
    FLUID("fluid", Type.CONFIG),
    FILL("fill", Type.CONFIG),
    BEDROCK("bedrock", Type.CONFIG),
    CLOSE_ALL_MODE("closeAllMode", Type.CONFIG),
    PUT_SKIP("putSkip", Type.CONFIG),
    CLOUD_INVENTORY("cloudInventory", Type.CONFIG),
    AUTO_INVENTORY("autoInventory", Type.CONFIG),
    STORE_ORDERLY("storeOrderly", Type.CONFIG),
    EXCAVATE_LIMITER("excavateLimiter", Type.CONFIG),
    EXCAVATE_LIMIT("excavateLimit", Type.CONFIG),
    SYNC_INVENTORY_COLOR("syncInventoryColor", Type.CONFIG),
    REPLACE_CORAL("replaceCoral", Type.CONFIG),
    RENDER_HUD("renderHud", Type.CONFIG),
    STRIP_LOGS("stripLogs", Type.CONFIG),

    // 配置列表值（仅生成list key）
    FILL_MODE_FACING_DOWN("fillModeFacing.down", Type.CONFIG_LIST),
    FILL_MODE_FACING_EAST("fillModeFacing.east", Type.CONFIG_LIST),
    FILL_MODE_FACING_NORTH("fillModeFacing.north", Type.CONFIG_LIST),
    FILL_MODE_FACING_SOUTH("fillModeFacing.south", Type.CONFIG_LIST),
    FILL_MODE_FACING_UP("fillModeFacing.up", Type.CONFIG_LIST),
    FILL_MODE_FACING_WEST("fillModeFacing.west", Type.CONFIG_LIST),
    ITERATOR_SHAPE_TYPE_CUBE("iteratorShapeType.cube", Type.CONFIG_LIST),
    ITERATOR_SHAPE_TYPE_OCTAHEDRON("iteratorShapeType.octahedron", Type.CONFIG_LIST),
    ITERATOR_SHAPE_TYPE_SPHERE("iteratorShapeType.sphere", Type.CONFIG_LIST),
    PRINTER_QUICK_SHULKER_MODE_CLICK_SLOT("printerQuickShulkerMode.click_slot", Type.CONFIG_LIST),
    PRINTER_QUICK_SHULKER_MODE_INVOKE("printerQuickShulkerMode.invoke", Type.CONFIG_LIST),

    FLUID_BLOCK_LIST("fluidBlockList", Type.CONFIG),
    FLUID_LIST("fluidList", Type.CONFIG),
    FILL_BLOCK_LIST("fillBlockList", Type.CONFIG),
    INVENTORY_LIST("inventoryList", Type.CONFIG),
    EXCAVATE_WHITELIST("excavateWhitelist", Type.CONFIG),
    EXCAVATE_BLACKLIST("excavateBlacklist", Type.CONFIG),
    PUT_SKIP_LIST("putSkipList", Type.CONFIG),
    REPLACEABLE_LIST("replaceableList", Type.CONFIG),

    // 菜单相关（非配置项）
    MENU_SETTINGS_BUTTON("menu.settings_button", Type.BASE),

    // 更新相关（非配置项）
    UPDATE_AVAILABLE("update.available", Type.BASE),
    UPDATE_DOWNLOAD("update.download", Type.BASE),
    UPDATE_FAILED("update.failed", Type.BASE),
    UPDATE_PASSWORD("update.password", Type.BASE),
    UPDATE_RECOMMENDATION("update.recommendation", Type.BASE),
    UPDATE_REPOSITORY("update.repository", Type.BASE),

    // ModMenu 相关（完整key直接传入，无需拼接）
    MODMENU_SUMMARY("modmenu.summaryTranslation.litematica-printer", Type.RAW),
    MODMENU_NAME("modmenu.nameTranslation.litematica-printer", Type.RAW),
    MODMENU_DESCRIPTION("modmenu.descriptionTranslation.litematica-printer", Type.RAW),

    // 热键相关（BASE类型，非配置项但需翻译）
    PRINT_HOTKEY("hotkey.print", Type.BASE),
    TOGGLE_PRINTING_MODE_HOTKEY("hotkey.togglePrintingMode", Type.BASE),
    SYNC_INVENTORY_HOTKEY("hotkey.syncInventory", Type.BASE),
    SYNC_INVENTORY_CHECK("syncInventoryCheck", Type.CONFIG),
    PRINTER_INVENTORY_HOTKEY("hotkey.printerInventory", Type.BASE),
    REMOVE_PRINT_INVENTORY_HOTKEY("hotkey.removePrintInventory", Type.BASE),
    LAST_HOTKEY("hotkey.last", Type.BASE),
    NEXT_HOTKEY("hotkey.next", Type.BASE),
    DELETE_HOTKEY("hotkey.delete", Type.BASE),

    PRINT("print", Type.CONFIG),
    TOGGLE_PRINTING_MODE("togglePrintingMode", Type.CONFIG),
    SYNC_INVENTORY("syncInventory", Type.CONFIG),
    PRINTER_INVENTORY("printerInventory", Type.CONFIG),
    REMOVE_PRINT_INVENTORY("removePrintInventory", Type.CONFIG),
    LAST("last", Type.CONFIG),
    NEXT("next", Type.CONFIG),
    DELETE("delete", Type.CONFIG),

    MESSAGE_TOGGLED("message.toggled", Type.BASE),
    MESSAGE_VALUE_OFF("message.value.off", Type.BASE),
    MESSAGE_VALUE_ON("message.value.on", Type.BASE);

    private enum Type {
        BASE,        // 基础项（拼接mod前缀：modid.xxx）
        CONFIG,      // 配置项（可生成name/comment）
        CONFIG_LIST, // 配置列表值（仅生成list key）
        RAW          // 原始key（直接使用，不拼接）
    }

    private static final String MOD_PREFIX = LitematicaPrinterMod.MOD_ID + ".";
    private static final String CONFIG_PREFIX = MOD_PREFIX + "config.";
    private static final String CONFIG_NAME_PREFIX = CONFIG_PREFIX + "name.";
    private static final String CONFIG_COMMENT_PREFIX = CONFIG_PREFIX + "comment.";
    private static final String CONFIG_LIST_PREFIX = CONFIG_PREFIX + "list.";

    private final String keySegment;
    private final Type type;

    I18n(String keySegment, Type type) {
        this.keySegment = keySegment;
        this.type = type;
    }


    public String getFullKey() {
        return switch (type) {
            case BASE -> MOD_PREFIX + keySegment;
            case CONFIG -> CONFIG_NAME_PREFIX + keySegment;
            case CONFIG_LIST -> CONFIG_LIST_PREFIX + keySegment;
            case RAW -> keySegment;
        };
    }

    public Component getBaseComponent() {
        if (type != Type.BASE) {
            throw new IllegalArgumentException("枚举项 " + name() + " 不是基础项，无法调用getBaseComponent()");
        }
        return StringUtils.translatable(getFullKey());
    }

    public Component getConfigName() {
        if (type != Type.CONFIG) {
            throw new IllegalArgumentException("枚举项 " + name() + " 不是配置项，无法调用getConfigName()");
        }
        return StringUtils.translatable(CONFIG_NAME_PREFIX + keySegment);
    }

    public Component getConfigComment() {
        if (type != Type.CONFIG) {
            throw new IllegalArgumentException("枚举项 " + name() + " 不是配置项，无法调用getConfigComment()");
        }
        return StringUtils.translatable(CONFIG_COMMENT_PREFIX + keySegment);
    }

    public Component getConfigList() {
        if (type != Type.CONFIG_LIST) {
            throw new IllegalArgumentException("枚举项 " + name() + " 不是配置列表项，无法调用getConfigList()");
        }
        return StringUtils.translatable(CONFIG_LIST_PREFIX + keySegment);
    }

    public Component getRawComponent() {
        if (type != Type.RAW) {
            throw new IllegalArgumentException("枚举项 " + name() + " 不是原始key项，无法调用getRawComponent()");
        }
        return StringUtils.translatable(keySegment);
    }

    public String getBaseString() {
        return getBaseComponent().getString();
    }

    public String getConfigNameString() {
        return getConfigName().getString();
    }

    public String getConfigCommentString() {
        return getConfigComment().getString();
    }

    public String getConfigListString() {
        return getConfigList().getString();
    }

    public String getRawString() {
        return getRawComponent().getString();
    }

    public String getConfigSimpleKey() {
        if (type != Type.CONFIG) {
            throw new IllegalArgumentException("枚举项 " + name() + " 不是配置项，无法调用getConfigSimpleKey()");
        }
        return keySegment;
    }


    public static @Nullable I18n get(String name) {
        for (I18n i18n : values()) {
            if (i18n.getFullKey().equalsIgnoreCase(name)) {
                return i18n;
            }
        }
        return null;
    }

}