package me.aleksilassila.litematica.printer;

import me.aleksilassila.litematica.printer.utils.StringUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 本地化翻译枚举，管理所有打印机模组的翻译键
 */
public enum I18n {
    // @formatter:off

    // 这个是之前做回调开关时候添加的开关提示语, 现在应该是用不到了
    MESSAGE_TOGGLED        ("message.toggled"),
    MESSAGE_VALUE_OFF      ("message.value.off"),
    MESSAGE_VALUE_ON       ("message.value.on"),

    OPEN_SCREEN("openScreen"),

    TAB_ALL     ("category.all"),
    TAB_GENERAL ("category.general"),
    TAB_PUT     ("category.put"),
    TAB_EXCAVATE("category.excavate"),
    TAB_BEDROCK ("category.bedrock"),
    TAB_HOTKEYS ("category.hotkeys"),
    TAB_COLOR   ("category.color"),

    // 基础提示
    AUTO_DISABLE_NOTICE                 ("auto_disable_notice"),

    // 配置项
    BLOCKLIST                           ("blocklist"),
    DEBUG_OUTPUT                        ("debugOutput"),
    EASY_PLACE_PROTOCOL                 ("easyPlaceProtocol"),
    FILL_MODE_FACING                    ("fillModeFacing"),
    FLUID_MODE_FILL_FLOWING             ("fluidModeFillFlowing"),
    PRINT_BREAK_WRONG_BLOCK             ("printBreakWrongBlock"),
    PRINT_WATER                         ("printWater"),
    PRINTER_AUTO_DISABLE                ("printerAutoDisable"),
    PRINTER_AUTO_FILL_COMPOSTER         ("printerAutoFillComposter"),
    PRINTER_AUTO_STRIP_LOGS             ("printerAutoStripLogs"),
    PRINTER_AUTO_TUNING                 ("printerAutoTuning"),
    PRINTER_BLOCKS_PER_TICK             ("printerBlocksPerTick"),
    PRINTER_BREAK_EXTRA_BLOCK           ("printerBreakExtraBlock"),
    PRINTER_BREAK_WRONG_STATE_BLOCK     ("printerBreakWrongStateBlock"),
    PRINTER_FALLING_BLOCK_CHECK         ("printerFallingBlockCheck"),
    PRINTER_ITERATOR_MODE               ("printerIteratorMode"),
    PRINTER_ITERATOR_SHAPE              ("printerIteratorShape"),
    PRINTER_ITERATOR_USE_TIME           ("printerIteratorUseTime"),
    PRINTER_LAG_CHECK                   ("printerLagCheck"),
    PRINTER_PLACE_COOLDOWN              ("printerPlaceCooldown"),
    PRINTER_QUICK_SHULKER               ("printerQuickShulker"),
    PRINTER_QUICK_SHULKER_COOLDOWN      ("printerQuickShulkerCooldown"),
    PRINTER_QUICK_SHULKER_MODE          ("printerQuickShulkerMode"),
    PRINTER_RANGE                       ("printerRange"),
    PRINTER_SAFELY_OBSERVER             ("printerSafelyObserver"),
    PRINTER_SKIP_WATERLOGGED            ("printerSkipWaterlogged"),
    PRINTER_SPEED                       ("printerSpeed"),
    PRINTER_USE_PACKET                  ("printerUsePacket"),
    PRINTER_WORKING_COUNT_PER_TICK      ("printerWorkingCountPerTick"),
    PRINTER_X_AXIS_REVERSE              ("printerXAxisReverse"),
    PRINTER_Y_AXIS_REVERSE              ("printerYAxisReverse"),
    PRINTER_Z_AXIS_REVERSE              ("printerZAxisReverse"),
    UPDATE_CHECK                        ("updateCheck"),

    MODE_SWITCH                         ("modeSwitch"),
    PRINTER_MODE                        ("printerMode"),
    MULTI_BREAK                         ("multiBreak"),
    RENDER_LAYER_LIMIT                  ("renderLayerLimit"),
    PRINT_IN_AIR                        ("printInAir"),
    PRINT_SWITCH                        ("printSwitch"),
    USE_EASYPLACE                       ("useEasyplace"),
    FORCED_SNEAK                        ("forcedSneak"),
    REPLACE                             ("replace"),
    SWITCH_PRINTER_MODE                 ("switchPrinterMode"),
    MINE                                ("mine"),
    FLUID                               ("fluid"),
    FILL                                ("fill"),
    BEDROCK                             ("bedrock"),
    CLOSE_ALL_MODE                      ("closeAllMode"),
    PUT_SKIP                            ("putSkip"),
    CLOUD_INVENTORY                     ("cloudInventory"),
    AUTO_INVENTORY                      ("autoInventory"),
    STORE_ORDERLY                       ("storeOrderly"),
    EXCAVATE_LIMITER                    ("excavateLimiter"),
    EXCAVATE_LIMIT                      ("excavateLimit"),
    SYNC_INVENTORY_COLOR                ("syncInventoryColor"),
    REPLACE_CORAL                       ("replaceCoral"),
    RENDER_HUD                          ("renderHud"),
    STRIP_LOGS                          ("stripLogs"),

    // 配置列表值（仅生成list key）
    FILL_MODE_FACING_DOWN               ("fillModeFacing.down"),
    FILL_MODE_FACING_EAST               ("fillModeFacing.east"),
    FILL_MODE_FACING_NORTH              ("fillModeFacing.north"),
    FILL_MODE_FACING_SOUTH              ("fillModeFacing.south"),
    FILL_MODE_FACING_UP                 ("fillModeFacing.up"),
    FILL_MODE_FACING_WEST               ("fillModeFacing.west"),

    ITERATOR_SHAPE_TYPE_CUBE            ("iteratorShapeType.cube"),
    ITERATOR_SHAPE_TYPE_OCTAHEDRON      ("iteratorShapeType.octahedron"),
    ITERATOR_SHAPE_TYPE_SPHERE          ("iteratorShapeType.sphere"),

    PRINTER_QUICK_SHULKER_MODE_CLICK_SLOT   ("printerQuickShulkerMode.click_slot"),
    PRINTER_QUICK_SHULKER_MODE_INVOKE       ("printerQuickShulkerMode.invoke"),

    FLUID_BLOCK_LIST       ("fluidBlockList"),
    FLUID_LIST             ("fluidList"),
    FILL_BLOCK_MODE        ("fillBlockMode"),
    FILE_BLOCK_MODE_TYPE_WHITELIST        ("fillBlockModeType.whitelist"),
    FILE_BLOCK_MODE_TYPE_HANDHELD         ("fillBlockModeType.handheld"),
    FILL_BLOCK_LIST        ("fillBlockList"),
    INVENTORY_LIST         ("inventoryList"),
    EXCAVATE_WHITELIST     ("excavateWhitelist"),
    EXCAVATE_BLACKLIST     ("excavateBlacklist"),
    PUT_SKIP_LIST          ("putSkipList"),
    REPLACEABLE_LIST       ("replaceableList"),

    MENU_SETTINGS_BUTTON   ("menu.settings_button"),

    UPDATE_AVAILABLE       ("update.available"),
    UPDATE_DOWNLOAD        ("update.download"),
    UPDATE_FAILED          ("update.failed"),
    UPDATE_PASSWORD        ("update.password"),
    UPDATE_RECOMMENDATION  ("update.recommendation"),
    UPDATE_REPOSITORY      ("update.repository"),

    SYNC_INVENTORY_CHECK   ("syncInventoryCheck"),
    PRINT                  ("print"),
    SYNC_INVENTORY         ("syncInventory"),
    PRINTER_INVENTORY      ("printerInventory"),
    REMOVE_PRINT_INVENTORY ("removePrintInventory"),
    LAST                   ("last"),
    NEXT                   ("next"),
    DELETE                 ("delete"),

    // 配置列表选项 - 打印模式
    PRINT_MODE_PRINTER     ("printMode.printer"),
    PRINT_MODE_MINE        ("printMode.mine"),
    PRINT_MODE_FLUID       ("printMode.fluid"),
    PRINT_MODE_FILL        ("printMode.fill"),
    PRINT_MODE_REPLACE     ("printMode.replace"),
    PRINT_MODE_BEDROCK     ("printMode.bedrock"),

    // 配置列表选项 - 挖掘列表模式
    EXCAVATE_LIST_MODE_TWEAKEROO  ("excavateListMode.tweakeroo"),
    EXCAVATE_LIST_MODE_CUSTOM     ("excavateListMode.custom"),

    // 配置列表选项 - 运行模式（多模/单模）
    MODE_TYPE_MULTI        ("modeType.multi"),
    MODE_TYPE_SINGLE       ("modeType.single"),

    // 配置列表选项 - 迭代顺序
    ITERATION_ORDER_XYZ    ("iterationOrder.xyz"),
    ITERATION_ORDER_XZY    ("iterationOrder.xzy"),
    ITERATION_ORDER_YXZ    ("iterationOrder.yxz"),
    ITERATION_ORDER_YZX    ("iterationOrder.yzx"),
    ITERATION_ORDER_ZXY    ("iterationOrder.zxy"),
    ITERATION_ORDER_ZYX    ("iterationOrder.zyx")

    ;
    // @formatter:on

    private static final String MOD_PREFIX = LitematicaPrinterMod.MOD_ID + ".";
    private static final String CONFIG_PREFIX = MOD_PREFIX + "config.";
    private static final String CONFIG_NAME_PREFIX = CONFIG_PREFIX + "name.";
    private static final String CONFIG_COMMENT_PREFIX = CONFIG_PREFIX + "comment.";
    private static final String CONFIG_LIST_PREFIX = CONFIG_PREFIX + "list.";

    private final String key;
    private final boolean isRawKey;

    public String getRawKey() {
        return key;
    }

    public boolean isRawKey() {
        return isRawKey;
    }

    I18n(String key, boolean isRawKey) {
        this.key = key;
        this.isRawKey = isRawKey;
    }

    I18n(String key) {
        this(key, false);
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