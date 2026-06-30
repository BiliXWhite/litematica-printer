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
import me.aleksilassila.litematica.printer.gui.ConfigUi;
import me.aleksilassila.litematica.printer.utils.ModUtils;
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
    private static final BooleanSupplier isSingle = () -> Core.WORK_MODE.getOptionListValue().equals(WorkingModeType.SINGLE);
    private static final BooleanSupplier isMulti = () -> Core.WORK_MODE.getOptionListValue().equals(WorkingModeType.MULTI);

    private static final BooleanSupplier isBreakCustom = () -> Break.BREAK_LIMITER.getOptionListValue().equals(MiningFilterType.CUSTOM);
    private static final BooleanSupplier isBreakWhitelist = () -> isBreakCustom.getAsBoolean() && Break.BREAK_LIMIT.getOptionListValue().equals(UsageRestriction.ListType.WHITELIST);
    private static final BooleanSupplier isBreakBlacklist = () -> isBreakCustom.getAsBoolean() && Break.BREAK_LIMIT.getOptionListValue().equals(UsageRestriction.ListType.BLACKLIST);


    private static final BooleanSupplier isExcavateCustom = () -> Mine.EXCAVATE_LIMITER.getOptionListValue().equals(MiningFilterType.CUSTOM);
    private static final BooleanSupplier isExcavateWhitelist = () -> isExcavateCustom.getAsBoolean() && Mine.EXCAVATE_LIMIT.getOptionListValue().equals(UsageRestriction.ListType.WHITELIST);
    private static final BooleanSupplier isExcavateBlacklist = () -> isExcavateCustom.getAsBoolean() && Mine.EXCAVATE_LIMIT.getOptionListValue().equals(UsageRestriction.ListType.BLACKLIST);
    private static final BooleanSupplier isBlocklist = () -> Fill.FILL_BLOCK_MODE.getOptionListValue().equals(FillBlockModeType.BLOCKLIST);
    private static final BooleanSupplier isRemoteInventoryLoaded = ModUtils::isRemoteInventoryNextLoaded;


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
        optionSet.addAll(Highlight.OPTIONS);      // 高亮
        OPTIONS = ImmutableList.copyOf(optionSet);

        List<IHotkey> hotkeys = new ArrayList<>();
        for (IConfigBase option : optionSet) {
            if (option instanceof IHotkey hokey) {
                hotkeys.add(hokey);
            }
        }
        HOTKEYS = ImmutableList.copyOf(hotkeys);
    }

    public static ImmutableList<IConfigBase> All = ImmutableList.<IConfigBase>builder()
            .addAll(Core.OPTIONS)
            .addAll(Placement.OPTIONS)
            .addAll(Break.OPTIONS)
            .addAll(Hotkeys.OPTIONS)
            .addAll(Hotkeys.OPTIONS)
            .addAll(Print.OPTIONS)
            .addAll(Mine.OPTIONS)
            .addAll(Fill.OPTIONS)
            .addAll(Fluid.OPTIONS)
            .addAll(Highlight.OPTIONS)
            .build();

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
        public static final ConfigDouble WORK_RANGE = floatValue("workRange")
                .defaultValue(0)
                .range(0, 256)
                .build();

        // 核心 - 使用手长距离（已弃用，由工作半径=0自动使用最大交互距离替代）
        public static final ConfigBoolean USE_REACH_DISTANCE = booleanValue("useReachDistance")
                .defaultValue(true)
                .build();

        // 核心 - 迭代占用时长（毫秒）
        public static final ConfigInteger ITERATION_TIME_LIMIT = integerValue("iterationTimeLimit")
                .defaultValue(8)
                .range(0, 32)
                .build();

        // 核心 - 延迟检测
        public static final ConfigBoolean LAG_CHECK = booleanValue("printerLagCheck")
                .defaultValue(true)
                .build();

        public static final ConfigInteger LAG_CHECK_MAX = integerValue("printerLagCheckMax")
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
        public static final ConfigBoolean X_REVERSE = booleanValue("printerXAxisReverse")
                .defaultValue(false)
                .build();

        // 核心 - 迭代Y轴反向
        public static final ConfigBoolean Y_REVERSE = booleanValue("printerYAxisReverse")
                .defaultValue(false)
                .build();

        // 核心 - 迭代Z轴反向
        public static final ConfigBoolean Z_REVERSE = booleanValue("printerZAxisReverse")
                .defaultValue(false)
                .build();

        // 核心 - 显示打印机HUD
        public static final ConfigBoolean RENDER_HUD = booleanValue("renderHud")
                .defaultValue(false)
                .build();

        // 核心 - 显示缺失材料HUD
        public static final ConfigBoolean MISSING_MATERIAL_HUD = booleanValue("missingMaterialHud")
                .defaultValue(true)
                .build();

        // 核心 - 自动禁用打印机
        public static final ConfigBoolean AUTO_DISABLE_PRINTER = booleanValue("printerAutoDisable")
                .defaultValue(true)
                .build();

        // 核心 - 检查更新
        public static final ConfigBoolean UPDATE_CHECK = booleanValue("updateCheck")
                .defaultValue(true)
                .build();

        // 核心 - 调试输出
        public static final ConfigBoolean DEBUG_OUTPUT = booleanValue("debugOutput")
                .defaultValue(false)
                .build();

        // 惰性扫描 - 进入惰性的空闲 tick 数（0=禁用惰性）
        public static final ConfigInteger LAZY_ENTER_TICKS = integerValue("lazyEnterTicks")
                .defaultValue(100)
                .range(0, 1000)
                .build();

        // 惰性扫描 - 唤醒时触发的脏区域阈值（低于此值做 PARTIAL 重扫，否则 FULL 重扫）
        public static final ConfigInteger LAZY_DIRTY_WAKE_THRESHOLD = integerValue("lazyDirtyWakeThreshold")
                .defaultValue(5)
                .range(0, 100)
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
                MISSING_MATERIAL_HUD,
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
                LAZY_ENTER_TICKS,
                LAZY_DIRTY_WAKE_THRESHOLD
        );
    }

    public static class Placement {

        // 使用数据包打印
        public static final ConfigBoolean PRINT_USE_PACKET = booleanValue("placeUsePacket")
                .defaultValue(false)
                .build();

        // 核心 - 工作间隔
        public static final ConfigInteger PLACE_INTERVAL = integerValue("placeInterval")
                .defaultValue(1)
                .range(0, 20)
                .build();

        // 每刻放置方块数
        public static final ConfigInteger PLACE_BLOCKS_PER_TICK = integerValue("placeBlocksPerTick")
                .defaultValue(1)
                .range(0, 256)
                .build();

        // 放置冷却
        public static final ConfigInteger PLACE_COOLDOWN = integerValue("placeCooldown")
                .defaultValue(3)
                .range(0, 64)
                .build();

        // 下落方块检查
        public static final ConfigBoolean FALLING_CHECK = booleanValue("printFallingBlockCheck")
            .defaultValue(true)
            .build();

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                PRINT_USE_PACKET,
                PLACE_INTERVAL,
                PLACE_BLOCKS_PER_TICK,
                PLACE_COOLDOWN,
                FALLING_CHECK
        );
    }

    public static class Break {
        public static final ConfigBoolean BREAK_USE_PACKET = booleanValue("breakUsePacket")
                .defaultValue(false)
                .build();

        public static final ConfigInteger BREAK_PROGRESS_THRESHOLD = integerValue("breakProgressThreshold")
                .defaultValue(100)
                .range(70, 100)
                .build();

        public static final ConfigInteger BREAK_INTERVAL = integerValue("breakInterval")
                .defaultValue(1)
                .range(0, 20)
                .build();

        public static final ConfigInteger BREAK_BLOCKS_PER_TICK = integerValue("breakBlocksPerTick")
                .defaultValue(1)
                .range(0, 256)
                .build();

        public static final ConfigInteger BREAK_COOLDOWN = integerValue("breakCooldown")
                .defaultValue(3)
                .range(0, 64)
                .build();

        public static final ConfigBoolean BREAK_CHECK_HARDNESS = booleanValue("breakCheckHardness")
                .defaultValue(true)
                .build();

        // 即时挖掘
        public static final ConfigBoolean BREAK_INSTANT_MINE = booleanValue("breakInstantOnSameTick")
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
        public static final ConfigStringList BREAK_WHITELIST = stringListValue("breakWhitelist")
                .setVisible(isBreakWhitelist)
                .build();

        // 黑名单
        public static final ConfigStringList BREAK_BLACKLIST = stringListValue("breakBlacklist")
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
        public static final ConfigBoolean EASY_PLACE_PROTOCOL = booleanValue("easyPlaceProtocol")
                .defaultValue(false)
                .build();

        // 凭空放置
        public static final ConfigBoolean PLACE_IN_AIR = booleanValue("placeInAir")
                .defaultValue(true)
                .build();

        // 跳过含水方块
        public static final ConfigBoolean SKIP_WATERLOGGED_BLOCK = booleanValue("printSkipWaterlogged")
                .defaultValue(false)
                .build();

        // 跳过放置
        public static final ConfigBoolean PRINT_SKIP = booleanValue("printSkip")
                .defaultValue(false)
                .build();

        // 跳过放置名单
        public static final ConfigStringList PRINT_SKIP_LIST = stringListValue("printSkipList")
                .build();

        // 始终潜行
        public static final ConfigBoolean PRINT_FORCED_SNEAK = booleanValue("printForcedSneak")
                .defaultValue(false)
                .build();

        // 覆盖打印
        public static final ConfigBoolean PRINT_REPLACE = booleanValue("printReplace")
                .defaultValue(true)
                .build();

        // 覆盖方块列表
        public static final ConfigStringList REPLACEABLE_LIST = stringListValue("printReplaceableList")
                .defaultValue(Blocks.SNOW, Blocks.LAVA, Blocks.WATER, Blocks.BUBBLE_COLUMN, Blocks.SHORT_GRASS)
                .build();

        // 替换珊瑚
        public static final ConfigBoolean REPLACE_CORAL = booleanValue("printReplaceCoral")
                .defaultValue(false)
                .build();

        // 破冰放水
        public static final ConfigBooleanHotkeyed PRINT_ICE_FOR_WATER = booleanHotkey("printIceForWater")
                .defaultValue(false)
                .build();

        // 自动去皮
        public static final ConfigBoolean STRIP_LOGS = booleanValue("printAutoStripLogs")
                .defaultValue(false)
                .build();

        // 音符盒自动调音
        public static final ConfigBoolean NOTE_BLOCK_TUNING = booleanValue("printAutoTuning")
                .defaultValue(true)
                .build();

        // 侦测器安全放置
        public static final ConfigBoolean SAFELY_OBSERVER = booleanValue("printSafelyObserver")
                .defaultValue(true)
                .build();

        // 堆肥桶自动填充
        public static final ConfigBoolean FILL_COMPOSTER = booleanValue("printAutoFillComposter")
                .defaultValue(false)
                .build();

        // 堆肥桶白名单
        public static final ConfigStringList FILL_COMPOSTER_WHITELIST = stringListValue("printAutoFillComposterWhitelist")
                .setVisible(FILL_COMPOSTER::getBooleanValue)
                .build();

        // 农作物催熟
        public static final ConfigBoolean BONEMEAL_CROPS = booleanValue("printBonemealCrops")
                .defaultValue(false)
                .build();

        // 破坏错误方块
        public static final ConfigBoolean BREAK_WRONG_BLOCK = booleanValue("printBreakWrongBlock")
                .defaultValue(false)
                .build();

        // 破坏多余方块
        public static final ConfigBoolean BREAK_EXTRA_BLOCK = booleanValue("printBreakExtraBlock")
                .defaultValue(false)
                .build();

        // 破坏错误状态方块（实验性）
        public static final ConfigBoolean BREAK_WRONG_STATE_BLOCK = booleanValue("printBreakWrongStateBlock")
                .defaultValue(false)
                .build();

        // 使用远程容器材料
        public static final ConfigBooleanHotkeyed USE_REMOTE_CONTAINER = booleanHotkey("useRemoteContainer")
                .defaultValue(false)
                .setVisible(isRemoteInventoryLoaded)
                .build();

        // 远程交互最大距离（0 = 无限）
        public static final ConfigDouble REMOTE_INTERACTION_DISTANCE = floatValue("remoteInteractionDistance")
                .defaultValue(32.0)
                .range(0.0, 256.0)
                .useSlider(true)
                .setVisible(isRemoteInventoryLoaded)
                .build();

        // 远程容器方块列表
        public static final ConfigStringList REMOTE_CONTAINER_BLOCKS = stringListValue("remoteContainerBlocks")
                .defaultValue("minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel")
                .setVisible(isRemoteInventoryLoaded)
                .build();

        // 使用快捷潜影盒
        public static final ConfigBoolean USE_QUICK_SHULKER = booleanValue("useQuickShulker")
                .defaultValue(true)
                .build();

        // 潜影盒来源
        public static final ConfigOptionList SHULKER_SOURCE = optionList("shulkerSource")
                .defaultValue(ShulkerSource.MOD)
                .build();

        // 潜影盒冷却
        public static final ConfigInteger SHULKER_COOLDOWN = integerValue("shulkerCooldown")
                .defaultValue(5)
                .range(0, 20)
                .build();

        // 背包满时有序放回潜影盒
        public static final ConfigBoolean RETURN_TO_SHULKER_WHEN_FULL = booleanValue("returnToShulkerWhenFull")
                .defaultValue(true)
                .build();

        // 背包满时有序放回远程容器
        public static final ConfigBoolean RETURN_TO_CONTAINER_WHEN_FULL = booleanValue("returnToContainerWhenFull")
                .defaultValue(true)
                .setVisible(isRemoteInventoryLoaded)
                .build();

        // 远程容器回塞节流（tick）
        public static final ConfigInteger CONTAINER_RETURN_INTERVAL = integerValue("containerReturnInterval")
                .defaultValue(60)
                .range(1, 200)
                .setVisible(isRemoteInventoryLoaded)
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
                BONEMEAL_CROPS,
                USE_REMOTE_CONTAINER,
                REMOTE_CONTAINER_BLOCKS,
                USE_QUICK_SHULKER,
                SHULKER_SOURCE,
                SHULKER_COOLDOWN,
                RETURN_TO_SHULKER_WHEN_FULL,
                RETURN_TO_CONTAINER_WHEN_FULL,
                CONTAINER_RETURN_INTERVAL
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
        public static final ConfigStringList EXCAVATE_WHITELIST = stringListValue("excavateWhitelist")
                .setVisible(isExcavateWhitelist)
                .build();

        // 挖掘黑名单
        public static final ConfigStringList EXCAVATE_BLACKLIST = stringListValue("excavateBlacklist")
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
        public static final ConfigStringList FILL_BLOCK_LIST = stringListValue("fillBlockList")
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
        public static final ConfigBoolean FILL_FLOWING_FLUID = booleanValue("fluidModeFillFlowing")
                .defaultValue(true)
                .build();

        // 方块名单
        public static final ConfigStringList FLUID_REPLACE_BLOCK_LIST = stringListValue("fluidReplaceBlockList")
                .defaultValue(Blocks.SAND)
                .build();

        // 液体名单
        public static final ConfigStringList FLUID_LIST = stringListValue("fluidList")
                .defaultValue(Blocks.WATER, Blocks.LAVA)
                .build();

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                FLUID_SELECTION_TYPE,         // 排流体 - 选区类型
                FILL_FLOWING_FLUID,           // 排流体 - 填充流动液体
                FLUID_REPLACE_BLOCK_LIST,             // 排流体 - 方块名单
                FLUID_LIST                    // 排流体 - 液体名单
        );
    }

    public static class Highlight {
        // 启用方块高亮
        public static final ConfigBoolean HIGHLIGHT_ENABLED = booleanValue("highlightEnabled")
                .defaultValue(false)
                .build();

        // 成功放置颜色
        public static final ConfigColor HIGHLIGHT_COLOR_PLACE = color("highlightColorPlace")
                .defaultValue("#80FFFFFF")
                .build();

        // 调整颜色（填充/流体）
        public static final ConfigColor HIGHLIGHT_COLOR_ADJUST = color("highlightColorAdjust")
                .defaultValue("#8000FF00")
                .build();

        // 破坏方块颜色
        public static final ConfigColor HIGHLIGHT_COLOR_BREAK = color("highlightColorBreak")
                .defaultValue("#80FF0000")
                .build();

        // 放置失败颜色
        public static final ConfigColor HIGHLIGHT_COLOR_FAILED = color("highlightColorFailed")
                .defaultValue("#80808080")
                .build();

        // 高亮样式
        public static final ConfigOptionList HIGHLIGHT_STYLE = optionList("highlightStyle")
                .defaultValue(HighlightStyleType.OUTLINE)
                .build();

        // 完成后渐隐时长（单位0.1秒，10=1秒）
        public static final ConfigInteger HIGHLIGHT_FADE_DURATION = integerValue("highlightFadeDuration")
                .defaultValue(5)
                .range(1, 100)
                .useSlider(true)
                .build();

        // 透视模式（透过方块查看）
        public static final ConfigBoolean HIGHLIGHT_THROUGH_WALLS = booleanValue("highlightThroughWalls")
                .defaultValue(false)
                .build();

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                HIGHLIGHT_ENABLED,
                HIGHLIGHT_COLOR_PLACE,
                HIGHLIGHT_COLOR_ADJUST,
                HIGHLIGHT_COLOR_BREAK,
                HIGHLIGHT_COLOR_FAILED,
                HIGHLIGHT_STYLE,
                HIGHLIGHT_FADE_DURATION,
                HIGHLIGHT_THROUGH_WALLS
        );
    }

    public static class Hotkeys {
        // 打开设置菜单
        public static final ConfigHotkey OPEN_SCREEN = hotkeyValue("openScreen")
                .defaultStorageString("Z,Y")
                .build();

        // 关闭全部模式
        public static final ConfigHotkey CLOSE_ALL_MODE = hotkeyValue("closeAllMode")
                .defaultStorageString("LEFT_CONTROL,G")
                .build();

        // 切换模式
        public static final ConfigHotkey SWITCH_PRINTER_MODE = hotkeyValue("switchPrinterMode")
                .bindConfig(Core.WORK_MODE_TYPE)
                .setVisible(isSingle) // 仅单模式时显示
                .build();

        // 破基岩
        public static final ConfigBooleanHotkeyed BEDROCK = booleanHotkey("bedrock")
                .defaultValue(false)
                .setVisible(isMulti) // 仅多模式时显示
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
                BEDROCK                       // 破基岩
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