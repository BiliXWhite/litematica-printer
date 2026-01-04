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
    public static final I18n CHECK_PLAYER_INTERACTION_RANGE = config("checkPlayerInteractionRange");

    public static final I18n PRINT_SELECTION_TYPE = config("printSelectionType");
    public static final I18n FILL_SELECTION_TYPE    = config("fillSelectionType");
    public static final I18n FLUID_SELECTION_TYPE   = config("fluidSelectionType");
    public static final I18n MINE_SELECTION_TYPE    = config("mineSelectionType");

    public static final I18n SELECTION_TYPE_LITEMATICA_SELECTION   = config("selectionType.litematica.selection");
    public static final I18n SELECTION_TYPE_LITEMATICA_RENDER_LAYER = config("selectionType.litematica.renderLayer");
    public static final I18n SELECTION_TYPE_LITEMATICA_SELECTION_BELOW_PLAYER = config("selectionType.litematica.selection.belowPlayer");
    public static final I18n SELECTION_TYPE_LITEMATICA_SELECTION_ABOVE_PLAYER = config("selectionType.litematica.selection.abovePlayer");

    public static final I18n MESSAGE_TOGGLED        = of("message.toggled");
    public static final I18n MESSAGE_VALUE_OFF      = of("message.value.off");
    public static final I18n MESSAGE_VALUE_ON       = of("message.value.on");

    public static final I18n OPEN_SCREEN = config("openScreen");
    public static final I18n TAB_ALL     = config("category.all");
    public static final I18n TAB_GENERAL = config("category.general");
    public static final I18n TAB_PUT     = config("category.put");
    public static final I18n TAB_EXCAVATE = config("category.excavate");
    public static final I18n TAB_BEDROCK = config("category.bedrock");
    public static final I18n TAB_HOTKEYS = config("category.hotkeys");
    public static final I18n TAB_COLOR   = config("category.color");
    // 基础提示
    public static final I18n AUTO_DISABLE_NOTICE                 = of("auto_disable_notice");
    // 配置项
    public static final I18n BLOCKLIST                           = config("blocklist");
    public static final I18n DEBUG_OUTPUT                        = config("debugOutput");
    public static final I18n EASY_PLACE_PROTOCOL                 = config("easyPlaceProtocol");
    public static final I18n FILL_MODE_FACING                    = config("fillModeFacing");
    public static final I18n FLUID_MODE_FILL_FLOWING             = config("fluidModeFillFlowing");
    public static final I18n PRINT_BREAK_WRONG_BLOCK             = config("printBreakWrongBlock");
    public static final I18n PRINT_WATER                         = config("printWater");
    public static final I18n PRINTER_AUTO_DISABLE                = config("printerAutoDisable");
    public static final I18n PRINTER_AUTO_FILL_COMPOSTER         = config("printerAutoFillComposter");
    public static final I18n PRINTER_AUTO_STRIP_LOGS             = config("printerAutoStripLogs");
    public static final I18n PRINTER_AUTO_TUNING                 = config("printerAutoTuning");
    public static final I18n PRINTER_BLOCKS_PER_TICK             = config("printerBlocksPerTick");
    public static final I18n PRINTER_BREAK_EXTRA_BLOCK           = config("printerBreakExtraBlock");
    public static final I18n PRINTER_BREAK_WRONG_STATE_BLOCK     = config("printerBreakWrongStateBlock");
    public static final I18n PRINTER_FALLING_BLOCK_CHECK         = config("printerFallingBlockCheck");
    public static final I18n PRINTER_ITERATOR_MODE               = config("printerIteratorMode");
    public static final I18n PRINTER_ITERATOR_SHAPE              = config("printerIteratorShape");
    public static final I18n PRINTER_ITERATOR_USE_TIME           = config("printerIteratorUseTime");
    public static final I18n PRINTER_LAG_CHECK                   = config("printerLagCheck");
    public static final I18n PRINTER_PLACE_COOLDOWN              = config("printerPlaceCooldown");
    public static final I18n PRINTER_QUICK_SHULKER               = config("printerQuickShulker");
    public static final I18n PRINTER_QUICK_SHULKER_COOLDOWN      = config("printerQuickShulkerCooldown");
    public static final I18n PRINTER_QUICK_SHULKER_MODE          = config("printerQuickShulkerMode");
    public static final I18n PRINTER_RANGE                       = config("printerRange");
    public static final I18n PRINTER_SAFELY_OBSERVER             = config("printerSafelyObserver");
    public static final I18n PRINTER_SKIP_WATERLOGGED            = config("printerSkipWaterlogged");
    public static final I18n PRINTER_SPEED                       = config("printerSpeed");
    public static final I18n PRINTER_USE_PACKET                  = config("printerUsePacket");
    public static final I18n PRINTER_WORKING_COUNT_PER_TICK      = config("printerWorkingCountPerTick");
    public static final I18n PRINTER_X_AXIS_REVERSE              = config("printerXAxisReverse");
    public static final I18n PRINTER_Y_AXIS_REVERSE              = config("printerYAxisReverse");
    public static final I18n PRINTER_Z_AXIS_REVERSE              = config("printerZAxisReverse");
    public static final I18n UPDATE_CHECK                        = config("updateCheck");
    public static final I18n MODE_SWITCH                         = config("modeSwitch");
    public static final I18n PRINTER_MODE                        = config("printerMode");
    public static final I18n MULTI_BREAK                         = config("multiBreak");
    public static final I18n RENDER_LAYER_LIMIT                  = config("renderLayerLimit");
    public static final I18n PRINT_IN_AIR                        = config("printInAir");
    public static final I18n PRINT_SWITCH                        = config("printSwitch");
    public static final I18n FORCED_SNEAK                        = config("forcedSneak");
    public static final I18n REPLACE                             = config("replace");
    public static final I18n SWITCH_PRINTER_MODE                 = config("switchPrinterMode");
    public static final I18n MINE                                = config("mine");
    public static final I18n FLUID                               = config("fluid");
    public static final I18n FILL                                = config("fill");
    public static final I18n BEDROCK                             = config("bedrock");
    public static final I18n CLOSE_ALL_MODE                      = config("closeAllMode");
    public static final I18n PUT_SKIP                            = config("putSkip");
    public static final I18n CLOUD_INVENTORY                     = config("cloudInventory");
    public static final I18n AUTO_INVENTORY                      = config("autoInventory");
    public static final I18n STORE_ORDERLY                       = config("storeOrderly");
    public static final I18n EXCAVATE_LIMITER                    = config("excavateLimiter");
    public static final I18n EXCAVATE_LIMIT                      = config("excavateLimit");
    public static final I18n SYNC_INVENTORY_COLOR                = config("syncInventoryColor");
    public static final I18n REPLACE_CORAL                       = config("replaceCoral");
    public static final I18n RENDER_HUD                          = config("renderHud");
    public static final I18n STRIP_LOGS                          = config("stripLogs");
    // 配置列表值（仅生成list id）
    public static final I18n FILL_MODE_FACING_DOWN               = config("fillModeFacing.down");
    public static final I18n FILL_MODE_FACING_EAST               = config("fillModeFacing.east");
    public static final I18n FILL_MODE_FACING_NORTH              = config("fillModeFacing.north");
    public static final I18n FILL_MODE_FACING_SOUTH              = config("fillModeFacing.south");
    public static final I18n FILL_MODE_FACING_UP                 = config("fillModeFacing.up");
    public static final I18n FILL_MODE_FACING_WEST               = config("fillModeFacing.west");
    public static final I18n ITERATOR_SHAPE_TYPE_CUBE            = config("iteratorShapeType.cube");
    public static final I18n ITERATOR_SHAPE_TYPE_OCTAHEDRON      = config("iteratorShapeType.octahedron");
    public static final I18n ITERATOR_SHAPE_TYPE_SPHERE          = config("iteratorShapeType.sphere");
    public static final I18n PRINTER_QUICK_SHULKER_MODE_CLICK_SLOT   = config("printerQuickShulkerMode.click_slot");
    public static final I18n PRINTER_QUICK_SHULKER_MODE_INVOKE       = config("printerQuickShulkerMode.invoke");
    public static final I18n FLUID_BLOCK_LIST       = config("fluidBlockList");
    public static final I18n FLUID_LIST             = config("fluidList");
    public static final I18n FILL_BLOCK_MODE        = config("fillBlockMode");
    public static final I18n FILE_BLOCK_MODE_TYPE_WHITELIST        = config("fillBlockModeType.whitelist");
    public static final I18n FILE_BLOCK_MODE_TYPE_HANDHELD         = config("fillBlockModeType.handheld");
    public static final I18n FILL_BLOCK_LIST        = config("fillBlockList");
    public static final I18n INVENTORY_LIST         = config("inventoryList");
    public static final I18n EXCAVATE_WHITELIST     = config("excavateWhitelist");
    public static final I18n EXCAVATE_BLACKLIST     = config("excavateBlacklist");
    public static final I18n PUT_SKIP_LIST          = config("putSkipList");
    public static final I18n REPLACEABLE_LIST       = config("replaceableList");
    public static final I18n MENU_SETTINGS_BUTTON   = config("menu.settings_button");
    public static final I18n UPDATE_AVAILABLE       = config("update.available");
    public static final I18n UPDATE_DOWNLOAD        = config("update.download");
    public static final I18n UPDATE_FAILED          = config("update.failed");
    public static final I18n UPDATE_PASSWORD        = config("update.password");
    public static final I18n UPDATE_RECOMMENDATION  = config("update.recommendation");
    public static final I18n UPDATE_REPOSITORY      = config("update.repository");
    public static final I18n SYNC_INVENTORY_CHECK   = config("syncInventoryCheck");
    public static final I18n PRINT                  = config("print");
    public static final I18n SYNC_INVENTORY         = config("syncInventory");
    public static final I18n PRINTER_INVENTORY      = config("printerInventory");
    public static final I18n REMOVE_PRINT_INVENTORY = config("removePrintInventory");
    public static final I18n LAST                   = config("last");
    public static final I18n NEXT                   = config("next");
    public static final I18n DELETE                 = config("delete");
    // 配置列表选项 - 打印模式
    public static final I18n PRINT_MODE_PRINTER     = config("printMode.printer");
    public static final I18n PRINT_MODE_MINE        = config("printMode.mine");
    public static final I18n PRINT_MODE_FLUID       = config("printMode.fluid");
    public static final I18n PRINT_MODE_FILL        = config("printMode.fill");
    public static final I18n PRINT_MODE_REPLACE     = config("printMode.replace");
    public static final I18n PRINT_MODE_BEDROCK     = config("printMode.bedrock");
    // 配置列表选项 - 挖掘列表模式
    public static final I18n EXCAVATE_LIST_MODE_TWEAKEROO  = config("excavateListMode.tweakeroo");
    public static final I18n EXCAVATE_LIST_MODE_CUSTOM     = config("excavateListMode.custom");
    // 配置列表选项 - 运行模式（多模/单模）
    public static final I18n MODE_TYPE_MULTI        = config("modeType.multi");
    public static final I18n MODE_TYPE_SINGLE       = config("modeType.single");
    // 配置列表选项 - 迭代顺序
    public static final I18n ITERATION_ORDER_XYZ    = config("iterationOrder.xyz");
    public static final I18n ITERATION_ORDER_XZY    = config("iterationOrder.xzy");
    public static final I18n ITERATION_ORDER_YXZ    = config("iterationOrder.yxz");
    public static final I18n ITERATION_ORDER_YZX    = config("iterationOrder.yzx");
    public static final I18n ITERATION_ORDER_ZXY    = config("iterationOrder.zxy");
    // @formatter:on
    public static final I18n ITERATION_ORDER_ZYX = config("iterationOrder.zyx");
    // 原有静态前缀常量保留
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
        return new I18n(LitematicaPrinterMod.MOD_ID, key);
    }

    public static I18n config(String key) {
        return new I18n(LitematicaPrinterMod.MOD_ID, key);
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