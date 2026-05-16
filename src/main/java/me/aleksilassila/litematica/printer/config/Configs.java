package me.aleksilassila.litematica.printer.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.*;
import fi.dy.masa.malilib.config.options.*;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import fi.dy.masa.malilib.config.ConfigManager;
import me.aleksilassila.litematica.printer.Reference;
import me.aleksilassila.litematica.printer.enums.*;
import me.aleksilassila.litematica.printer.utils.ModUtils;
import me.aleksilassila.litematica.printer.gui.ConfigUi;
import net.minecraft.world.level.block.Blocks;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BooleanSupplier;

//#if MC >= 12111
import fi.dy.masa.malilib.util.data.json.JsonUtils;
//#else
//$$ import fi.dy.masa.malilib.util.JsonUtils;
//#endif
public class Configs extends ConfigBuilders implements IConfigHandler {
    private static final Configs INSTANCE = new Configs();

    private static final String FILE_PATH = "./config/" + Reference.MOD_ID + ".json";
    private static final File CONFIG_DIR = new File("./config");

    private static final KeybindSettings GUI_NO_ORDER = KeybindSettings.create(KeybindSettings.Context.GUI, KeyAction.PRESS, false, false, false, true);

    // 配置页面是否可视(函数式, 动态获取, 全局统一使用)
    private static final BooleanSupplier isLoadChestTrackerLoaded = ModUtils::isChestTrackerLoaded;
    private static final BooleanSupplier isSingle = () -> Core.WORK_MODE.getOptionListValue().equals(WorkingModeType.SINGLE);
    private static final BooleanSupplier isMulti = () -> Core.WORK_MODE.getOptionListValue().equals(WorkingModeType.MULTI);

    private static final BooleanSupplier isBreakCustom = () -> Break.BREAK_LIMITER.getOptionListValue().equals(MiningFilterType.CUSTOM);
    private static final BooleanSupplier isBreakWhitelist = () -> isBreakCustom.getAsBoolean() && Break.BREAK_LIMIT.getOptionListValue().equals(UsageRestriction.ListType.WHITELIST);
    private static final BooleanSupplier isBreakBlacklist = () -> isBreakCustom.getAsBoolean() && Break.BREAK_LIMIT.getOptionListValue().equals(UsageRestriction.ListType.BLACKLIST);


    private static final BooleanSupplier isExcavateCustom = () -> Mine.EXCAVATE_LIMITER.getOptionListValue().equals(MiningFilterType.CUSTOM);
    private static final BooleanSupplier isExcavateWhitelist = () -> isExcavateCustom.getAsBoolean() && Mine.EXCAVATE_LIMIT.getOptionListValue().equals(UsageRestriction.ListType.WHITELIST);
    private static final BooleanSupplier isExcavateBlacklist = () -> isExcavateCustom.getAsBoolean() && Mine.EXCAVATE_LIMIT.getOptionListValue().equals(UsageRestriction.ListType.BLACKLIST);
    private static final BooleanSupplier isBlocklist = () -> Fill.FILL_BLOCK_MODE.getOptionListValue().equals(FillBlockModeType.BLOCKLIST);


    public static final ImmutableList<IConfigBase> OPTIONS;
    public static final ImmutableList<IHotkey> HOTKEYS;


    static {
        LinkedHashSet<IConfigBase> optionSet = new LinkedHashSet<>();
        optionSet.addAll(Core.OPTIONS);           // 核心
        optionSet.addAll(Placement.OPTIONS);      // 放置
        optionSet.addAll(Break.OPTIONS);          // 破坏
        optionSet.addAll(Hotkeys.OPTIONS);        // 热键
        optionSet.addAll(Print.OPTIONS);          // 打印
        optionSet.addAll(Mine.OPTIONS);           // 挖掘
        optionSet.addAll(Fill.OPTIONS);           // 填充
        optionSet.addAll(Fluid.OPTIONS);          // 排流体
        OPTIONS = ImmutableList.copyOf(optionSet);

        List<IHotkey> hotkeys = new ArrayList<>();
        for (IConfigBase option : optionSet) {
            if (option instanceof IHotkey hokey) {
                hotkeys.add(hokey);
            }
        }
        HOTKEYS = ImmutableList.copyOf(hotkeys);
    }

    public static class Core {
        // 打印状态
        public static final ConfigBooleanHotkeyed WORK_SWITCH = booleanHotkey("workingSwitch")
                .defaultValue(false)
                .defaultHotkey("CAPS_LOCK")
                .keybindSettings(KeybindSettings.PRESS_ALLOWEXTRA_EMPTY)
                .build();

        // 核心 - 模式切换
        public static final ConfigOptionList WORK_MODE = optionList("modeSwitch")
                .defaultValue(WorkingModeType.SINGLE)
                .build();

        // 多模 - 打印
        public static final ConfigBooleanHotkeyed PRINT = booleanHotkey("print")
                .defaultValue(false)
                .setVisible(isMulti) // 仅多模式时显示
                .build();

        // 多模 - 挖掘
        public static final ConfigBooleanHotkeyed MINE = booleanHotkey("mine")
                .defaultValue(false)
                .setVisible(isMulti) // 仅多模式时显示
                .build();

        // 多模 - 填充
        public static final ConfigBooleanHotkeyed FILL = booleanHotkey("fill")
                .defaultValue(false)
                .setVisible(isMulti) // 仅多模式时显示
                .build();

        // 多模 - 排流体
        public static final ConfigBooleanHotkeyed FLUID = booleanHotkey("fluid")
                .defaultValue(false)
                .setVisible(isMulti) // 仅多模式时显示
                .build();

        // 核心 - 单模模式
        public static final ConfigOptionList WORK_MODE_TYPE = optionList("printerMode")
                .defaultValue(PrintModeType.PRINTER)
                .setVisible(isSingle) // 仅单模式时显示
                .build();

        // 核心 - 工作半径（0 = 自动使用最大可用交互距离）
        public static final ConfigInteger WORK_RANGE = integer("workRange")
                .defaultValue(0)
                .range(0, 256)
                .build();

        // 核心 - 使用手长距离（已弃用，由工作半径=0自动使用最大交互距离替代）
        public static final ConfigBoolean USE_REACH_DISTANCE = bool("useReachDistance")
                .defaultValue(true)
                .build();

        // 核心 - 迭代占用时长（毫秒）
        public static final ConfigInteger ITERATION_TIME_LIMIT = integer("iterationTimeLimit")
                .defaultValue(8)
                .range(0, 32)
                .build();

        // 核心 - 延迟检测
        public static final ConfigBoolean LAG_CHECK = bool("printerLagCheck")
                .defaultValue(true)
                .build();

        public static final ConfigInteger LAG_CHECK_MAX = integer("printerLagCheckMax")
                .defaultValue(20)
                .setVisible(LAG_CHECK::getBooleanValue)
                .range(20, 1200)
                .build();

        // 核心 - 迭代区域形状
        public static final ConfigOptionList ITERATOR_SHAPE = optionList("printerIteratorShape")
                .defaultValue(RadiusShapeType.SPHERE)
                .build();

        // 核心 - 遍历顺序
        public static final ConfigOptionList ITERATION_ORDER = optionList("printerIteratorMode")
                .defaultValue(IterationOrderType.XZY)
                .build();

        // 核心 - 迭代X轴反向
        public static final ConfigBoolean X_REVERSE = bool("printerXAxisReverse")
                .defaultValue(false)
                .build();

        // 核心 - 迭代Y轴反向
        public static final ConfigBoolean Y_REVERSE = bool("printerYAxisReverse")
                .defaultValue(false)
                .build();

        // 核心 - 迭代Z轴反向
        public static final ConfigBoolean Z_REVERSE = bool("printerZAxisReverse")
                .defaultValue(false)
                .build();

        // 核心 - 显示打印机HUD
        public static final ConfigBoolean RENDER_HUD = bool("renderHud")
                .defaultValue(false)
                .build();

        // 核心 - 自动禁用打印机
        public static final ConfigBoolean AUTO_DISABLE_PRINTER = bool("printerAutoDisable")
                .defaultValue(true)
                .build();

        // 核心 - 检查更新
        public static final ConfigBoolean UPDATE_CHECK = bool("updateCheck")
                .defaultValue(true)
                .build();

        // 核心 - 调试输出
        public static final ConfigBoolean DEBUG_OUTPUT = bool("debugOutput")
                .defaultValue(false)
                .build();

        public static final ConfigBoolean SELECTION_MATERIALS = bool("selectionMaterials")
                .defaultValue(false)
                .setVisible(isLoadChestTrackerLoaded)
                .build();

        // 远程交互 - 开关
        public static final ConfigBoolean CLOUD_INVENTORY = bool("cloudInventory")
                .defaultValue(false)
                .setVisible(isLoadChestTrackerLoaded) // 仅箱子追踪 Mod 加载时显示
                .build();

        // 远程交互 - 自动设置远程交互
        public static final ConfigBoolean AUTO_INVENTORY = bool("autoInventory")
                .defaultValue(false)
                .setVisible(isLoadChestTrackerLoaded) // 仅箱子追踪 Mod 加载时显示
                .build();

        // 远程交互 - 库存白名单
        public static final ConfigStringList INVENTORY_LIST = stringList("inventoryList")
                .defaultValue(Blocks.CHEST)
                .setVisible(isLoadChestTrackerLoaded) // 仅箱子追踪 Mod 加载时显示
                .build();

        // 容器同步与打印机添加库存高亮颜色
        public static final ConfigColor SYNC_INVENTORY_COLOR = color("syncInventoryColor")
                .defaultValue("#4CFF4CE6")
                .build();

        // 通用配置项列表（按功能分类排序）
        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                WORK_SWITCH,
                WORK_MODE,
                WORK_MODE_TYPE,
                PRINT,
                MINE,
                FILL,
                FLUID,
                WORK_RANGE,
                USE_REACH_DISTANCE,
                ITERATION_TIME_LIMIT,
                RENDER_HUD,
                LAG_CHECK,
                LAG_CHECK_MAX,
                ITERATOR_SHAPE,
                ITERATION_ORDER,
                X_REVERSE,
                Y_REVERSE,
                Z_REVERSE,
                AUTO_DISABLE_PRINTER,
                UPDATE_CHECK,
                DEBUG_OUTPUT,
                SELECTION_MATERIALS,
                CLOUD_INVENTORY,
                AUTO_INVENTORY,
                INVENTORY_LIST,
                SYNC_INVENTORY_COLOR
        );
    }

    public static class Placement {

        // 使用数据包打印
        public static final ConfigBoolean PRINT_USE_PACKET = bool("placeUsePacket")
                .defaultValue(false)
                .build();

        // 核心 - 工作间隔
        public static final ConfigInteger PLACE_INTERVAL = integer("placeInterval")
                .defaultValue(1)
                .range(0, 20)
                .build();

        // 每刻放置方块数
        public static final ConfigInteger PLACE_BLOCKS_PER_TICK = integer("placeBlocksPerTick")
                .defaultValue(1)
                .range(0, 256)
                .build();

        // 放置冷却
        public static final ConfigInteger PLACE_COOLDOWN = integer("placeCooldown")
                .defaultValue(3)
                .range(0, 64)
                .build();

        // 下落方块检查
        public static final ConfigBoolean FALLING_CHECK = bool("printFallingBlockCheck")
            .defaultValue(true)
            .build();

        // 快捷潜影盒 - 开关
        public static final ConfigBoolean QUICK_SHULKER = bool("quickShulker")
                .defaultValue(false)
                .build();

        // 快捷潜影盒 - 工作模式
        public static final ConfigOptionList QUICK_SHULKER_MODE = optionList("quickShulkerMode")
                .defaultValue(QuickShulkerModeType.INVOKE)
                .build();

        // 快捷潜影盒 - 冷却时间
        public static final ConfigInteger QUICK_SHULKER_COOLDOWN = integer("quickShulkerCooldown")
                .defaultValue(10)
                .range(0, 20)
                .build();

        // 储存管理 - 有序存放
        public static final ConfigBoolean STORE_ORDERLY = bool("storeOrderly")
                .defaultValue(false)
                .build();

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                PRINT_USE_PACKET,
                PLACE_INTERVAL,
                PLACE_BLOCKS_PER_TICK,
                PLACE_COOLDOWN,
                FALLING_CHECK,
                STORE_ORDERLY,
                QUICK_SHULKER,
                QUICK_SHULKER_MODE,
                QUICK_SHULKER_COOLDOWN
        );
    }

    public static class Break {
        public static final ConfigBoolean BREAK_USE_PACKET = bool("breakUsePacket")
                .defaultValue(false)
                .build();

        public static final ConfigInteger BREAK_PROGRESS_THRESHOLD = integer("breakProgressThreshold")
                .defaultValue(100)
                .range(70, 100)
                .build();

        public static final ConfigInteger BREAK_INTERVAL = integer("breakInterval")
                .defaultValue(1)
                .range(0, 20)
                .build();

        public static final ConfigInteger BREAK_BLOCKS_PER_TICK = integer("breakBlocksPerTick")
                .defaultValue(1)
                .range(0, 256)
                .build();

        public static final ConfigInteger BREAK_COOLDOWN = integer("breakCooldown")
                .defaultValue(3)
                .range(0, 64)
                .build();

        public static final ConfigBoolean BREAK_CHECK_HARDNESS = bool("breakCheckHardness")
                .defaultValue(true)
                .build();

        // 即时挖掘
        public static final ConfigBoolean BREAK_INSTANT_MINE = bool("breakInstantOnSameTick")
                .defaultValue(false)
                .build();

        // 模式限制器
        public static final ConfigOptionList BREAK_LIMITER = optionList("breakLimiter")
                .defaultValue(MiningFilterType.CUSTOM)
                .build();

        // 模式限制
        public static final ConfigOptionList BREAK_LIMIT = optionList("breakLimit")
                .defaultValue(UsageRestriction.ListType.NONE)
                .setVisible(isBreakCustom)
                .build();

        // 白名单
        public static final ConfigStringList BREAK_WHITELIST = stringList("breakWhitelist")
                .setVisible(isBreakWhitelist)
                .build();

        // 黑名单
        public static final ConfigStringList BREAK_BLACKLIST = stringList("breakBlacklist")
                .setVisible(isBreakBlacklist)
                .build();

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                BREAK_CHECK_HARDNESS,
                BREAK_INSTANT_MINE,
                BREAK_USE_PACKET,
                BREAK_INTERVAL,
                BREAK_BLOCKS_PER_TICK,
                BREAK_COOLDOWN,
                BREAK_PROGRESS_THRESHOLD,
                // 限制器
                BREAK_LIMITER,
                BREAK_LIMIT,
                BREAK_WHITELIST,
                BREAK_BLACKLIST
        );
    }

    public static class Print {
        // 选区类型
        public static final ConfigOptionList PRINT_SELECTION_TYPE = optionList("printSelectionType")
                .defaultValue(SelectionType.LITEMATICA_RENDER_LAYER)
                .build();

        // 投影轻松放置协议
        public static final ConfigBoolean EASY_PLACE_PROTOCOL = bool("easyPlaceProtocol")
                .defaultValue(false)
                .build();

        // 凭空放置
        public static final ConfigBoolean PLACE_IN_AIR = bool("placeInAir")
                .defaultValue(true)
                .build();

        // 跳过含水方块
        public static final ConfigBoolean SKIP_WATERLOGGED_BLOCK = bool("printSkipWaterlogged")
                .defaultValue(false)
                .build();

        // 跳过放置
        public static final ConfigBoolean PRINT_SKIP = bool("printSkip")
                .defaultValue(false)
                .build();

        // 跳过放置名单
        public static final ConfigStringList PRINT_SKIP_LIST = stringList("printSkipList")
                .build();

        // 始终潜行
        public static final ConfigBoolean PRINT_FORCED_SNEAK = bool("printForcedSneak")
                .defaultValue(false)
                .build();

        // 覆盖打印
        public static final ConfigBoolean PRINT_REPLACE = bool("printReplace")
                .defaultValue(true)
                .build();

        // 覆盖方块列表
        public static final ConfigStringList REPLACEABLE_LIST = stringList("printReplaceableList")
                .defaultValue(Blocks.SNOW, Blocks.LAVA, Blocks.WATER, Blocks.BUBBLE_COLUMN, Blocks.SHORT_GRASS)
                .build();

        // 替换珊瑚
        public static final ConfigBoolean REPLACE_CORAL = bool("printReplaceCoral")
                .defaultValue(false)
                .build();

        // 破冰放水
        public static final ConfigBooleanHotkeyed PRINT_ICE_FOR_WATER = booleanHotkey("printIceForWater")
                .defaultValue(false)
                .build();

        // 自动去皮
        public static final ConfigBoolean STRIP_LOGS = bool("printAutoStripLogs")
                .defaultValue(false)
                .build();

        // 音符盒自动调音
        public static final ConfigBoolean NOTE_BLOCK_TUNING = bool("printAutoTuning")
                .defaultValue(true)
                .build();

        // 侦测器安全放置
        public static final ConfigBoolean SAFELY_OBSERVER = bool("printSafelyObserver")
                .defaultValue(true)
                .build();

        // 堆肥桶自动填充
        public static final ConfigBoolean FILL_COMPOSTER = bool("printAutoFillComposter")
                .defaultValue(false)
                .build();

        // 堆肥桶白名单
        public static final ConfigStringList FILL_COMPOSTER_WHITELIST = stringList("printAutoFillComposterWhitelist")
                .setVisible(FILL_COMPOSTER::getBooleanValue)
                .build();

        // 农作物催熟
        public static final ConfigBoolean BONEMEAL_CROPS = bool("printBonemealCrops")
                .defaultValue(false)
                .build();

        // 破坏错误方块
        public static final ConfigBoolean BREAK_WRONG_BLOCK = bool("printBreakWrongBlock")
                .defaultValue(false)
                .build();

        // 破坏多余方块
        public static final ConfigBoolean BREAK_EXTRA_BLOCK = bool("printBreakExtraBlock")
                .defaultValue(false)
                .build();

        // 破坏错误状态方块（实验性）
        public static final ConfigBoolean BREAK_WRONG_STATE_BLOCK = bool("printBreakWrongStateBlock")
                .defaultValue(false)
                .build();

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                PRINT_SELECTION_TYPE,
                EASY_PLACE_PROTOCOL,
                PLACE_IN_AIR,
                PRINT_FORCED_SNEAK,
                BREAK_WRONG_BLOCK,
                BREAK_EXTRA_BLOCK,
                BREAK_WRONG_STATE_BLOCK,
                PRINT_SKIP,
                PRINT_SKIP_LIST,
                PRINT_REPLACE,
                REPLACEABLE_LIST,
                SKIP_WATERLOGGED_BLOCK,
                PRINT_ICE_FOR_WATER,
                SAFELY_OBSERVER,
                STRIP_LOGS,
                NOTE_BLOCK_TUNING,
                REPLACE_CORAL,
                FILL_COMPOSTER,
                FILL_COMPOSTER_WHITELIST,
                BONEMEAL_CROPS
        );
    }

    public static class Mine {
        // 选区类型
        public static final ConfigOptionList MINE_SELECTION_TYPE = optionList("mineSelectionType")
                .defaultValue(SelectionType.LITEMATICA_SELECTION)
                .build();

        // 挖掘模式限制器
        public static final ConfigOptionList EXCAVATE_LIMITER = optionList("excavateLimiter")
                .defaultValue(MiningFilterType.CUSTOM)
                .build();

        // 挖掘模式限制
        public static final ConfigOptionList EXCAVATE_LIMIT = optionList("excavateLimit")
                .defaultValue(UsageRestriction.ListType.NONE)
                .setVisible(isExcavateCustom)
                .build();

        // 挖掘白名单
        public static final ConfigStringList EXCAVATE_WHITELIST = stringList("excavateWhitelist")
                .setVisible(isExcavateWhitelist)
                .build();

        // 挖掘黑名单
        public static final ConfigStringList EXCAVATE_BLACKLIST = stringList("excavateBlacklist")
                .setVisible(isExcavateBlacklist)
                .build();

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                MINE_SELECTION_TYPE,          // 挖掘 - 选区类型
                EXCAVATE_LIMITER,             // 挖掘 - 挖掘模式限制器
                EXCAVATE_LIMIT,               // 挖掘 - 挖掘模式限制
                EXCAVATE_WHITELIST,           // 挖掘 - 挖掘白名单
                EXCAVATE_BLACKLIST            // 挖掘 - 挖掘黑名单
        );
    }

    public static class Fill {
        // 选区类型
        public static final ConfigOptionList FILL_SELECTION_TYPE = optionList("fillSelectionType")
                .defaultValue(SelectionType.LITEMATICA_SELECTION)
                .build();

        // 填充方块模式
        public static final ConfigOptionList FILL_BLOCK_MODE = optionList("fillBlockMode")
                .defaultValue(FillBlockModeType.BLOCKLIST)
                .build();

        // 填充方块名单
        public static final ConfigStringList FILL_BLOCK_LIST = stringList("fillBlockList")
                .defaultValue(Blocks.COBBLESTONE)
                .setVisible(isBlocklist)
                .build();

        // 模式朝向
        public static final ConfigOptionList FILL_BLOCK_FACING = optionList("fillModeFacing")
                .defaultValue(FillModeFacingType.NONE)
                .build();

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                FILL_SELECTION_TYPE,          // 填充 - 选区类型
                FILL_BLOCK_MODE,              // 填充 - 填充方块模式
                FILL_BLOCK_LIST,              // 填充 - 填充方块名单
                FILL_BLOCK_FACING             // 填充 - 模式朝向
        );
    }

    public static class Fluid {

        // 选区类型
        public static final ConfigOptionList FLUID_SELECTION_TYPE = optionList("fluidSelectionType")
                .defaultValue(SelectionType.LITEMATICA_SELECTION)
                .build();

        // 填充流动液体
        public static final ConfigBoolean FILL_FLOWING_FLUID = bool("fluidModeFillFlowing")
                .defaultValue(true)
                .build();

        // 方块名单
        public static final ConfigStringList FLUID_REPLACE_BLOCK_LIST = stringList("fluidReplaceBlockList")
                .defaultValue(Blocks.SAND)
                .build();

        // 液体名单
        public static final ConfigStringList FLUID_LIST = stringList("fluidList")
                .defaultValue(Blocks.WATER, Blocks.LAVA)
                .build();

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                FLUID_SELECTION_TYPE,         // 排流体 - 选区类型
                FILL_FLOWING_FLUID,           // 排流体 - 填充流动液体
                FLUID_REPLACE_BLOCK_LIST,             // 排流体 - 方块名单
                FLUID_LIST                    // 排流体 - 液体名单
        );
    }

    public static class Hotkeys {
        // 打开设置菜单
        public static final ConfigHotkey OPEN_SCREEN = hotkey("openScreen")
                .defaultStorageString("Z,Y")
                .build();

        // 关闭全部模式
        public static final ConfigHotkey CLOSE_ALL_MODE = hotkey("closeAllMode")
                .defaultStorageString("LEFT_CONTROL,G")
                .build();

        // 切换模式
        public static final ConfigHotkey SWITCH_PRINTER_MODE = hotkey("switchPrinterMode")
                .bindConfig(Core.WORK_MODE_TYPE)
                .setVisible(isSingle) // 仅单模式时显示
                .build();

        // 破基岩
        public static final ConfigBooleanHotkeyed BEDROCK = booleanHotkey("bedrock")
                .defaultValue(false)
                .setVisible(isMulti) // 仅多模式时显示
                .build();

        // 同步容器热键
        public static final ConfigHotkey SYNC_INVENTORY = hotkey("syncInventory")
                .build();

        // 同步容器开关热键
        public static final ConfigBooleanHotkeyed SYNC_INVENTORY_CHECK = booleanHotkey("syncInventoryCheck")
                .defaultValue(false)
                .build();

        // ========== 远程交互热键 ==========

        // 设置打印机库存热键
        public static final ConfigHotkey PRINTER_INVENTORY = hotkey("printerInventory")
                .setVisible(isLoadChestTrackerLoaded) // 仅箱子追踪 Mod 加载时显示
                .build();

        // 清空打印机库存热键
        public static final ConfigHotkey REMOVE_PRINT_INVENTORY = hotkey("removePrintInventory")
                .setVisible(isLoadChestTrackerLoaded) // 仅箱子追踪 Mod 加载时显示
                .build();

        // 上一个箱子
        public static final ConfigHotkey LAST = hotkey("last")
                .keybindSettings(GUI_NO_ORDER)
                .setVisible(isLoadChestTrackerLoaded) // 仅箱子追踪 Mod 加载时显示
                .build();

        // 下一个箱子
        public static final ConfigHotkey NEXT = hotkey("next")
                .keybindSettings(GUI_NO_ORDER)
                .setVisible(isLoadChestTrackerLoaded) // 仅箱子追踪 Mod 加载时显示
                .build();

        // 删除当前容器
        public static final ConfigHotkey DELETE = hotkey("delete")
                .keybindSettings(GUI_NO_ORDER)
                .setVisible(isLoadChestTrackerLoaded) // 仅箱子追踪 Mod 加载时显示
                .build();

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                OPEN_SCREEN,                  // 打开设置菜单
                Core.WORK_SWITCH,
                CLOSE_ALL_MODE,               // 关闭全部模式
                SWITCH_PRINTER_MODE,          // 切换模式

                // 多模
                Core.PRINT,
                Core.MINE,                // 挖掘
                Core.FILL,                    // 填充
                Core.FLUID,                  // 排流体
                BEDROCK,                      // 破基岩

                // 远程交互
                SYNC_INVENTORY,               // 同步容器热键
                SYNC_INVENTORY_CHECK,         // 同步容器开关热键
                PRINTER_INVENTORY,            // 设置打印机库存热键
                REMOVE_PRINT_INVENTORY,       // 清空打印机库存热键
                LAST,                         // 上一个箱子
                NEXT,                         // 下一个箱子
                DELETE                        // 删除当前容器
        );
    }

    @Override
    public void load() {
        File settingFile = new File(FILE_PATH);
        if (settingFile.isFile() && settingFile.exists()) {
            //#if MC >= 12111
            JsonElement jsonElement = JsonUtils.parseJsonFile(settingFile.toPath());
            //#else
            //$$ JsonElement jsonElement = JsonUtils.parseJsonFile(settingFile);
            //#endif
            if (jsonElement != null && jsonElement.isJsonObject()) {
                JsonObject obj = jsonElement.getAsJsonObject();
                ConfigUtils.readConfigBase(obj, Reference.MOD_ID, OPTIONS);
            }
        }
    }

    @Override
    public void save() {
        if ((CONFIG_DIR.exists() && CONFIG_DIR.isDirectory()) || CONFIG_DIR.mkdirs()) {
            JsonObject configRoot = new JsonObject();
            ConfigUtils.writeConfigBase(configRoot, Reference.MOD_ID, OPTIONS);
            //#if MC >= 12111
            JsonUtils.writeJsonToFile(configRoot, new File(FILE_PATH).toPath());
            //#else
            //$$ JsonUtils.writeJsonToFile(configRoot, new File(FILE_PATH));
            //#endif
        }
    }

    public static void init() {
        Configs.INSTANCE.load();
        ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, Configs.INSTANCE);
        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerKeyboardInputHandler(InputHandler.getInstance());
        //#if MC > 12006
        fi.dy.masa.malilib.registry.Registry.CONFIG_SCREEN.registerConfigScreenFactory(
                new fi.dy.masa.malilib.util.data.ModInfo(Reference.MOD_ID, Reference.MOD_NAME, ConfigUi::new)
        );
        //#endif
    }
}