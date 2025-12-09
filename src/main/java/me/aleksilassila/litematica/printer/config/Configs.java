package me.aleksilassila.litematica.printer.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.*;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.util.JsonUtils;
import me.aleksilassila.litematica.printer.InitHandler;
import me.aleksilassila.litematica.printer.LitematicaPrinterMod;
import me.aleksilassila.litematica.printer.printer.State;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static me.aleksilassila.litematica.printer.InitHandler.*;

public class Configs implements IConfigHandler {
    public static Configs INSTANCE = new Configs();
    private static final String FILE_PATH = "./config/" + LitematicaPrinterMod.MOD_ID + ".json";
    private static final File CONFIG_DIR = new File("./config");

    // @formatter:off

    //===========通用设置===========
    public static ImmutableList<IConfigBase> getGeneral(){
        List<IConfigBase> list = new ArrayList<>();
        // 箱子追踪
        if(Statistics.loadChestTracker)
        {
            list.add(CLOUD_INVENTORY);              // 远程交互容器
            list.add(AUTO_INVENTORY);               // 自动设置远程交互
        }
        list.add(PRINT_SWITCH);                     // 打印状态
        list.add(PRINTER_SPEED);                    // 核心 - 打印速度
        if (PRINTER_SPEED.getIntegerValue() == 0){  // 核心 - 打印速度
            list.add(BLOCKS_PER_TICK);              // 核心 - 每刻放置方块数
        }
        list.add(PRINTER_RANGE);                    // 核心 - 工作半径长度
        list.add(PLACE_COOLDOWN);                   // 核心 - 放置冷却
        list.add(ITERATOR_USE_TIME);                // 核心 - 迭代占用时长
        list.add(ITERATOR_SHAPE);                   // 核心 - 迭代区域形状
        list.add(LAG_CHECK);                        // 核心 - 延迟检测
        list.add(PLACE_USE_PACKET);                 // 打印 - 数据包打印
        list.add(RENDER_HUD);                       // 显示打印机HUD
        list.add(QUICK_SHULKER);                    // 快捷潜影盒
        list.add(QUICK_SHULKER_MODE);               // 快捷潜影盒 - 工作模式
        list.add(QUICK_SHULKER_COOLDOWN);           // 快捷潜影盒 - 冷却时间
        list.add(ITERATION_ORDER);                  // 迭代 - 遍历顺序
        list.add(X_REVERSE);                        // 迭代-X轴反向
        list.add(Y_REVERSE);                        // 迭代-Y轴反向
        list.add(Z_REVERSE);                        // 迭代-Z轴反向
        list.add(MODE_SWITCH);                      // 模式切换
        // 模式切换
        if(MODE_SWITCH.getOptionListValue().equals(State.ModeType.SINGLE)) {
            list.add(PRINTER_MODE);                 // 打印机模式
        }
        else {
            list.add(MULTI_BREAK);                  // 多模阻断
        }
        list.add(RENDER_LAYER_LIMIT);               // 渲染层数限制
        list.add(FLUID_BLOCK_LIST);                 // 排流体-方块名单
        list.add(FLUID_LIST);                       // 排流体-液体名单
        list.add(FILL_BLOCK_MODE);                  // 填充方块模式
        list.add(FILL_BLOCK_LIST);                  // 填充方块名单
        list.add(FILL_BLOCK_FACING);                // 填充 - 模式朝向
        // 箱子追踪
        if(Statistics.loadChestTracker)
        {
            list.add(INVENTORY_LIST);               // 库存白名单
        }
        list.add(DEBUG_OUTPUT);                     // 调试输出
        list.add(UPDATE_CHECK);                     // 检查更新
        list.add(AUTO_DISABLE_PRINTER);             // 核心 - 自动禁用打印机
        return ImmutableList.copyOf(list);
    }

    //===========放置设置===========
    public static ImmutableList<IConfigBase> getPut(){
        List<IConfigBase> list = new ArrayList<>();
        list.add(PUT_SKIP);                 // 跳过放置
        list.add(PUT_SKIP_LIST);            // 跳过放置名单
        list.add(STORE_ORDERLY);            // 有序存放
        list.add(EASYPLACE_PROTOCOL);       // 打印 - 使用轻松放置协议
        list.add(USE_EASYPLACE);            // 轻松放置模式
        list.add(FORCED_SNEAK);             // 打印时潜行
        list.add(PRINT_IN_AIR);             // 凭空放置
        list.add(SKIP_WATERLOGGED_BLOCK);   // 跳过打印含水方块
        list.add(REPLACE);                  // 覆盖打印
        list.add(REPLACEABLE_LIST);         // 覆盖方块列表
        list.add(REPLACE_CORAL);            // 替换珊瑚
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
    public static ImmutableList<IConfigBase> getExcavate(){
        List<IConfigBase> list = new ArrayList<>();
        list.add(EXCAVATE_LIMITER);         // 挖掘模式限制器
        if(EXCAVATE_LIMITER.getOptionListValue().equals(State.ExcavateListMode.CUSTOM)){
            list.add(EXCAVATE_LIMIT);       // 挖掘模式限制
            list.add(EXCAVATE_WHITELIST);   // 挖掘白名单
            list.add(EXCAVATE_BLACKLIST);   // 挖掘黑名单
        }
        return ImmutableList.copyOf(list);
    }

    //===========填充设置===========
    public static ImmutableList<IConfigBase> getFills(){
        List<IConfigBase> list = new ArrayList<>();
        if(MODE_SWITCH.getOptionListValue().equals(State.ModeType.MULTI)) {
            list.add(FILL);                         // 填充
        }
        list.add(FILL_BLOCK_MODE);                  // 填充方块模式
        list.add(FILL_BLOCK_LIST);                  // 填充方块名单
        list.add(FILL_BLOCK_FACING);                // 填充 - 模式朝向
        return ImmutableList.copyOf(list);
    }

    //===========破基岩设置===========
    public static ImmutableList<IConfigBase> getBedrock(){
        List<IConfigBase> list = new ArrayList<>();
        return ImmutableList.copyOf(list);
    }

    //===========热键设置===========
    public static ImmutableList<IConfigBase> getHotkeys(){
        List<IConfigBase> list = new ArrayList<>();
        list.add(OPEN_SCREEN);                  // 打开设置菜单
        list.add(PRINT);                        // 打印热键
        list.add(TOGGLE_PRINTING_MODE);         // 切换打印状态热键
        // 切换打印状态热键
        if(MODE_SWITCH.getOptionListValue().equals(State.ModeType.SINGLE)) {
            list.add(SWITCH_PRINTER_MODE);      // 切换模式
        } else {
            list.add(MINE);                     // 挖掘
            list.add(FLUID);                    // 排流体
            list.add(FILL);                     // 填充
            list.add(BEDROCK);                  // 破基岩
        }
        list.add(CLOSE_ALL_MODE);               // 关闭全部模式
        list.add(SYNC_INVENTORY);               // 同步容器热键
        // 箱子追踪
        if(Statistics.loadChestTracker) {
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
    public static ImmutableList<IConfigBase> getColor(){
        List<IConfigBase> list = new ArrayList<>();
        list.add(SYNC_INVENTORY_COLOR); // 容器同步与打印机添加库存高亮颜色

        return ImmutableList.copyOf(list);
    }

    //按下时激活
    public static ImmutableList<ConfigHotkey> getKeyList(){
        ArrayList<ConfigHotkey> list = new ArrayList<>();
        list.add(OPEN_SCREEN);                  // 打开设置菜单
        list.add(SYNC_INVENTORY);               // 同步容器热键
        list.add(SWITCH_PRINTER_MODE);          // 切换模式

        // 箱子追踪
        if(Statistics.loadChestTracker) {
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
    public static ImmutableList<IHotkeyTogglable> getSwitchKey(){
        ArrayList<IHotkeyTogglable> list = new ArrayList<>();
        list.add(MINE);             // 挖掘
        list.add(FLUID);            // 排流体
        list.add(FILL);             // 填充
        list.add(BEDROCK);          // 破基岩
        list.add(PRINT_WATER);      // 打印 - 破冰放水
        list.add(USE_EASYPLACE);    // 轻松放置模式
        return ImmutableList.copyOf(list);
    }

    public static ImmutableList<IConfigBase> getAllConfigs(){
        List<IConfigBase> list = new ArrayList<>();
        list.addAll(getGeneral());  // 通用
        list.addAll(getPut());      // 放置
        list.addAll(getExcavate()); // 挖掘
        list.addAll(getHotkeys());  // 热键
        list.addAll(getColor());    // 颜色
        return ImmutableList.copyOf(list);
    }

    // @formatter:on

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
    }
}