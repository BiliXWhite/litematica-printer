package me.aleksilassila.litematica.printer;

import me.aleksilassila.litematica.printer.utils.StringUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 本地化翻译类，管理所有打印机模组的翻译键
 * 重构说明：从枚举改为普通类，支持new实例化，原有枚举值转为静态常量
 */
public class I18n {
    // @formatter:off

    // 原枚举值改为静态I18n实例（保持原有命名和key不变）
    // 这个是之前做回调开关时候添加的开关提示语, 现在应该是用不到了
    public static final I18n MESSAGE_TOGGLED        = new I18n("message.toggled");
    public static final I18n MESSAGE_VALUE_OFF      = new I18n("message.value.off");
    public static final I18n MESSAGE_VALUE_ON       = new I18n("message.value.on");

    public static final I18n OPEN_SCREEN = new I18n("openScreen");

    public static final I18n TAB_ALL     = new I18n("category.all");
    public static final I18n TAB_GENERAL = new I18n("category.general");
    public static final I18n TAB_PUT     = new I18n("category.put");
    public static final I18n TAB_EXCAVATE = new I18n("category.excavate");
    public static final I18n TAB_BEDROCK = new I18n("category.bedrock");
    public static final I18n TAB_HOTKEYS = new I18n("category.hotkeys");
    public static final I18n TAB_COLOR   = new I18n("category.color");

    // 基础提示
    public static final I18n AUTO_DISABLE_NOTICE                 = new I18n("auto_disable_notice");

    // 配置项
    public static final I18n BLOCKLIST                           = new I18n("blocklist");
    public static final I18n DEBUG_OUTPUT                        = new I18n("debugOutput");
    public static final I18n EASY_PLACE_PROTOCOL                 = new I18n("easyPlaceProtocol");
    public static final I18n FILL_MODE_FACING                    = new I18n("fillModeFacing");
    public static final I18n FLUID_MODE_FILL_FLOWING             = new I18n("fluidModeFillFlowing");
    public static final I18n PRINT_BREAK_WRONG_BLOCK             = new I18n("printBreakWrongBlock");
    public static final I18n PRINT_WATER                         = new I18n("printWater");
    public static final I18n PRINTER_AUTO_DISABLE                = new I18n("printerAutoDisable");
    public static final I18n PRINTER_AUTO_FILL_COMPOSTER         = new I18n("printerAutoFillComposter");
    public static final I18n PRINTER_AUTO_STRIP_LOGS             = new I18n("printerAutoStripLogs");
    public static final I18n PRINTER_AUTO_TUNING                 = new I18n("printerAutoTuning");
    public static final I18n PRINTER_BLOCKS_PER_TICK             = new I18n("printerBlocksPerTick");
    public static final I18n PRINTER_BREAK_EXTRA_BLOCK           = new I18n("printerBreakExtraBlock");
    public static final I18n PRINTER_BREAK_WRONG_STATE_BLOCK     = new I18n("printerBreakWrongStateBlock");
    public static final I18n PRINTER_FALLING_BLOCK_CHECK         = new I18n("printerFallingBlockCheck");
    public static final I18n PRINTER_ITERATOR_MODE               = new I18n("printerIteratorMode");
    public static final I18n PRINTER_ITERATOR_SHAPE              = new I18n("printerIteratorShape");
    public static final I18n PRINTER_ITERATOR_USE_TIME           = new I18n("printerIteratorUseTime");
    public static final I18n PRINTER_LAG_CHECK                   = new I18n("printerLagCheck");
    public static final I18n PRINTER_PLACE_COOLDOWN              = new I18n("printerPlaceCooldown");
    public static final I18n PRINTER_QUICK_SHULKER               = new I18n("printerQuickShulker");
    public static final I18n PRINTER_QUICK_SHULKER_COOLDOWN      = new I18n("printerQuickShulkerCooldown");
    public static final I18n PRINTER_QUICK_SHULKER_MODE          = new I18n("printerQuickShulkerMode");
    public static final I18n PRINTER_RANGE                       = new I18n("printerRange");
    public static final I18n PRINTER_SAFELY_OBSERVER             = new I18n("printerSafelyObserver");
    public static final I18n PRINTER_SKIP_WATERLOGGED            = new I18n("printerSkipWaterlogged");
    public static final I18n PRINTER_SPEED                       = new I18n("printerSpeed");
    public static final I18n PRINTER_USE_PACKET                  = new I18n("printerUsePacket");
    public static final I18n PRINTER_WORKING_COUNT_PER_TICK      = new I18n("printerWorkingCountPerTick");
    public static final I18n PRINTER_X_AXIS_REVERSE              = new I18n("printerXAxisReverse");
    public static final I18n PRINTER_Y_AXIS_REVERSE              = new I18n("printerYAxisReverse");
    public static final I18n PRINTER_Z_AXIS_REVERSE              = new I18n("printerZAxisReverse");
    public static final I18n UPDATE_CHECK                        = new I18n("updateCheck");

    public static final I18n MODE_SWITCH                         = new I18n("modeSwitch");
    public static final I18n PRINTER_MODE                        = new I18n("printerMode");
    public static final I18n MULTI_BREAK                         = new I18n("multiBreak");
    public static final I18n RENDER_LAYER_LIMIT                  = new I18n("renderLayerLimit");
    public static final I18n PRINT_IN_AIR                        = new I18n("printInAir");
    public static final I18n PRINT_SWITCH                        = new I18n("printSwitch");
    public static final I18n USE_EASYPLACE                       = new I18n("useEasyplace");
    public static final I18n FORCED_SNEAK                        = new I18n("forcedSneak");
    public static final I18n REPLACE                             = new I18n("replace");
    public static final I18n SWITCH_PRINTER_MODE                 = new I18n("switchPrinterMode");
    public static final I18n MINE                                = new I18n("mine");
    public static final I18n FLUID                               = new I18n("fluid");
    public static final I18n FILL                                = new I18n("fill");
    public static final I18n BEDROCK                             = new I18n("bedrock");
    public static final I18n CLOSE_ALL_MODE                      = new I18n("closeAllMode");
    public static final I18n PUT_SKIP                            = new I18n("putSkip");
    public static final I18n CLOUD_INVENTORY                     = new I18n("cloudInventory");
    public static final I18n AUTO_INVENTORY                      = new I18n("autoInventory");
    public static final I18n STORE_ORDERLY                       = new I18n("storeOrderly");
    public static final I18n EXCAVATE_LIMITER                    = new I18n("excavateLimiter");
    public static final I18n EXCAVATE_LIMIT                      = new I18n("excavateLimit");
    public static final I18n SYNC_INVENTORY_COLOR                = new I18n("syncInventoryColor");
    public static final I18n REPLACE_CORAL                       = new I18n("replaceCoral");
    public static final I18n RENDER_HUD                          = new I18n("renderHud");
    public static final I18n STRIP_LOGS                          = new I18n("stripLogs");

    // 配置列表值（仅生成list key）
    public static final I18n FILL_MODE_FACING_DOWN               = new I18n("fillModeFacing.down");
    public static final I18n FILL_MODE_FACING_EAST               = new I18n("fillModeFacing.east");
    public static final I18n FILL_MODE_FACING_NORTH              = new I18n("fillModeFacing.north");
    public static final I18n FILL_MODE_FACING_SOUTH              = new I18n("fillModeFacing.south");
    public static final I18n FILL_MODE_FACING_UP                 = new I18n("fillModeFacing.up");
    public static final I18n FILL_MODE_FACING_WEST               = new I18n("fillModeFacing.west");

    public static final I18n ITERATOR_SHAPE_TYPE_CUBE            = new I18n("iteratorShapeType.cube");
    public static final I18n ITERATOR_SHAPE_TYPE_OCTAHEDRON      = new I18n("iteratorShapeType.octahedron");
    public static final I18n ITERATOR_SHAPE_TYPE_SPHERE          = new I18n("iteratorShapeType.sphere");

    public static final I18n PRINTER_QUICK_SHULKER_MODE_CLICK_SLOT   = new I18n("printerQuickShulkerMode.click_slot");
    public static final I18n PRINTER_QUICK_SHULKER_MODE_INVOKE       = new I18n("printerQuickShulkerMode.invoke");

    public static final I18n FLUID_BLOCK_LIST       = new I18n("fluidBlockList");
    public static final I18n FLUID_LIST             = new I18n("fluidList");
    public static final I18n FILL_BLOCK_MODE        = new I18n("fillBlockMode");
    public static final I18n FILE_BLOCK_MODE_TYPE_WHITELIST        = new I18n("fillBlockModeType.whitelist");
    public static final I18n FILE_BLOCK_MODE_TYPE_HANDHELD         = new I18n("fillBlockModeType.handheld");
    public static final I18n FILL_BLOCK_LIST        = new I18n("fillBlockList");
    public static final I18n INVENTORY_LIST         = new I18n("inventoryList");
    public static final I18n EXCAVATE_WHITELIST     = new I18n("excavateWhitelist");
    public static final I18n EXCAVATE_BLACKLIST     = new I18n("excavateBlacklist");
    public static final I18n PUT_SKIP_LIST          = new I18n("putSkipList");
    public static final I18n REPLACEABLE_LIST       = new I18n("replaceableList");

    public static final I18n MENU_SETTINGS_BUTTON   = new I18n("menu.settings_button");

    public static final I18n UPDATE_AVAILABLE       = new I18n("update.available");
    public static final I18n UPDATE_DOWNLOAD        = new I18n("update.download");
    public static final I18n UPDATE_FAILED          = new I18n("update.failed");
    public static final I18n UPDATE_PASSWORD        = new I18n("update.password");
    public static final I18n UPDATE_RECOMMENDATION  = new I18n("update.recommendation");
    public static final I18n UPDATE_REPOSITORY      = new I18n("update.repository");

    public static final I18n SYNC_INVENTORY_CHECK   = new I18n("syncInventoryCheck");
    public static final I18n PRINT                  = new I18n("print");
    public static final I18n SYNC_INVENTORY         = new I18n("syncInventory");
    public static final I18n PRINTER_INVENTORY      = new I18n("printerInventory");
    public static final I18n REMOVE_PRINT_INVENTORY = new I18n("removePrintInventory");
    public static final I18n LAST                   = new I18n("last");
    public static final I18n NEXT                   = new I18n("next");
    public static final I18n DELETE                 = new I18n("delete");

    // 配置列表选项 - 打印模式
    public static final I18n PRINT_MODE_PRINTER     = new I18n("printMode.printer");
    public static final I18n PRINT_MODE_MINE        = new I18n("printMode.mine");
    public static final I18n PRINT_MODE_FLUID       = new I18n("printMode.fluid");
    public static final I18n PRINT_MODE_FILL        = new I18n("printMode.fill");
    public static final I18n PRINT_MODE_REPLACE     = new I18n("printMode.replace");
    public static final I18n PRINT_MODE_BEDROCK     = new I18n("printMode.bedrock");

    // 配置列表选项 - 挖掘列表模式
    public static final I18n EXCAVATE_LIST_MODE_TWEAKEROO  = new I18n("excavateListMode.tweakeroo");
    public static final I18n EXCAVATE_LIST_MODE_CUSTOM     = new I18n("excavateListMode.custom");

    // 配置列表选项 - 运行模式（多模/单模）
    public static final I18n MODE_TYPE_MULTI        = new I18n("modeType.multi");
    public static final I18n MODE_TYPE_SINGLE       = new I18n("modeType.single");

    // 配置列表选项 - 迭代顺序
    public static final I18n ITERATION_ORDER_XYZ    = new I18n("iterationOrder.xyz");
    public static final I18n ITERATION_ORDER_XZY    = new I18n("iterationOrder.xzy");
    public static final I18n ITERATION_ORDER_YXZ    = new I18n("iterationOrder.yxz");
    public static final I18n ITERATION_ORDER_YZX    = new I18n("iterationOrder.yzx");
    public static final I18n ITERATION_ORDER_ZXY    = new I18n("iterationOrder.zxy");
    public static final I18n ITERATION_ORDER_ZYX    = new I18n("iterationOrder.zyx");

    // @formatter:on

    // 原有静态前缀常量保留
    private static final String MOD_PREFIX = LitematicaPrinterMod.MOD_ID + ".";
    private static final String CONFIG_PREFIX = MOD_PREFIX + "config.";
    private static final String CONFIG_NAME_PREFIX = CONFIG_PREFIX + "name.";
    private static final String CONFIG_COMMENT_PREFIX = CONFIG_PREFIX + "comment.";
    private static final String CONFIG_LIST_PREFIX = CONFIG_PREFIX + "list.";

    // 原有成员变量保留
    private final String key;
    private final boolean isRawKey;

    // 构造方法改为public，支持外部new实例（核心修改点）
    public I18n(String key) {
        this(key, false);
    }

    public I18n(String key, boolean isRawKey) {
        this.key = key;
        this.isRawKey = isRawKey;
    }

    // 原有所有方法逻辑完全保留，确保功能兼容
    public String getRawKey() {
        return key;
    }

    public boolean isRawKey() {
        return isRawKey;
    }

    /*** 获取key(带模组ID前缀)  ***/
    public String getKey() {
        if (isRawKey) {
            return key;
        } else {
            return MOD_PREFIX + key;
        }
    }

    public String getConfigKey() {
        if (isRawKey) {
            return key;
        } else {
            return CONFIG_PREFIX + key;
        }
    }

    public String getConfigNameKey() {
        if (isRawKey) {
            return key;
        } else {
            return CONFIG_NAME_PREFIX + key;
        }
    }

    public String getConfigCommentKey() {
        if (isRawKey) {
            return key;
        } else {
            return CONFIG_COMMENT_PREFIX + key;
        }
    }

    public String getConfigListKey() {
        if (isRawKey) {
            return key;
        } else {
            return CONFIG_LIST_PREFIX + key;
        }
    }

    public MutableComponent getRawKeyComponent() {
        return StringUtils.translatable(getRawKey());
    }

    public MutableComponent getRawKeyComponent(Object... objects) {
        return StringUtils.translatable(getRawKey(), objects);
    }

    public MutableComponent getKeyComponent() {
        return StringUtils.translatable(getKey());
    }

    public MutableComponent getKeyComponent(Object... objects) {
        return StringUtils.translatable(getKey(), objects);
    }

    public MutableComponent getConfigKeyComponent() {
        return StringUtils.translatable(getConfigKey());
    }

    public MutableComponent getConfigKeyComponent(Object... objects) {
        return StringUtils.translatable(getConfigKey(), objects);
    }

    public MutableComponent getConfigNameKeyComponent() {
        return StringUtils.translatable(getConfigNameKey());
    }

    public MutableComponent getConfigNameKeyComponent(Object... objects) {
        return StringUtils.translatable(getConfigNameKey(), objects);
    }

    public MutableComponent getConfigCommentKeyComponent() {
        return StringUtils.translatable(getConfigCommentKey());
    }

    public MutableComponent getConfigCommentKeyComponent(Object... objects) {
        return StringUtils.translatable(getConfigCommentKey(), objects);
    }

    public MutableComponent getConfigListKeyComponent() {
        return StringUtils.translatable(getConfigListKey());
    }

    public MutableComponent getConfigListKeyComponent(Object... objects) {
        return StringUtils.translatable(getConfigListKey(), objects);
    }

    public String getSimpleKey() {
        if (key == null || key.isEmpty()) {
            return key == null ? "" : key;
        }
        int lastDotIndex = key.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return key;
        }
        if (lastDotIndex == key.length() - 1) {
            return "";
        }
        return key.substring(lastDotIndex + 1);
    }
}