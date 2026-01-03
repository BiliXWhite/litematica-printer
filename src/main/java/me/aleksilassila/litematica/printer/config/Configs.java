package me.aleksilassila.litematica.printer.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.config.*;
import fi.dy.masa.malilib.config.options.*;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.LitematicaPrinterMod;
import me.aleksilassila.litematica.printer.bilixwhite.utils.BedrockUtils;
import me.aleksilassila.litematica.printer.config.enums.*;
import me.aleksilassila.litematica.printer.bilixwhite.ModLoadStatus;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.utils.MessageUtils;
import me.aleksilassila.litematica.printer.utils.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static me.aleksilassila.litematica.printer.config.ConfigFactory.*;
import static me.aleksilassila.litematica.printer.config.ConfigFactory.bool;
import static me.aleksilassila.litematica.printer.config.ConfigFactory.booleanHotkey;
import static me.aleksilassila.litematica.printer.config.ConfigFactory.hotkey;
import static me.aleksilassila.litematica.printer.config.ConfigFactory.integer;

public class Configs implements IConfigHandler {
    public static Configs INSTANCE = new Configs();
    private static final String FILE_PATH = "./config/" + LitematicaPrinterMod.MOD_ID + ".json";
    private static final File CONFIG_DIR = new File("./config");


    // @formatter:off

//    public static final ConfigBooleanHotkeyed REPLACE_BLOCK = new ConfigBooleanHotkeyed("替换", false,"", "替换方块，通过\"替换方块名单\"配置");

    public static final ConfigBoolean CHECK_PLAYER_INTERACTION_RANGE = bool(I18n.CHECK_PLAYER_INTERACTION_RANGE, true);

    public static final ConfigOptionList PRINT_SELECTION_TYPE     = optionList(I18n.PRINT_SELECTION_TYPE            , SelectionType.LITEMATICA_RENDER_LAYER);
    public static final ConfigOptionList FILL_SELECTION_TYPE      = optionList(I18n.FILL_SELECTION_TYPE            , SelectionType.LITEMATICA_SELECTION);
    public static final ConfigOptionList FLUID_SELECTION_TYPE     = optionList(I18n.FLUID_SELECTION_TYPE            , SelectionType.LITEMATICA_SELECTION);
    public static final ConfigOptionList MINE_SELECTION_TYPE      = optionList(I18n.MINE_SELECTION_TYPE            , SelectionType.LITEMATICA_SELECTION_ABOVE_PLAYER);

    public static final ConfigStringList FLUID_BLOCK_LIST = stringList(I18n.FLUID_BLOCK_LIST, ImmutableList.of("minecraft:sand"));
    public static final ConfigStringList FLUID_LIST = stringList(I18n.FLUID_LIST, ImmutableList.of("minecraft:water", "minecraft:lava"));

    public static final ConfigOptionList FILL_BLOCK_MODE = optionList(I18n.FILL_BLOCK_MODE, FileBlockModeType.WHITELIST);
    public static final ConfigStringList FILL_BLOCK_LIST = stringList(I18n.FILL_BLOCK_LIST, ImmutableList.of("minecraft:cobblestone"));
    public static final ConfigStringList INVENTORY_LIST = stringList(
            I18n.INVENTORY_LIST,
            ImmutableList.of("minecraft:chest")
    );
    public static final ConfigStringList EXCAVATE_WHITELIST = stringList(I18n.EXCAVATE_WHITELIST, ImmutableList.of(""));
    public static final ConfigStringList EXCAVATE_BLACKLIST = stringList(I18n.EXCAVATE_BLACKLIST, ImmutableList.of(""));
    public static final ConfigStringList PUT_SKIP_LIST = stringList(I18n.PUT_SKIP_LIST, ImmutableList.of(""));
    public static final ConfigStringList REPLACEABLE_LIST = stringList(
            I18n.REPLACEABLE_LIST,
            ImmutableList.of(
                    "minecraft:snow", "minecraft:lava", "minecraft:water",
                    "minecraft:bubble_column", "minecraft:short_grass"
            )
    );

    public static final ConfigOptionList ITERATOR_SHAPE     = optionList(I18n.PRINTER_ITERATOR_SHAPE    , RadiusShapeType.SPHERE);
    public static final ConfigOptionList QUICK_SHULKER_MODE = optionList(I18n.PRINTER_QUICK_SHULKER_MODE, QuickShulkerModeType.INVOKE);
    public static final ConfigOptionList ITERATION_ORDER    = optionList(I18n.PRINTER_ITERATOR_MODE     , IterationOrderType.XZY);
    public static final ConfigOptionList FILL_BLOCK_FACING  = optionList(I18n.FILL_MODE_FACING          , FillModeFacingType.DOWN);

    public static final ConfigOptionList MODE_SWITCH        = optionList(I18n.MODE_SWITCH               , ModeType.SINGLE);
    public static final ConfigOptionList PRINTER_MODE       = optionList(I18n.PRINTER_MODE              , PrintModeType.PRINTER);
    public static final ConfigOptionList EXCAVATE_LIMITER   = optionList(I18n.EXCAVATE_LIMITER          , ExcavateListMode.CUSTOM);
    public static final ConfigOptionList EXCAVATE_LIMIT     = optionList(I18n.EXCAVATE_LIMIT            , UsageRestriction.ListType.NONE);
    public static final ConfigColor SYNC_INVENTORY_COLOR    = color(I18n.SYNC_INVENTORY_COLOR           , "#4CFF4CE6");


    public static final ConfigBoolean PLACE_USE_PACKET          = bool(I18n.PRINTER_USE_PACKET              , false);
    public static final ConfigBoolean STRIP_LOGS                = bool(I18n.PRINTER_AUTO_STRIP_LOGS         , false);
    public static final ConfigBoolean QUICK_SHULKER             = bool(I18n.PRINTER_QUICK_SHULKER           , false);
    public static final ConfigBoolean LAG_CHECK                 = bool(I18n.PRINTER_LAG_CHECK               , true );
    public static final ConfigBoolean X_REVERSE                 = bool(I18n.PRINTER_X_AXIS_REVERSE          , false);
    public static final ConfigBoolean Y_REVERSE                 = bool(I18n.PRINTER_Y_AXIS_REVERSE          , false);
    public static final ConfigBoolean Z_REVERSE                 = bool(I18n.PRINTER_Z_AXIS_REVERSE          , false);
    public static final ConfigBoolean FALLING_CHECK             = bool(I18n.PRINTER_FALLING_BLOCK_CHECK     , true );
    public static final ConfigBoolean BREAK_WRONG_BLOCK         = bool(I18n.PRINT_BREAK_WRONG_BLOCK         , false);
    public static final ConfigBoolean DEBUG_OUTPUT              = bool(I18n.DEBUG_OUTPUT                    , false);
    public static final ConfigBoolean NOTE_BLOCK_TUNING         = bool(I18n.PRINTER_AUTO_TUNING             , true );
    public static final ConfigBoolean SAFELY_OBSERVER           = bool(I18n.PRINTER_SAFELY_OBSERVER         , true );
    public static final ConfigBoolean BREAK_EXTRA_BLOCK         = bool(I18n.PRINTER_BREAK_EXTRA_BLOCK       , false);
    public static final ConfigBoolean BREAK_WRONG_STATE_BLOCK   = bool(I18n.PRINTER_BREAK_WRONG_STATE_BLOCK , false);
    public static final ConfigBoolean SKIP_WATERLOGGED_BLOCK    = bool(I18n.PRINTER_SKIP_WATERLOGGED        , false);
    public static final ConfigBoolean UPDATE_CHECK              = bool(I18n.UPDATE_CHECK                    , true );
    public static final ConfigBoolean FILL_COMPOSTER            = bool(I18n.PRINTER_AUTO_FILL_COMPOSTER     , false);
    public static final ConfigBoolean FILL_FLOWING_FLUID        = bool(I18n.FLUID_MODE_FILL_FLOWING         , true );
    public static final ConfigBoolean AUTO_DISABLE_PRINTER      = bool(I18n.PRINTER_AUTO_DISABLE            , true );
    public static final ConfigBoolean EASYPLACE_PROTOCOL        = bool(I18n.EASY_PLACE_PROTOCOL             , true );
    public static final ConfigBoolean MULTI_BREAK               = bool(I18n.MULTI_BREAK                     , true );
    //    public static final ConfigBoolean RENDER_LAYER_LIMIT        = bool(I18n.RENDER_LAYER_LIMIT              , false);
    public static final ConfigBoolean PRINT_IN_AIR              = bool(I18n.PRINT_IN_AIR                    , true );
    public static final ConfigBoolean FORCED_SNEAK              = bool(I18n.FORCED_SNEAK                    , false);
    public static final ConfigBoolean REPLACE                   = bool(I18n.REPLACE                         , true );
    public static final ConfigBoolean PUT_SKIP                  = bool(I18n.PUT_SKIP                        , false);
    public static final ConfigBoolean CLOUD_INVENTORY           = bool(I18n.CLOUD_INVENTORY                 , false);
    public static final ConfigBoolean AUTO_INVENTORY            = bool(I18n.AUTO_INVENTORY                  , false);
    public static final ConfigBoolean STORE_ORDERLY             = bool(I18n.STORE_ORDERLY                   , false);
    public static final ConfigBoolean REPLACE_CORAL             = bool(I18n.REPLACE_CORAL                   , false);
    public static final ConfigBoolean RENDER_HUD                = bool(I18n.RENDER_HUD                      , false);

    public static final ConfigInteger PRINTER_SPEED             = integer(I18n.PRINTER_SPEED                    , 1, 0, 20);
    public static final ConfigInteger BLOCKS_PER_TICK           = integer(I18n.PRINTER_BLOCKS_PER_TICK          , 4, 0, 24);
    public static final ConfigInteger PLACE_COOLDOWN            = integer(I18n.PRINTER_PLACE_COOLDOWN           , 3, 0, 64);
    public static final ConfigInteger PRINTER_RANGE             = integer(I18n.PRINTER_RANGE                    , 6, 1, 256);
    public static final ConfigInteger QUICK_SHULKER_COOLDOWN    = integer(I18n.PRINTER_QUICK_SHULKER_COOLDOWN   , 10, 0, 20);
    public static final ConfigInteger ITERATOR_USE_TIME         = integer(I18n.PRINTER_ITERATOR_USE_TIME        , 8, 0, 128);

    public static final ConfigBooleanHotkeyed PRINT_WATER           = booleanHotkey(I18n.PRINT_WATER              ,false);
    public static final ConfigBooleanHotkeyed MINE                  = booleanHotkey(I18n.MINE                     ,false);
    public static final ConfigBooleanHotkeyed FLUID                 = booleanHotkey(I18n.FLUID                    ,false);
    public static final ConfigBooleanHotkeyed FILL                  = booleanHotkey(I18n.FILL                     ,false);
    public static final ConfigBooleanHotkeyed BEDROCK               = booleanHotkey(I18n.BEDROCK                  ,false);
    public static final ConfigBooleanHotkeyed SYNC_INVENTORY_CHECK  = booleanHotkey(I18n.SYNC_INVENTORY_CHECK     ,false);
    public static final ConfigBooleanHotkeyed PRINT_SWITCH          = booleanHotkey(I18n.PRINT_SWITCH     ,false, "CAPS_LOCK", KeybindSettings.PRESS_ALLOWEXTRA_EMPTY);

    public static final ConfigHotkey OPEN_SCREEN            = hotkey(I18n.OPEN_SCREEN, "Z,Y");
    public static final ConfigHotkey PRINT                  = hotkey(I18n.PRINT, KeybindSettings.PRESS_ALLOWEXTRA_EMPTY);
    public static final ConfigHotkey CLOSE_ALL_MODE         = hotkey(I18n.CLOSE_ALL_MODE,       "LEFT_CONTROL,G");
    public static final ConfigHotkey SWITCH_PRINTER_MODE    = hotkey(I18n.SWITCH_PRINTER_MODE       );
    public static final ConfigHotkey SYNC_INVENTORY         = hotkey(I18n.SYNC_INVENTORY            );
    public static final ConfigHotkey PRINTER_INVENTORY      = hotkey(I18n.PRINTER_INVENTORY         );
    public static final ConfigHotkey REMOVE_PRINT_INVENTORY = hotkey(I18n.REMOVE_PRINT_INVENTORY    );
    //#if MC >= 12001
    private static final KeybindSettings GUI_NO_ORDER = KeybindSettings.create(
            KeybindSettings.Context.GUI, KeyAction.PRESS, false, false, false, true
    );
    public static final ConfigHotkey LAST   = hotkey(I18n.LAST      , GUI_NO_ORDER);
    public static final ConfigHotkey NEXT   = hotkey(I18n.NEXT      , GUI_NO_ORDER);
    public static final ConfigHotkey DELETE = hotkey(I18n.DELETE    , GUI_NO_ORDER);
    //#endif

    // @formatter:on


    public static List<IConfigBase> getHotkeyList() {
        List<IConfigBase> list = new ArrayList<>(Hotkeys.HOTKEY_LIST);
        list.add(PRINT);
        list.add(PRINT_SWITCH);
        list.add(CLOSE_ALL_MODE);
        if (MODE_SWITCH.getOptionListValue() == ModeType.SINGLE) {
            list.add(SWITCH_PRINTER_MODE);
        }
        return ImmutableList.copyOf(list);
    }


    //===========通用设置===========
    public static ImmutableList<IConfigBase> getGeneral() {
        List<IConfigBase> list = new ArrayList<>();
        // 箱子追踪
        if (ModLoadStatus.isLoadChestTrackerLoaded()) {
            list.add(CLOUD_INVENTORY);                          // 远程交互容器
            list.add(AUTO_INVENTORY);                           // 自动设置远程交互
        }
        list.add(PRINT_SWITCH);                                 // 打印状态
        list.add(CHECK_PLAYER_INTERACTION_RANGE); // 核心 - 检查玩家方块交互距离
        list.add(PRINTER_SPEED);                                // 核心 - 工作间隔
        list.add(BLOCKS_PER_TICK);                              // 核心 - 每刻放置方块数(没必要隐藏)
        list.add(PRINTER_RANGE);                                // 核心 - 工作半径长度
        list.add(PLACE_COOLDOWN);                               // 核心 - 放置冷却
        list.add(ITERATOR_USE_TIME);                            // 核心 - 迭代占用时长
        list.add(ITERATOR_SHAPE);                               // 核心 - 迭代区域形状
        list.add(LAG_CHECK);                                    // 核心 - 延迟检测
        list.add(PLACE_USE_PACKET);                             // 打印 - 数据包打印
        list.add(RENDER_HUD);                                   // 显示打印机 HUD
        list.add(QUICK_SHULKER);                                // 快捷潜影盒
        list.add(QUICK_SHULKER_MODE);                           // 快捷潜影盒 - 工作模式
        list.add(QUICK_SHULKER_COOLDOWN);                       // 快捷潜影盒 - 冷却时间
        list.add(ITERATION_ORDER);                              // 迭代 - 遍历顺序
        list.add(X_REVERSE);                                    // 迭代-X轴反向
        list.add(Y_REVERSE);                                    // 迭代-Y轴反向
        list.add(Z_REVERSE);                                    // 迭代-Z轴反向
        list.add(MODE_SWITCH);                                  // 模式切换
        // 模式切换
        if (MODE_SWITCH.getOptionListValue().equals(ModeType.SINGLE)) {
            list.add(PRINTER_MODE);                 // 打印机模式
        } else {
            list.add(MULTI_BREAK);                  // 多模阻断
        }
        list.add(FLUID_BLOCK_LIST);                 // 排流体-方块名单
        list.add(FLUID_LIST);                       // 排流体-液体名单
        list.add(FILL_BLOCK_MODE);                  // 填充方块模式
        list.add(FILL_BLOCK_LIST);                  // 填充方块名单
        list.add(FILL_BLOCK_FACING);                // 填充 - 模式朝向

        // 箱子追踪
        if (ModLoadStatus.isLoadChestTrackerLoaded()) {
            list.add(INVENTORY_LIST);               // 库存白名单
        }
        list.add(DEBUG_OUTPUT);                     // 调试输出
        list.add(UPDATE_CHECK);                     // 检查更新
        list.add(AUTO_DISABLE_PRINTER);             // 核心 - 自动禁用打印机
        return ImmutableList.copyOf(list);
    }

    //===========放置设置===========
    public static ImmutableList<IConfigBase> getPut() {
        List<IConfigBase> list = new ArrayList<>();
        list.add(PUT_SKIP);                 // 跳过放置
        list.add(PUT_SKIP_LIST);            // 跳过放置名单
        list.add(STORE_ORDERLY);            // 有序存放
        list.add(EASYPLACE_PROTOCOL);       // 打印 - 使用轻松放置协议
        list.add(FORCED_SNEAK);             // 打印时潜行
        list.add(PRINT_IN_AIR);             // 凭空放置
        list.add(SKIP_WATERLOGGED_BLOCK);   // 跳过打印含水方块
        list.add(REPLACE);                  // 覆盖打印
        list.add(REPLACEABLE_LIST);         // 覆盖方块列表
        list.add(REPLACE_CORAL);            // 替换珊瑚
        list.add(PRINT_SELECTION_TYPE);     // 打印 - 选区类型
        list.add(PRINT_WATER);              // 打印 - 破冰放水
        list.add(STRIP_LOGS);               // 打印 - 自动去皮
        list.add(NOTE_BLOCK_TUNING);        // 打印 - 音符盒自动调音
        list.add(SAFELY_OBSERVER);          // 打印 - 侦测器安全放置
        list.add(FILL_COMPOSTER);           // 打印 - 堆肥桶自动填充
        list.add(FALLING_CHECK);            // 打印 - 下落方块检查
        list.add(BREAK_WRONG_BLOCK);        // 打印 - 破坏错误方块
        list.add(BREAK_EXTRA_BLOCK);        // 打印 - 破坏多余方块
        list.add(BREAK_WRONG_STATE_BLOCK);  // 打印 - 破坏错误状态方块（实验性）
        list.add(FILL_FLOWING_FLUID);       // 排流体 - 填充流动状态液体
        return ImmutableList.copyOf(list);
    }

    //===========挖掘设置===========
    public static ImmutableList<IConfigBase> getExcavate() {
        List<IConfigBase> list = new ArrayList<>();
        list.add(EXCAVATE_LIMITER);         // 挖掘模式限制器
        if (EXCAVATE_LIMITER.getOptionListValue().equals(ExcavateListMode.CUSTOM)) {
            list.add(MINE_SELECTION_TYPE);  // 挖掘 - 选区类型
            list.add(EXCAVATE_LIMIT);       // 挖掘模式限制
            list.add(EXCAVATE_WHITELIST);   // 挖掘白名单
            list.add(EXCAVATE_BLACKLIST);   // 挖掘黑名单
        }
        return ImmutableList.copyOf(list);
    }

    //===========填充设置===========
    public static ImmutableList<IConfigBase> getFills() {
        List<IConfigBase> list = new ArrayList<>();
        if (MODE_SWITCH.getOptionListValue().equals(ModeType.MULTI)) {
            list.add(FILL);                         // 填充
        }
        list.add(FILL_SELECTION_TYPE);              // 填充 - 选区类型
        list.add(FILL_BLOCK_MODE);                  // 填充方块模式
        list.add(FILL_BLOCK_LIST);                  // 填充方块名单
        list.add(FILL_BLOCK_FACING);                // 填充 - 模式朝向
        return ImmutableList.copyOf(list);
    }

    //===========破基岩设置===========
    public static ImmutableList<IConfigBase> getBedrock() {
        List<IConfigBase> list = new ArrayList<>();
        return ImmutableList.copyOf(list);
    }

    //===========热键设置===========
    public static ImmutableList<IConfigBase> getHotkeys() {
        List<IConfigBase> list = new ArrayList<>();
        list.add(OPEN_SCREEN);                  // 打开设置菜单
        list.add(PRINT);                        // 打印热键
        list.add(PRINT_SWITCH);         // 切换打印状态
        // 切换打印状态热键
        if (MODE_SWITCH.getOptionListValue().equals(ModeType.SINGLE)) {
            list.add(SWITCH_PRINTER_MODE);      // 切换模式
        } else {
            list.add(MINE);                     // 挖掘
            list.add(FLUID);                    // 排流体
            list.add(FILL);                     // 填充
//            list.add(REPLACE_BLOCK);            // 替换
            list.add(BEDROCK);                  // 破基岩
        }
        list.add(CLOSE_ALL_MODE);               // 关闭全部模式
        list.add(SYNC_INVENTORY);               // 同步容器热键
        // 箱子追踪
        if (ModLoadStatus.isLoadChestTrackerLoaded()) {
            list.add(PRINTER_INVENTORY);        // 设置打印机库存热键
            list.add(REMOVE_PRINT_INVENTORY);   // 清空打印机库存热键
            //#if MC >= 12001
            list.add(LAST);                     // 切换到上一个箱子热键
            list.add(NEXT);                     // 切换到下一个箱子热键
            list.add(DELETE);                   // 删除当前容器热键
            //#endif
        }
        return ImmutableList.copyOf(list);
    }

    //===========颜色设置===========
    public static ImmutableList<IConfigBase> getColor() {
        List<IConfigBase> list = new ArrayList<>();
        list.add(SYNC_INVENTORY_COLOR); // 容器同步与打印机添加库存高亮颜色

        return ImmutableList.copyOf(list);
    }

    //按下时激活
    public static ImmutableList<ConfigHotkey> getKeyList() {
        ArrayList<ConfigHotkey> list = new ArrayList<>();
        list.add(OPEN_SCREEN);                  // 打开设置菜单
        list.add(SYNC_INVENTORY);               // 同步容器热键
        list.add(SWITCH_PRINTER_MODE);          // 切换模式

        // 箱子追踪
        if (ModLoadStatus.isLoadChestTrackerLoaded()) {
            list.add(PRINTER_INVENTORY);        // 设置打印机库存热键
            list.add(REMOVE_PRINT_INVENTORY);   // 清空打印机库存热键
            //#if MC >= 12001
            list.add(LAST);                     // 切换到上一个箱子热键
            list.add(NEXT);                     // 切换到下一个箱子热键
            list.add(DELETE);                   // 删除当前容器热键
            //#endif
        }
        return ImmutableList.copyOf(list);
    }

    //切换型开关
    public static ImmutableList<IHotkeyTogglable> getSwitchKey() {
        ArrayList<IHotkeyTogglable> list = new ArrayList<>();
        list.add(MINE);             // 挖掘
        list.add(FLUID);            // 排流体
        list.add(FILL);             // 填充
//        list.add(REPLACE_BLOCK);    // 替换
        list.add(BEDROCK);          // 破基岩
        list.add(PRINT_WATER);      // 打印 - 破冰放水
        return ImmutableList.copyOf(list);
    }

    public static ImmutableList<IConfigBase> getAllConfigs() {
        List<IConfigBase> list = new ArrayList<>();
        list.addAll(getGeneral());  // 通用
        list.addAll(getPut());      // 放置
        list.addAll(getExcavate()); // 挖掘
        list.addAll(getBedrock());  // 破基岩
        list.addAll(getHotkeys());  // 热键
        list.addAll(getColor());    // 颜色
        list = list.stream().distinct().toList();   // 去重
        return ImmutableList.copyOf(list);
    }

    @Override
    public void load() {
        File settingFile = new File(FILE_PATH);
        if (settingFile.isFile() && settingFile.exists()) {
            JsonElement jsonElement = JsonUtils.parseJsonFile(settingFile);
            if (jsonElement != null && jsonElement.isJsonObject()) {
                JsonObject obj = jsonElement.getAsJsonObject();
                ConfigUtils.readConfigBase(obj, LitematicaPrinterMod.MOD_ID, getAllConfigs());
            }
        }
    }

    @Override
    public void save() {
        if ((CONFIG_DIR.exists() && CONFIG_DIR.isDirectory()) || CONFIG_DIR.mkdirs()) {
            JsonObject configRoot = new JsonObject();
            ConfigUtils.writeConfigBase(configRoot, LitematicaPrinterMod.MOD_ID, getAllConfigs());
            JsonUtils.writeJsonToFile(configRoot, new File(FILE_PATH));
        }
    }

    public static void init() {
        Configs.INSTANCE.load();
        ConfigManager.getInstance().registerConfigHandler(LitematicaPrinterMod.MOD_ID, Configs.INSTANCE);
        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerKeyboardInputHandler(InputHandler.getInstance());
        HotkeysCallback.init();

        // 箱子追踪
        if (!ModLoadStatus.isLoadChestTrackerLoaded()) {
            AUTO_INVENTORY.setBooleanValue(false);  // 自动设置远程交互
            CLOUD_INVENTORY.setBooleanValue(false); // 远程交互容器
        }

        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerKeyboardInputHandler(InputHandler.getInstance());

        //#if MC >= 12001 && MC <= 12104
        //$$ if (ModLoadStatus.isLoadChestTrackerLoaded()) {
        //$$     me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils.setup();
        //$$ }
        //#endif

        CLOSE_ALL_MODE.getKeybind().setCallback((action, keybind) -> {
            if (keybind.isPressed()) {
                Configs.MINE.setBooleanValue(false);
                Configs.FLUID.setBooleanValue(false);
                Configs.PRINT_SWITCH.setBooleanValue(false);
//            Configs.REPLACE_BLOCK.setBooleanValue(false);
                Configs.PRINTER_MODE.setOptionListValue(PrintModeType.PRINTER);
                MessageUtils.setOverlayMessage(StringUtils.nullToEmpty("已关闭全部模式"));
            }
            return true;
        });

        // 打印状态值被修改
        PRINT_SWITCH.setValueChangeCallback(b -> {
            if (!b.getBooleanValue()) {
                Printer printer = Printer.getInstance();
                printer.clearQueue();
                printer.lastPos = null;
                printer.basePos = null;
                printer.pistonNeedFix = false;
                printer.blockContext = null;
                if (ModLoadStatus.isBedrockMinerLoaded()) {
                    if (BedrockUtils.isWorking()) {
                        BedrockUtils.setWorking(false);
                        BedrockUtils.setBedrockMinerFeatureEnable(true);
                    }
                }
            }
        });

        // 切换模式时, 关闭破基岩
        PRINTER_MODE.setValueChangeCallback(b -> {
            if (!b.getOptionListValue().equals(PrintModeType.BEDROCK)) {
                if (ModLoadStatus.isBedrockMinerLoaded()) {
                    if (BedrockUtils.isWorking()) {
                        BedrockUtils.setWorking(false);
                        BedrockUtils.setBedrockMinerFeatureEnable(true);
                    }
                }
            }
        });

    }
}