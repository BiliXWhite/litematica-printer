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
import me.aleksilassila.litematica.printer.I18n;
import fi.dy.masa.malilib.config.ConfigManager;
import me.aleksilassila.litematica.printer.Reference;
import me.aleksilassila.litematica.printer.config.enums.*;
import me.aleksilassila.litematica.printer.bilixwhite.ModLoadStatus;
import me.aleksilassila.litematica.printer.gui.ConfigUi;
import net.minecraft.world.level.block.Blocks;

import java.io.File;
import java.util.ArrayList;
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
    private static final BooleanSupplier isSingle = () -> General.MODE_SWITCH.getOptionListValue().equals(ModeType.SINGLE);
    private static final BooleanSupplier isMulti = () -> General.MODE_SWITCH.getOptionListValue().equals(ModeType.MULTI);
    private static final BooleanSupplier isExcavateCustom = () -> Excavate.EXCAVATE_LIMITER.getOptionListValue().equals(ExcavateListMode.CUSTOM);

    public static final ImmutableList<IConfigBase> OPTIONS;
    public static final ImmutableList<IHotkey> HOTKEYS;


    static {
        ArrayList<IConfigBase> options = new ArrayList<>();
        options.addAll(General.OPTIONS);        // 通用配置
        options.addAll(Put.OPTIONS);            // 放置配置
        options.addAll(Excavate.OPTIONS);       // 挖掘配置
        options.addAll(Fill.OPTIONS);           // 填充配置
        options.addAll(FLUID.OPTIONS);          // 排流体配置
        options.addAll(Color.OPTIONS);          // 颜色配置
        options.addAll(Hotkeys.OPTIONS);        // 热键配置
        OPTIONS = ImmutableList.copyOf(options);

        List<IHotkey> hotkeys = new ArrayList<>();
        for (IConfigBase option : options) {
            if (option instanceof IHotkey hokey) {
                hotkeys.add(hokey);
            }
        }
        HOTKEYS = ImmutableList.copyOf(hotkeys);
    }

    public static class General {
        // ========== 核心开关 & 模式 ==========
        // 打印状态
        public static final ConfigBooleanHotkeyed PRINT_SWITCH = booleanHotkey(I18n.PRINT_SWITCH)
                .defaultValue(false)
                .defaultHotkey("CAPS_LOCK")
                .keybindSettings(KeybindSettings.PRESS_ALLOWEXTRA_EMPTY)
                .build();

        // 核心 - 模式切换
        public static final ConfigOptionList MODE_SWITCH = optionList(I18n.MODE_SWITCH)
                .defaultValue(ModeType.SINGLE)
                .build();

        // 核心 - 单模模式
        public static final ConfigOptionList PRINTER_MODE = optionList(I18n.PRINTER_MODE)
                .defaultValue(PrintModeType.PRINTER)
                .setVisible(isSingle) // 仅单模式时显示
                .build();

        // 核心 - 多模阻断
        public static final ConfigBoolean MULTI_BREAK = bool(I18n.MULTI_BREAK)
                .defaultValue(true)
                .setVisible(isMulti) // 仅多模式时显示
                .build();

        // 核心 - 使用投影轻松放置协议
        public static final ConfigBoolean EASY_PLACE_PROTOCOL = bool(I18n.EASY_PLACE_PROTOCOL)
                .defaultValue(true)
                .build();

        // 核心 - 工作半径
        public static final ConfigInteger PRINTER_RANGE = integer(I18n.PRINTER_RANGE)
                .defaultValue(6)
                .range(1, 256)
                .build();

        // 核心 - 工作间隔
        public static final ConfigInteger PRINTER_SPEED = integer(I18n.PRINTER_SPEED)
                .defaultValue(1)
                .range(0, 20)
                .build();

        // 核心 - 每刻放置方块数
        public static final ConfigInteger BLOCKS_PER_TICK = integer(I18n.PRINTER_BLOCKS_PER_TICK)
                .defaultValue(4)
                .range(0, 24)
                .build();

        // 核心 - 迭代占用时长
        public static final ConfigInteger ITERATOR_USE_TIME = integer(I18n.PRINTER_ITERATOR_USE_TIME)
                .defaultValue(8)
                .range(0, 128)
                .build();

        // 核心 - 检查玩家方块交互范围
        public static final ConfigBoolean CHECK_PLAYER_INTERACTION_RANGE = bool(I18n.CHECK_PLAYER_INTERACTION_RANGE)
                .defaultValue(true)
                .build();

        // 核心 - 放置冷却
        public static final ConfigInteger PLACE_COOLDOWN = integer(I18n.PRINTER_PLACE_COOLDOWN)
                .defaultValue(3)
                .range(0, 64)
                .build();

        // 核心 - 延迟检测
        public static final ConfigBoolean LAG_CHECK = bool(I18n.PRINTER_LAG_CHECK)
                .defaultValue(true)
                .build();

        // 核心 - 迭代区域形状
        public static final ConfigOptionList ITERATOR_SHAPE = optionList(I18n.PRINTER_ITERATOR_SHAPE)
                .defaultValue(RadiusShapeType.SPHERE)
                .build();

        // 核心 - 遍历顺序
        public static final ConfigOptionList ITERATION_ORDER = optionList(I18n.PRINTER_ITERATOR_MODE)
                .defaultValue(IterationOrderType.XZY)
                .build();

        // 核心 - 迭代模式
        public static final ConfigOptionList ITERATION_MODE = optionList(I18n.of("ITERATION_MODE"))
                .defaultValue(IterationModeType.LINEAR)
                .build();

        // 核心 - 迭代X轴反向
        public static final ConfigBoolean X_REVERSE = bool(I18n.PRINTER_X_AXIS_REVERSE)
                .defaultValue(false)
                .build();

        // 核心 - 迭代X轴反向
        public static final ConfigBoolean CIRCLE_DIRECTION = bool(I18n.of("CIRCLE_DIRECTION"))
                .defaultValue(false)
                .build();

        // 核心 - 迭代Y轴反向
        public static final ConfigBoolean Y_REVERSE = bool(I18n.PRINTER_Y_AXIS_REVERSE)
                .defaultValue(false)
                .build();

        // 核心 - 迭代Z轴反向
        public static final ConfigBoolean Z_REVERSE = bool(I18n.PRINTER_Z_AXIS_REVERSE)
                .defaultValue(false)
                .build();

        // 核心 - 显示打印机HUD
        public static final ConfigBoolean RENDER_HUD = bool(I18n.RENDER_HUD)
                .defaultValue(false)
                .build();

        // 核心 - 自动禁用打印机
        public static final ConfigBoolean AUTO_DISABLE_PRINTER = bool(I18n.PRINTER_AUTO_DISABLE)
                .defaultValue(true)
                .build();

        // 核心 - 检查更新
        public static final ConfigBoolean UPDATE_CHECK = bool(I18n.UPDATE_CHECK)
                .defaultValue(true)
                .build();

        // 核心 - 调试输出
        public static final ConfigBoolean DEBUG_OUTPUT = bool(I18n.DEBUG_OUTPUT)
                .defaultValue(false)
                .build();

        // 远程交互 - 开关
        public static final ConfigBoolean CLOUD_INVENTORY = bool(I18n.CLOUD_INVENTORY)
                .defaultValue(false)
                .setVisible(isLoadChestTrackerLoaded) // 仅箱子追踪 Mod 加载时显示
                .build();

        // 远程交互 - 自动设置远程交互
        public static final ConfigBoolean AUTO_INVENTORY = bool(I18n.AUTO_INVENTORY)
                .defaultValue(false)
                .setVisible(isLoadChestTrackerLoaded) // 仅箱子追踪 Mod 加载时显示
                .build();

        // 远程交互 - 库存白名单
        public static final ConfigStringList INVENTORY_LIST = stringList(I18n.INVENTORY_LIST)
                .defaultValue(Blocks.CHEST)
                .setVisible(isLoadChestTrackerLoaded) // 仅箱子追踪 Mod 加载时显示
                .build();

        // 快捷潜影盒 - 开关
        public static final ConfigBoolean QUICK_SHULKER = bool(I18n.PRINTER_QUICK_SHULKER)
                .setVisible(isLoadQuickShulkerLoaded)
                .defaultValue(false)
                .build();

        // 快捷潜影盒 - 工作模式
        public static final ConfigOptionList QUICK_SHULKER_MODE = optionList(I18n.PRINTER_QUICK_SHULKER_MODE)
                .setVisible(isLoadQuickShulkerLoaded)
                .defaultValue(QuickShulkerModeType.INVOKE)
                .build();

        // 快捷潜影盒 - 冷却时间
        public static final ConfigInteger QUICK_SHULKER_COOLDOWN = integer(I18n.PRINTER_QUICK_SHULKER_COOLDOWN)
                .setVisible(isLoadQuickShulkerLoaded)
                .defaultValue(10)
                .range(0, 20)
                .build();

        // 储存管理 - 有序存放
        public static final ConfigBoolean STORE_ORDERLY = bool(I18n.STORE_ORDERLY)
                .defaultValue(false)
                .build();

        // 通用配置项列表（按功能分类排序）
        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                PRINT_SWITCH,                  // 打印状态
                MODE_SWITCH,                   // 核心 - 模式切换
                PRINTER_MODE,                  // 核心 - 打印机模式
                MULTI_BREAK,                   // 核心 - 多模阻断
                PRINTER_SPEED,                 // 核心 - 工作间隔
                BLOCKS_PER_TICK,               // 核心 - 每刻放置方块数
                ITERATOR_USE_TIME,             // 核心 - 迭代占用时长
                PLACE_COOLDOWN,                // 核心 - 放置冷却
                PRINTER_RANGE,                 // 核心 - 工作半径
                RENDER_HUD,                    // 核心 - 显示打印机HUD
                LAG_CHECK,                     // 核心 - 延迟检测
                EASY_PLACE_PROTOCOL,           // 核心 - 使用投影轻松放置协议
                CHECK_PLAYER_INTERACTION_RANGE,// 核心 - 检查玩家方块交互距离
                ITERATOR_SHAPE,                // 核心 - 迭代区域形状
                ITERATION_ORDER,               // 核心 - 迭代遍历顺序
                ITERATION_MODE ,
                CIRCLE_DIRECTION,
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

    public static class Put {
        // ========== 选区 & 基础放置 ==========
        // 打印 - 选区类型
        public static final ConfigOptionList PRINT_SELECTION_TYPE = optionList(I18n.PRINT_SELECTION_TYPE)
                .defaultValue(SelectionType.LITEMATICA_RENDER_LAYER)
                .build();

        // 打印 - 使用数据包打印
        public static final ConfigBoolean PLACE_USE_PACKET = bool(I18n.PRINTER_USE_PACKET)
                .defaultValue(false)
                .build();

        // 打印 - 凭空放置
        public static final ConfigBoolean PRINT_IN_AIR = bool(I18n.PRINT_IN_AIR)
                .defaultValue(true)
                .build();

        // 打印 - 跳过含水方块
        public static final ConfigBoolean SKIP_WATERLOGGED_BLOCK = bool(I18n.PRINTER_SKIP_WATERLOGGED)
                .defaultValue(false)
                .build();

        // 打印 - 跳过放置
        public static final ConfigBoolean PUT_SKIP = bool(I18n.PUT_SKIP)
                .defaultValue(false)
                .build();

        // 打印 - 跳过放置名单
        public static final ConfigStringList PUT_SKIP_LIST = stringList(I18n.PUT_SKIP_LIST)
                .setVisible(PUT_SKIP::getBooleanValue)
                .build();

        // 打印 - 始终潜行
        public static final ConfigBoolean FORCED_SNEAK = bool(I18n.FORCED_SNEAK)
                .defaultValue(false)
                .build();

        // 打印 - 覆盖打印
        public static final ConfigBoolean REPLACE = bool(I18n.REPLACE)
                .defaultValue(true)
                .build();

        // 打印 - 覆盖方块列表
        public static final ConfigStringList REPLACEABLE_LIST = stringList(I18n.REPLACEABLE_LIST)
                .defaultValue(Blocks.SNOW, Blocks.LAVA, Blocks.WATER, Blocks.BUBBLE_COLUMN, Blocks.SHORT_GRASS)
                .build();

        // 打印 - 替换珊瑚
        public static final ConfigBoolean REPLACE_CORAL = bool(I18n.REPLACE_CORAL)
                .defaultValue(false)
                .build();

        // 打印 - 破冰放水
        public static final ConfigBooleanHotkeyed PRINT_WATER = booleanHotkey(I18n.PRINT_WATER)
                .defaultValue(false)
                .build();

        // 打印 - 自动去皮
        public static final ConfigBoolean STRIP_LOGS = bool(I18n.PRINTER_AUTO_STRIP_LOGS)
                .defaultValue(false)
                .build();

        // 打印 - 音符盒自动调音
        public static final ConfigBoolean NOTE_BLOCK_TUNING = bool(I18n.PRINTER_AUTO_TUNING)
                .defaultValue(true)
                .build();

        // 打印 - 侦测器安全放置
        public static final ConfigBoolean SAFELY_OBSERVER = bool(I18n.PRINTER_SAFELY_OBSERVER)
                .defaultValue(true)
                .build();

        // 打印 - 堆肥桶自动填充
        public static final ConfigBoolean FILL_COMPOSTER = bool(I18n.PRINTER_AUTO_FILL_COMPOSTER)
                .defaultValue(false)
                .build();

        // 打印 - 堆肥桶白名单
        public static final ConfigStringList FILL_COMPOSTER_WHITELIST = stringList(I18n.PRINTER_AUTO_FILL_COMPOSTER_WHITELIST)
                .setVisible(FILL_COMPOSTER::getBooleanValue)
                .build();

        // 打印 - 下落方块检查
        public static final ConfigBoolean FALLING_CHECK = bool(I18n.PRINTER_FALLING_BLOCK_CHECK)
                .defaultValue(true)
                .build();

        // 打印 - 破坏错误方块
        public static final ConfigBoolean BREAK_WRONG_BLOCK = bool(I18n.PRINT_BREAK_WRONG_BLOCK)
                .defaultValue(false)
                .build();

        // 打印 - 破坏多余方块
        public static final ConfigBoolean BREAK_EXTRA_BLOCK = bool(I18n.PRINTER_BREAK_EXTRA_BLOCK)
                .defaultValue(false)
                .build();

        // 打印 - 破坏错误状态方块（实验性）
        public static final ConfigBoolean BREAK_WRONG_STATE_BLOCK = bool(I18n.PRINTER_BREAK_WRONG_STATE_BLOCK)
                .defaultValue(false)
                .build();

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                PRINT_SELECTION_TYPE,         // 打印 - 选区类型
                PLACE_USE_PACKET,             // 打印 - 数据包打印
                PRINT_IN_AIR,                 // 打印 - 凭空放置
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
        // ========== 核心开关 & 限制器 ==========
        // 挖掘 - 多模开关
        public static final ConfigBooleanHotkeyed MINE = booleanHotkey(I18n.MINE)
                .defaultValue(false)
                .setVisible(isMulti) // 仅多模式时显示
                .build();

        // 挖掘 - 挖掘模式限制器
        public static final ConfigOptionList EXCAVATE_LIMITER = optionList(I18n.EXCAVATE_LIMITER)
                .defaultValue(ExcavateListMode.CUSTOM)
                .build();

        // ========== 自定义限制配置 ==========
        // 挖掘 - 选区类型
        public static final ConfigOptionList MINE_SELECTION_TYPE = optionList(I18n.MINE_SELECTION_TYPE)
                .defaultValue(SelectionType.LITEMATICA_SELECTION_ABOVE_PLAYER)
                .setVisible(isExcavateCustom) // 仅自定义挖掘限制时显示
                .build();

        // 挖掘 - 挖掘模式限制
        public static final ConfigOptionList EXCAVATE_LIMIT = optionList(I18n.EXCAVATE_LIMIT)
                .defaultValue(UsageRestriction.ListType.NONE)
                .setVisible(isExcavateCustom) // 仅自定义挖掘限制时显示
                .build();

        // 挖掘 - 挖掘白名单
        public static final ConfigStringList EXCAVATE_WHITELIST = stringList(I18n.EXCAVATE_WHITELIST)
                .setVisible(isExcavateCustom) // 仅自定义挖掘限制时显示
                .build();

        // 挖掘 - 挖掘黑名单
        public static final ConfigStringList EXCAVATE_BLACKLIST = stringList(I18n.EXCAVATE_BLACKLIST)
                .setVisible(isExcavateCustom) // 仅自定义挖掘限制时显示
                .build();

        // 挖掘配置项列表（按功能分类排序）
        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                // 核心开关 & 限制器
                MINE,                         // 挖掘 - 多模开关
                EXCAVATE_LIMITER,             // 挖掘 - 挖掘模式限制器

                // 自定义限制配置
                MINE_SELECTION_TYPE,          // 挖掘 - 选区类型
                EXCAVATE_LIMIT,               // 挖掘 - 挖掘模式限制
                EXCAVATE_WHITELIST,           // 挖掘 - 挖掘白名单
                EXCAVATE_BLACKLIST            // 挖掘 - 挖掘黑名单
        );
    }

    public static class Fill {
        // ========== 核心开关 & 基础配置 ==========
        // 填充 - 多模开关
        public static final ConfigBooleanHotkeyed FILL = booleanHotkey(I18n.FILL)
                .defaultValue(false)
                .setVisible(isMulti) // 仅多模式时显示
                .build();

        // 填充 - 选区类型
        public static final ConfigOptionList FILL_SELECTION_TYPE = optionList(I18n.FILL_SELECTION_TYPE)
                .defaultValue(SelectionType.LITEMATICA_SELECTION)
                .build();

        // ========== 填充规则配置 ==========
        // 填充 - 填充方块模式
        public static final ConfigOptionList FILL_BLOCK_MODE = optionList(I18n.FILL_BLOCK_MODE)
                .defaultValue(FileBlockModeType.WHITELIST)
                .build();

        // 填充 - 填充方块名单
        public static final ConfigStringList FILL_BLOCK_LIST = stringList(I18n.FILL_BLOCK_LIST)
                .defaultValue(Blocks.COBBLESTONE)
                .build();

        // 填充 - 模式朝向
        public static final ConfigOptionList FILL_BLOCK_FACING = optionList(I18n.FILL_MODE_FACING)
                .defaultValue(FillModeFacingType.DOWN)
                .build();

        // 填充配置项列表（按功能分类排序）
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
        // ========== 核心开关 & 基础配置 ==========
        // 排流体 - 多模开关
        public static final ConfigBooleanHotkeyed FLUID = booleanHotkey(I18n.FLUID)
                .defaultValue(false)
                .setVisible(isMulti) // 仅多模式时显示
                .build();

        // 排流体 - 选区类型
        public static final ConfigOptionList FLUID_SELECTION_TYPE = optionList(I18n.FLUID_SELECTION_TYPE)
                .defaultValue(SelectionType.LITEMATICA_SELECTION)
                .build();

        // ========== 替换规则配置 ==========
        // 排流体 - 填充流动液体
        public static final ConfigBoolean FILL_FLOWING_FLUID = bool(I18n.FLUID_MODE_FILL_FLOWING)
                .defaultValue(true)
                .build();

        // 排流体 - 方块名单
        public static final ConfigStringList FLUID_BLOCK_LIST = stringList(I18n.FLUID_BLOCK_LIST)
                .defaultValue(Blocks.SAND)
                .build();

        // 排流体 - 液体名单
        public static final ConfigStringList FLUID_LIST = stringList(I18n.FLUID_LIST)
                .defaultValue(Blocks.WATER, Blocks.LAVA)
                .build();

        // 排流体配置项列表（按功能分类排序）
        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                // 核心开关 & 基础配置
                FLUID,                        // 排流体 - 多模开关
                FLUID_SELECTION_TYPE,         // 排流体 - 选区类型

                // 替换规则配置
                FILL_FLOWING_FLUID,           // 排流体 - 填充流动液体
                FLUID_BLOCK_LIST,             // 排流体 - 方块名单
                FLUID_LIST                    // 排流体 - 液体名单
        );
    }

    public static class Hotkeys {
        // ========== 核心操作热键 ==========
        // 打开设置菜单
        public static final ConfigHotkey OPEN_SCREEN = hotkey(I18n.OPEN_SCREEN)
                .defaultStorageString("Z,Y")
                .build();

        // 打印状态
        public static final ConfigBooleanHotkeyed PRINT_SWITCH = General.PRINT_SWITCH;

        // 打印热键
        public static final ConfigHotkey PRINT = hotkey(I18n.PRINT)
                .keybindSettings(BOTH_ALLOW_EXTRA_EMPTY)
                .build();

        // 关闭全部模式
        public static final ConfigHotkey CLOSE_ALL_MODE = hotkey(I18n.CLOSE_ALL_MODE)
                .defaultStorageString("LEFT_CONTROL,G")
                .build();

        // 切换模式
        public static final ConfigHotkey SWITCH_PRINTER_MODE = hotkey(I18n.SWITCH_PRINTER_MODE)
                .bindConfig(General.PRINTER_MODE)
                .setVisible(isSingle) // 仅单模式时显示
                .build();

        // ========== 多模功能热键 ==========
        // 挖掘
        public static final ConfigBooleanHotkeyed MINE = Configs.Excavate.MINE;

        // 填充
        public static final ConfigBooleanHotkeyed FILL = Configs.Fill.FILL;

        // 排流体
        public static final ConfigBooleanHotkeyed FLUID = Configs.FLUID.FLUID;

        // 破基岩
        public static final ConfigBooleanHotkeyed BEDROCK = booleanHotkey(I18n.BEDROCK)
                .defaultValue(false)
                .setVisible(isMulti) // 仅多模式时显示
                .build();

        // ========== 远程交互热键 ==========
        // 同步容器热键
        public static final ConfigHotkey SYNC_INVENTORY = hotkey(I18n.SYNC_INVENTORY)
                .build();

        // 同步容器开关热键
        public static final ConfigBooleanHotkeyed SYNC_INVENTORY_CHECK = booleanHotkey(I18n.SYNC_INVENTORY_CHECK)
                .defaultValue(false)
                .build();

        // 设置打印机库存热键
        public static final ConfigHotkey PRINTER_INVENTORY = hotkey(I18n.PRINTER_INVENTORY)
                .setVisible(isLoadChestTrackerLoaded) // 仅箱子追踪 Mod 加载时显示
                .build();

        // 清空打印机库存热键
        public static final ConfigHotkey REMOVE_PRINT_INVENTORY = hotkey(I18n.REMOVE_PRINT_INVENTORY)
                .setVisible(isLoadChestTrackerLoaded) // 仅箱子追踪 Mod 加载时显示
                .build();

        // 上一个箱子
        public static final ConfigHotkey LAST = hotkey(I18n.LAST)
                .keybindSettings(GUI_NO_ORDER)
                .setVisible(isLoadChestTrackerLoaded) // 仅箱子追踪 Mod 加载时显示
                .build();

        // 下一个箱子
        public static final ConfigHotkey NEXT = hotkey(I18n.NEXT)
                .keybindSettings(GUI_NO_ORDER)
                .setVisible(isLoadChestTrackerLoaded) // 仅箱子追踪 Mod 加载时显示
                .build();

        // 删除当前容器
        public static final ConfigHotkey DELETE = hotkey(I18n.DELETE)
                .keybindSettings(GUI_NO_ORDER)
                .setVisible(isLoadChestTrackerLoaded) // 仅箱子追踪 Mod 加载时显示
                .build();

        // 热键配置项列表（按功能分类排序）
        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                // 核心操作热键
                OPEN_SCREEN,                  // 打开设置菜单
                PRINT_SWITCH,                 // 打印状态
                PRINT,                        // 打印热键
                CLOSE_ALL_MODE,               // 关闭全部模式
                SWITCH_PRINTER_MODE,          // 切换模式

                // 多模功能热键
                MINE,                         // 挖掘
                FILL,                         // 填充
                FLUID,                        // 排流体
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
        public static final ConfigColor SYNC_INVENTORY_COLOR = color(I18n.SYNC_INVENTORY_COLOR)
                .defaultValue("#4CFF4CE6")
                .build();

        // 颜色配置项列表
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
    }
}