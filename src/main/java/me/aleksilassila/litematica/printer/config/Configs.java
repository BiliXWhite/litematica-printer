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
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import fi.dy.masa.malilib.config.ConfigManager;
import me.aleksilassila.litematica.printer.Reference;
import me.aleksilassila.litematica.printer.config.enums.*;
import me.aleksilassila.litematica.printer.bilixwhite.ModLoadStatus;
import me.aleksilassila.litematica.printer.gui.ConfigUi;
import net.minecraft.world.level.block.Blocks;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BooleanSupplier;

public class Configs extends ConfigBuilders implements IConfigHandler {
    private static final Configs INSTANCE = new Configs();

    private static final String FILE_PATH = "./config/" + Reference.MOD_ID + ".json";
    private static final File CONFIG_DIR = new File("./config");

    private static final KeybindSettings BOTH_ALLOW_EXTRA_EMPTY = KeybindSettings.create(KeybindSettings.Context.INGAME, KeyAction.BOTH, true, true, false, true, true);
    private static final KeybindSettings GUI_NO_ORDER = KeybindSettings.create(KeybindSettings.Context.GUI, KeyAction.PRESS, false, false, false, true);

    // 配置页面是否可视(函数式, 动态获取, 全局统一使用)
    private static final BooleanSupplier isLoadChestTrackerLoaded = ModLoadStatus::isLoadChestTrackerLoaded;
    private static final BooleanSupplier isLoadQuickShulkerLoaded = ModLoadStatus::isLoadQuickShulkerLoaded;
    private static final BooleanSupplier isSingle = () -> General.WORK_MODE.getOptionListValue().equals(ModeType.SINGLE);
    private static final BooleanSupplier isMulti = () -> General.WORK_MODE.getOptionListValue().equals(ModeType.MULTI);
    private static final BooleanSupplier isExcavateCustom = () -> Excavate.EXCAVATE_LIMITER.getOptionListValue().equals(ExcavateListMode.CUSTOM);

    public static final ImmutableList<IConfigBase> OPTIONS;
    public static final ImmutableList<IHotkey> HOTKEYS;


    static {
        LinkedHashSet<IConfigBase> optionSet = new LinkedHashSet<>();
        optionSet.addAll(General.OPTIONS);        // 通用
        optionSet.addAll(Placement.OPTIONS);      // 放置
        optionSet.addAll(Break.OPTIONS);          // 破坏
        optionSet.addAll(Color.OPTIONS);          // 颜色
        optionSet.addAll(Hotkeys.OPTIONS);        // 热键
        optionSet.addAll(Print.OPTIONS);          // 打印（原注释笔误，建议修正）
        optionSet.addAll(Excavate.OPTIONS);       // 挖掘
        optionSet.addAll(Fill.OPTIONS);           // 填充
        optionSet.addAll(FLUID.OPTIONS);          // 排流体
        OPTIONS = ImmutableList.copyOf(optionSet);

        List<IHotkey> hotkeys = new ArrayList<>();
        for (IConfigBase option : optionSet) {
            if (option instanceof IHotkey hokey) {
                hotkeys.add(hokey);
            }
        }
        HOTKEYS = ImmutableList.copyOf(hotkeys);
    }

    public static class General {
        // 打印状态
        public static final ConfigBooleanHotkeyed WORK_TOGGLE = booleanHotkey("printSwitch")
                .defaultValue(false)
                .defaultHotkey("CAPS_LOCK")
                .keybindSettings(KeybindSettings.PRESS_ALLOWEXTRA_EMPTY)
                .build();

        // 核心 - 模式切换
        public static final ConfigOptionList WORK_MODE = optionList("modeSwitch")
                .defaultValue(ModeType.SINGLE)
                .build();

        // 核心 - 单模模式
        public static final ConfigOptionList WORK_MODE_TYPE = optionList("printerMode")
                .defaultValue(PrintModeType.PRINTER)
                .setVisible(isSingle) // 仅单模式时显示
                .build();

        // 核心 - 多模阻断
        public static final ConfigBoolean WORK_MODE_TYPE_MULTI_BREAK = bool("multiBreak")
                .defaultValue(true)
                .setVisible(isMulti) // 仅多模式时显示
                .build();

        // 核心 - 工作半径
        public static final ConfigInteger WORK_RANGE = integer("printerRange")
                .defaultValue(6)
                .range(1, 256)
                .build();

        // 核心 - 迭代占用时长
        public static final ConfigInteger ITERATOR_USE_TIME = integer("printerIteratorUseTime")
                .defaultValue(8)
                .range(0, 128)
                .build();

        // 核心 - 检查玩家方块交互范围
        public static final ConfigBoolean CHECK_PLAYER_INTERACTION_RANGE = bool("checkPlayerInteractionRange")
                .defaultValue(true)
                .build();

        // 核心 - 延迟检测
        public static final ConfigBoolean LAG_CHECK = bool("printerLagCheck")
                .defaultValue(true)
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
                .defaultValue(true)
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

        // 快捷潜影盒 - 开关
        public static final ConfigBoolean QUICK_SHULKER = bool("printerQuickShulker")
                .setVisible(isLoadQuickShulkerLoaded)
                .defaultValue(false)
                .build();

        // 快捷潜影盒 - 工作模式
        public static final ConfigOptionList QUICK_SHULKER_MODE = optionList("printerQuickShulkerMode")
                .setVisible(isLoadQuickShulkerLoaded)
                .defaultValue(QuickShulkerModeType.INVOKE)
                .build();

        // 快捷潜影盒 - 冷却时间
        public static final ConfigInteger QUICK_SHULKER_COOLDOWN = integer("printerQuickShulkerCooldown")
                .setVisible(isLoadQuickShulkerLoaded)
                .defaultValue(10)
                .range(0, 20)
                .build();

        // 储存管理 - 有序存放
        public static final ConfigBoolean STORE_ORDERLY = bool("storeOrderly")
                .defaultValue(false)
                .build();

        // 通用配置项列表（按功能分类排序）
        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                WORK_TOGGLE,                  // 打印状态
                WORK_MODE,                   // 核心 - 模式切换
                WORK_MODE_TYPE,                  // 核心 - 打印机模式
                WORK_MODE_TYPE_MULTI_BREAK,                   // 核心 - 多模阻断
                WORK_RANGE,                 // 核心 - 工作半径
                ITERATOR_USE_TIME,             // 核心 - 迭代占用时长
                Placement.PLACE_SPEED,
                Break.BREAK_SPEED,
                Placement.PLACE_BLOCKS_PER_TICK,
                Break.BREAK_BLOCKS_PER_TICK,
                RENDER_HUD,                    // 核心 - 显示打印机HUD
                LAG_CHECK,                     // 核心 - 延迟检测
                CHECK_PLAYER_INTERACTION_RANGE,// 核心 - 检查玩家方块交互距离
                ITERATOR_SHAPE,                // 核心 - 迭代区域形状
                ITERATION_ORDER,               // 核心 - 迭代遍历顺序
                X_REVERSE,                     // 核心 - 迭代X轴反向
                Y_REVERSE,                     // 核心 - 迭代Y轴反向
                Z_REVERSE,                     // 核心 - 迭代Z轴反向
                AUTO_DISABLE_PRINTER,          // 核心 - 自动禁用打印机
                UPDATE_CHECK,                  // 核心 - 检查更新
                DEBUG_OUTPUT,                  // 核心 - 调试输出
                CLOUD_INVENTORY,               // 远程交互 - 开关
                AUTO_INVENTORY,                // 远程交互 - 自动设置远程交互
                INVENTORY_LIST,                // 远程交互 - 库存白名单
                QUICK_SHULKER,                 // 快捷潜影盒 - 开关
                QUICK_SHULKER_MODE,            // 快捷潜影盒 - 工作模式
                QUICK_SHULKER_COOLDOWN,        // 快捷潜影盒 - 冷却时间
                STORE_ORDERLY                  // 储存管理 - 有序存放
        );
    }

    public static class Placement {
        // 投影轻松放置协议
        public static final ConfigBoolean EASY_PLACE_PROTOCOL = bool("easyPlaceProtocol")
                .defaultValue(false)
                .build();

        // 使用数据包打印
        public static final ConfigBoolean PLACE_USE_PACKET = bool("printerUsePacket")
                .defaultValue(false)
                .build();

        // 凭空放置
        public static final ConfigBoolean PLACE_IN_AIR = bool("printInAir")
                .defaultValue(true)
                .build();

        // 核心 - 工作间隔
        public static final ConfigInteger PLACE_SPEED = integer("printSpeed")
                .defaultValue(1)
                .range(0, 20)
                .build();

        // 每刻放置方块数
        public static final ConfigInteger PLACE_BLOCKS_PER_TICK = integer("placeBlocksPerTick")
                .defaultValue(8)
                .range(0, 256)
                .build();

        // 放置冷却
        public static final ConfigInteger PLACE_COOLDOWN = integer("placeCooldown")
                .defaultValue(3)
                .range(0, 64)
                .build();

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                EASY_PLACE_PROTOCOL,
                PLACE_USE_PACKET,
                General.WORK_RANGE,
                General.ITERATOR_USE_TIME,
                PLACE_SPEED,
                PLACE_BLOCKS_PER_TICK,
                PLACE_COOLDOWN
        );
    }

    public static class Break {
        public static final ConfigBoolean BREAK_PLACE_USE_PACKET = bool("breakPlaceUsePacket")
                .defaultValue(false)
                .build();

        public static final ConfigInteger BREAK_PROGRESS_THRESHOLD = integer("breakProgressThreshold")
                .defaultValue(70)
                .range(70, 100)
                .build();

        public static final ConfigInteger BREAK_SPEED = integer("breakSpeed")
                .defaultValue(1)
                .range(0, 20)
                .build();

        public static final ConfigInteger BREAK_BLOCKS_PER_TICK = integer("breakBlocksPerTick")
                .defaultValue(15)
                .range(0, 256)
                .build();

        public static final ConfigInteger BREAK_COOLDOWN = integer("breakCooldown")
                .defaultValue(1)
                .range(0, 64)
                .build();

        public static final ConfigBoolean BREAK_CHECK_BLOCK_HARDNESS = bool("breakCheckBlockHardness")
                .defaultValue(true)
                .build();

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                BREAK_PLACE_USE_PACKET,
                BREAK_PROGRESS_THRESHOLD,
                General.WORK_RANGE,
                General.ITERATOR_USE_TIME,
                BREAK_SPEED,
                BREAK_BLOCKS_PER_TICK,
                BREAK_COOLDOWN,
                BREAK_CHECK_BLOCK_HARDNESS
        );
    }

    public static class Print {
        // 打印热键
        public static final ConfigHotkey WORK_TOGGLE_HOTKEY = hotkey("printHotkey")
                .keybindSettings(BOTH_ALLOW_EXTRA_EMPTY)
                .build();

        // 多模开关
        public static final ConfigBooleanHotkeyed PRINT = booleanHotkey("print")
                .defaultValue(false)
                .setVisible(isMulti) // 仅多模式时显示
                .build();

        // 选区类型
        public static final ConfigOptionList PRINT_SELECTION_TYPE = optionList("printSelectionType")
                .defaultValue(SelectionType.LITEMATICA_RENDER_LAYER)
                .build();

        // 跳过含水方块
        public static final ConfigBoolean SKIP_WATERLOGGED_BLOCK = bool("printerSkipWaterlogged")
                .defaultValue(false)
                .build();

        // 跳过放置
        public static final ConfigBoolean PUT_SKIP = bool("putSkip")
                .defaultValue(false)
                .build();

        // 跳过放置名单
        public static final ConfigStringList PUT_SKIP_LIST = stringList("putSkipList")
                .build();

        // 始终潜行
        public static final ConfigBoolean FORCED_SNEAK = bool("forcedSneak")
                .defaultValue(false)
                .build();

        // 覆盖打印
        public static final ConfigBoolean REPLACE = bool("replace")
                .defaultValue(true)
                .build();

        // 覆盖方块列表
        public static final ConfigStringList REPLACEABLE_LIST = stringList("replaceableList")
                .defaultValue(Blocks.SNOW, Blocks.LAVA, Blocks.WATER, Blocks.BUBBLE_COLUMN, Blocks.SHORT_GRASS)
                .build();

        // 替换珊瑚
        public static final ConfigBoolean REPLACE_CORAL = bool("replaceCoral")
                .defaultValue(false)
                .build();

        // 破冰放水
        public static final ConfigBooleanHotkeyed PRINT_WATER = booleanHotkey("printWater")
                .defaultValue(false)
                .build();

        // 自动去皮
        public static final ConfigBoolean STRIP_LOGS = bool("printerAutoStripLogs")
                .defaultValue(false)
                .build();

        // 音符盒自动调音
        public static final ConfigBoolean NOTE_BLOCK_TUNING = bool("printerAutoTuning")
                .defaultValue(true)
                .build();

        // 侦测器安全放置
        public static final ConfigBoolean SAFELY_OBSERVER = bool("printerSafelyObserver")
                .defaultValue(true)
                .build();

        // 堆肥桶自动填充
        public static final ConfigBoolean FILL_COMPOSTER = bool("printerAutoFillComposter")
                .defaultValue(false)
                .build();

        // 堆肥桶白名单
        public static final ConfigStringList FILL_COMPOSTER_WHITELIST = stringList("printerAutoFillComposterWhitelist")
                .setVisible(FILL_COMPOSTER::getBooleanValue)
                .build();

        // 下落方块检查
        public static final ConfigBoolean FALLING_CHECK = bool("printerFallingBlockCheck")
                .defaultValue(true)
                .build();

        // 破坏错误方块
        public static final ConfigBoolean BREAK_WRONG_BLOCK = bool("printBreakWrongBlock")
                .defaultValue(false)
                .build();

        // 破坏多余方块
        public static final ConfigBoolean BREAK_EXTRA_BLOCK = bool("printerBreakExtraBlock")
                .defaultValue(false)
                .build();

        // 破坏错误状态方块（实验性）
        public static final ConfigBoolean BREAK_WRONG_STATE_BLOCK = bool("printerBreakWrongStateBlock")
                .defaultValue(false)
                .build();

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                WORK_TOGGLE_HOTKEY,
                PRINT,
                PRINT_SELECTION_TYPE,         // 打印 - 选区类型
                SKIP_WATERLOGGED_BLOCK,       // 打印 - 跳过打印含水方块
                PUT_SKIP,                     // 打印 - 跳过放置
                PUT_SKIP_LIST,                // 打印 - 跳过放置名单
                FORCED_SNEAK,                 // 打印 - 始终潜行
                REPLACE,                      // 打印 - 覆盖打印
                REPLACEABLE_LIST,             // 打印 - 覆盖方块列表
                REPLACE_CORAL,                // 打印 - 替换珊瑚
                PRINT_WATER,                  // 打印 - 破冰放水
                STRIP_LOGS,                   // 打印 - 自动去皮
                NOTE_BLOCK_TUNING,            // 打印 - 音符盒自动调音
                SAFELY_OBSERVER,              // 打印 - 侦测器安全放置
                FILL_COMPOSTER,               // 打印 - 堆肥桶自动填充
                FILL_COMPOSTER_WHITELIST,     // 打印 - 堆肥桶填充白名单
                FALLING_CHECK,                // 打印 - 下落方块检查
                BREAK_WRONG_BLOCK,            // 打印 - 破坏错误方块
                BREAK_EXTRA_BLOCK,            // 打印 - 破坏多余方块
                BREAK_WRONG_STATE_BLOCK       // 打印 - 破坏错误状态方块（实验性）
        );
    }

    public static class Excavate {
        // 多模开关
        public static final ConfigBooleanHotkeyed MINE = booleanHotkey("mine")
                .defaultValue(false)
                .setVisible(isMulti) // 仅多模式时显示
                .build();

        // 挖掘模式限制器
        public static final ConfigOptionList EXCAVATE_LIMITER = optionList("excavateLimiter")
                .defaultValue(ExcavateListMode.CUSTOM)
                .build();

        // 选区类型
        public static final ConfigOptionList MINE_SELECTION_TYPE = optionList("mineSelectionType")
                .defaultValue(SelectionType.LITEMATICA_SELECTION_ABOVE_PLAYER)
                .setVisible(isExcavateCustom) // 仅自定义挖掘限制时显示
                .build();

        // 挖掘模式限制
        public static final ConfigOptionList EXCAVATE_LIMIT = optionList("excavateLimit")
                .defaultValue(UsageRestriction.ListType.NONE)
                .setVisible(isExcavateCustom) // 仅自定义挖掘限制时显示
                .build();

        // 挖掘白名单
        public static final ConfigStringList EXCAVATE_WHITELIST = stringList("excavateWhitelist")
                .setVisible(isExcavateCustom) // 仅自定义挖掘限制时显示
                .build();

        // 挖掘黑名单
        public static final ConfigStringList EXCAVATE_BLACKLIST = stringList("excavateBlacklist")
                .setVisible(isExcavateCustom) // 仅自定义挖掘限制时显示
                .build();

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                MINE,                         // 挖掘 - 多模开关
                EXCAVATE_LIMITER,             // 挖掘 - 挖掘模式限制器

                Break.BREAK_PLACE_USE_PACKET,
                Break.BREAK_PROGRESS_THRESHOLD,

                // 自定义限制配置
                MINE_SELECTION_TYPE,          // 挖掘 - 选区类型
                EXCAVATE_LIMIT,               // 挖掘 - 挖掘模式限制
                EXCAVATE_WHITELIST,           // 挖掘 - 挖掘白名单
                EXCAVATE_BLACKLIST            // 挖掘 - 挖掘黑名单
        );
    }

    public static class Fill {
        // 开关
        public static final ConfigBooleanHotkeyed FILL = booleanHotkey("fill")
                .defaultValue(false)
                .setVisible(isMulti) // 仅多模式时显示
                .build();

        // 选区类型
        public static final ConfigOptionList FILL_SELECTION_TYPE = optionList("fillSelectionType")
                .defaultValue(SelectionType.LITEMATICA_SELECTION)
                .build();

        // 填充方块模式
        public static final ConfigOptionList FILL_BLOCK_MODE = optionList("fillBlockMode")
                .defaultValue(FileBlockModeType.WHITELIST)
                .build();

        // 填充方块名单
        public static final ConfigStringList FILL_BLOCK_LIST = stringList("fillBlockList")
                .defaultValue(Blocks.COBBLESTONE)
                .build();

        // 模式朝向
        public static final ConfigOptionList FILL_BLOCK_FACING = optionList("fillModeFacing")
                .defaultValue(FillModeFacingType.DOWN)
                .build();

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                // 核心开关 & 基础配置
                FILL,                         // 填充 - 多模开关
                FILL_SELECTION_TYPE,          // 填充 - 选区类型

                // 填充规则配置
                FILL_BLOCK_MODE,              // 填充 - 填充方块模式
                FILL_BLOCK_LIST,              // 填充 - 填充方块名单
                FILL_BLOCK_FACING             // 填充 - 模式朝向
        );
    }

    public static class FLUID {
        // 开关
        public static final ConfigBooleanHotkeyed FLUID = booleanHotkey("fluid")
                .defaultValue(false)
                .setVisible(isMulti) // 仅多模式时显示
                .build();

        // 选区类型
        public static final ConfigOptionList FLUID_SELECTION_TYPE = optionList("fluidSelectionType")
                .defaultValue(SelectionType.LITEMATICA_SELECTION)
                .build();

        // 填充流动液体
        public static final ConfigBoolean FILL_FLOWING_FLUID = bool("fluidModeFillFlowing")
                .defaultValue(true)
                .build();

        // 方块名单
        public static final ConfigStringList FLUID_BLOCK_LIST = stringList("fluidBlockList")
                .defaultValue(Blocks.SAND)
                .build();

        // 液体名单
        public static final ConfigStringList FLUID_LIST = stringList("fluidList")
                .defaultValue(Blocks.WATER, Blocks.LAVA)
                .build();

        // 排流体配置项列表（按功能分类排序）
        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                FLUID,                        // 排流体 - 多模开关
                FLUID_SELECTION_TYPE,         // 排流体 - 选区类型
                FILL_FLOWING_FLUID,           // 排流体 - 填充流动液体
                FLUID_BLOCK_LIST,             // 排流体 - 方块名单
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
                .bindConfig(General.WORK_MODE_TYPE)
                .setVisible(isSingle) // 仅单模式时显示
                .build();

        // 破基岩
        public static final ConfigBooleanHotkeyed BEDROCK = booleanHotkey("bedrock")
                .defaultValue(false)
                .setVisible(isMulti) // 仅多模式时显示
                .build();

        // ========== 远程交互热键 ==========
        // 同步容器热键
        public static final ConfigHotkey SYNC_INVENTORY = hotkey("syncInventory")
                .build();

        // 同步容器开关热键
        public static final ConfigBooleanHotkeyed SYNC_INVENTORY_CHECK = booleanHotkey("syncInventoryCheck")
                .defaultValue(false)
                .build();

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
                // 核心操作热键
                OPEN_SCREEN,                  // 打开设置菜单
                General.WORK_TOGGLE,
                Print.WORK_TOGGLE_HOTKEY,           // 打印热键
                CLOSE_ALL_MODE,               // 关闭全部模式
                SWITCH_PRINTER_MODE,          // 切换模式

                // 多模功能热键
                Print.PRINT,
                Excavate.MINE,                // 挖掘
                Fill.FILL,                    // 填充
                FLUID.FLUID,                  // 排流体
                BEDROCK,                      // 破基岩

                // 远程交互热键
                SYNC_INVENTORY,               // 同步容器热键
                SYNC_INVENTORY_CHECK,         // 同步容器开关热键
                PRINTER_INVENTORY,            // 设置打印机库存热键
                REMOVE_PRINT_INVENTORY,       // 清空打印机库存热键
                LAST,                         // 上一个箱子
                NEXT,                         // 下一个箱子
                DELETE                        // 删除当前容器
        );
    }

    public static class Color {
        // 容器同步与打印机添加库存高亮颜色
        public static final ConfigColor SYNC_INVENTORY_COLOR = color("syncInventoryColor")
                .defaultValue("#4CFF4CE6")
                .build();

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                SYNC_INVENTORY_COLOR          // 容器同步与打印机添加库存高亮颜色
        );
    }

    @Override
    public void load() {
        File settingFile = new File(FILE_PATH);
        if (settingFile.isFile() && settingFile.exists()) {
            JsonElement jsonElement = JsonUtils.parseJsonFile(settingFile);
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
            JsonUtils.writeJsonToFile(configRoot, new File(FILE_PATH));
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