package me.aleksilassila.litematica.printer;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.*;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeyCallbackToggleBooleanConfigWithMessage;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import me.aleksilassila.litematica.printer.printer.State;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.HighlightBlockRenderer;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
//#if MC >= 12001
import me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils;
//#endif
import java.util.List;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics.loadChestTracker;

public class LitematicaMixinMod implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "litematica_printer";
    private static final KeybindSettings GUI_NO_ORDER = KeybindSettings.create(KeybindSettings.Context.GUI, KeyAction.PRESS, false, false, false, true);
    // Config settings
    public static final ConfigInteger PRINT_INTERVAL = new ConfigInteger( "打印机工作间隔",1,0,20,"每次放置的间隔，以§b游戏刻度§r为单位。数值越低意味着打印速度越快。\n在值为0时可能在服务器中表现效果不佳，需开启§b使用数据包放置方块§r。");
    public static final ConfigInteger COMPULSION_RANGE = new ConfigInteger("打印机工作半径",6,1,256,"每次放置到的最远距离，以玩家中心为半径。数值越高意味着打印的范围越大。\n此参数与§b§6半径模式§r有关联。");
    public static final ConfigInteger PUT_COOLING = new ConfigInteger("放置冷却", 2,0,256,"当同一位置的方块没有成功放置时，等待特定的值才会再次放置。");
    public static final ConfigBoolean PLACE_USE_PACKET = new ConfigBoolean("使用数据包放置方块",false,"可以得到更快的放置速度，并且不会出现“幽灵方块”的情况。§b但是无法听到放置方块的声音。§r");
    public static final ConfigBoolean SWITCH_ITEM_USE_PACKET = new ConfigBoolean("使用数据包切换物品",false,"可以避免在开启§b使用数据包放置方块§r打印的情况下，因客户端同步不及时所导致误将错误的方块打印出来。\n对于不使用§b使用数据包放置方块§r并且§b打印机工作间隔§r大于1的用户来说，无需开启这个功能。");
    public static final ConfigBoolean AFTER_SWITCH_ITEM_SYNC = new ConfigBoolean("物品栏同步到客户端",false,"可以修复由于§b使用数据包切换到物品栏§r导致的物品栏和实际手持物品货不对板的问题。");
    public static final ConfigOptionList RANGE_MODE = new ConfigOptionList("半径模式", State.ListType.SPHERE,"立方体建议3，球体建议设置6，破基岩在立方体模式下无法正常使用。");
    public static final ConfigOptionList MODE_SWITCH = new ConfigOptionList("模式切换", State.ModeType.SINGLE,"单模：仅运行一个模式。多模：可多个模式同时运行。");
    public static final ConfigOptionList PRINTER_MODE = new ConfigOptionList("打印机模式", State.PrintModeType.PRINTER,"打印机的工作模式。");
    public static final ConfigBoolean MULTI_BREAK = new ConfigBoolean("多模阻断",true, "启用后将按模式优先级运行，同时启用多个模式时优先级低的无法执行。");
    public static final ConfigBoolean RENDER_LAYER_LIMIT = new ConfigBoolean("渲染层数限制",false, "根据Litematica的渲染层数限制来限制打印机的工作范围。");
    public static final ConfigBoolean PRINT_IN_AIR = new ConfigBoolean("凭空放置",true, "无视是否有方块面支撑，直接放置方块。");
    public static final ConfigBooleanHotkeyed PRINT_WATER_LOGGED_BLOCK = new ConfigBooleanHotkeyed("打印含水方块",  false,"","启用后会自动放置冰,破坏冰来使方块含水。");
    public static final ConfigBooleanHotkeyed BREAK_ERROR_BLOCK = new ConfigBooleanHotkeyed("破坏错误方块",  false,"","打印过程中自动破坏投影中错误的方块。");
    public static final ConfigBoolean PRINT_SWITCH = new ConfigBoolean("开启打印",false, "打印的开关状态。");
    public static final ConfigBoolean EASY_MODE = new ConfigBoolean("精准放置",false, "根据投影的设置使用对应的协议。");
    public static final ConfigBooleanHotkeyed USE_EASY_MODE = new ConfigBooleanHotkeyed("轻松放置模式",false,"", "启用后会调用轻松放置来进行放置，因为轻松放置本身会使用放置协议，所以在开启了这个功能后无需启用§b精准放置§r。");
    public static final ConfigBoolean FORCED_PLACEMENT = new ConfigBoolean("强制潜行",false, "打印时会强制shift避免一些方块的交互。");
    public static final ConfigBoolean REPLACE = new ConfigBoolean("替换列表方块",true, "可以直接在一些可替换方块放置，例如 草 雪片。");
    public static final ConfigBoolean STRIP_LOGS = new ConfigBoolean("自动去树皮",false, "在打印时会自动去除树木的树皮。");
    public static boolean shouldPrintInAir = PRINT_IN_AIR.getBooleanValue();
    public static final ConfigHotkey SWITCH_PRINTER_MODE = new ConfigHotkey("切换模式", "J", "切换打印机工作模式。");
    public static final ConfigBooleanHotkeyed BEDROCK_SWITCH = new ConfigBooleanHotkeyed("破基岩", false,"", "切换为破基岩模式,此模式下y轴会从上往下判定。");
    public static final ConfigBooleanHotkeyed EXCAVATE = new ConfigBooleanHotkeyed("挖掘", false,"", "挖掘所选区内的方块。");
    public static final ConfigBooleanHotkeyed FLUID = new ConfigBooleanHotkeyed("排流体", false,"", "在岩浆源、水源处放方块默认是沙子。");
    public static final ConfigHotkey CLOSE_ALL_MODE = new ConfigHotkey("关闭全部模式", "LEFT_CONTROL,G","关闭全部模式，若此时为单模模式将模式恢复为打印");

    //#if MC >= 12001
    public static final ConfigHotkey LAST = new ConfigHotkey("上一个箱子", "",GUI_NO_ORDER,"");
    public static final ConfigHotkey NEXT = new ConfigHotkey("下一个箱子", "",GUI_NO_ORDER,"");
    public static final ConfigHotkey DELETE = new ConfigHotkey("删除当前容器", "",GUI_NO_ORDER,"");
    //#endif

    public static final ConfigStringList FLUID_BLOCK_LIST = new ConfigStringList("排流体方块名单", ImmutableList.of("minecraft:sand"), "此项需严格填写");
    public static final ConfigBoolean PUT_SKIP = new ConfigBoolean("跳过放置", false, "开启后会跳过列表内的方块");
    public static final ConfigBoolean PUT_TESTING = new ConfigBoolean("侦测器放置检测", false, "检测侦测器看向的方块是否和投影方块一致，若不一致测跳过放置");
    public static final ConfigBoolean QUICKSHULKER = new ConfigBoolean("快捷潜影盒", false, "在服务器装有AxShulkers的情况下可以直接从背包内的潜影盒取出物品\n替换的位置为Litematica的预设位置,如果所有预设位置都有濳影盒则不会替换。");
    public static final ConfigBoolean INVENTORY = new ConfigBoolean("远程交互容器", false, "在服务器支持远程交互容器或单机的情况下可以远程交互\n替换的位置为投影的预设位置。");
    public static final ConfigBoolean AUTO_INVENTORY = new ConfigBoolean("自动设置远程交互", false, "在服务器若允许使用则自动开启远程交互容器，反之则自动关闭");

    public static final ConfigBoolean PRINT_CHECK = new ConfigBoolean("有序存放", false, "在背包满时将从快捷盒子或打印机库存中取出的物品还原到之前位置，关闭后将会打乱打印机以及濳影盒");

    public static final ConfigStringList INVENTORY_LIST = new ConfigStringList("库存白名单", ImmutableList.of("minecraft:chest"), "打印机库存的白名单，只有白名单内的容器才会被记录。");
    public static final ConfigOptionList EXCAVATE_LIMITER = new ConfigOptionList("挖掘模式限制器",State.ExcavateListMode.ME,"使用tw挖掘限制预设或自带的限制");
    public static final ConfigOptionList EXCAVATE_LIMIT = new ConfigOptionList("挖掘模式限制", UsageRestriction.ListType.NONE,"挖掘模式的限制");
    public static final ConfigStringList EXCAVATE_WHITELIST = new ConfigStringList("挖掘白名单", ImmutableList.of(""), "#minecraft:*** 前方加入#可以按标签搜索，用,分隔可以填入参数\n" +
            "c:包含（例如 橡树树叶 填入 叶,c 那么此项会被通过）");
    public static final ConfigStringList EXCAVATE_BLACKLIST = new ConfigStringList("挖掘黑名单", ImmutableList.of(""), "#minecraft:*** 前方加入#可以按标签搜索，用,分隔可以填入参数\n" +
            "c:包含（例如 橡树树叶 填入 叶,c 那么此项会被通过）");
    public static final ConfigStringList PUT_SKIP_LIST = new ConfigStringList("跳过放置名单", ImmutableList.of(), "跳过放置的方块名单");
    public static final ConfigStringList BEDROCK_LIST = new ConfigStringList("基岩模式白名单", ImmutableList.of("minecraft:bedrock"), "破基岩模式的白名单，只有白名单内的方块会被破坏。");
    public static final ConfigStringList REPLACEABLE_LIST = new ConfigStringList("可替换方块",
            ImmutableList.of("minecraft:snow","minecraft:lava","minecraft:water","minecraft:bubble_column","minecraft:short_grass"), "打印时将忽略这些错误方块，直接在该位置打印。");
    public static ImmutableList<IConfigBase> getConfigList() {
        List<IConfigBase> list = new java.util.ArrayList<>(Configs.Generic.OPTIONS);
        list.add(PRINT_SWITCH);
        list.add(EASY_MODE);
        list.add(PRINT_INTERVAL);
        list.add(COMPULSION_RANGE);
        if(PRINTER_MODE.getOptionListValue().equals(State.ModeType.SINGLE)) list.add(PRINTER_MODE);
        if(EXCAVATE_LIMITER.getOptionListValue().equals(State.ExcavateListMode.ME)) list.add(EXCAVATE_LIMIT);
        if(EXCAVATE_LIMITER.getOptionListValue().equals(State.ExcavateListMode.ME)) list.add(EXCAVATE_WHITELIST);
        if(EXCAVATE_LIMITER.getOptionListValue().equals(State.ExcavateListMode.ME)) list.add(EXCAVATE_BLACKLIST);
        list.add(PRINT_IN_AIR);
        list.add(PRINT_WATER_LOGGED_BLOCK);
        list.add(BREAK_ERROR_BLOCK);

        return ImmutableList.copyOf(list);
    }

    // Hotkeys
    public static final ConfigHotkey PRINT = new ConfigHotkey("print", "", KeybindSettings.PRESS_ALLOWEXTRA_EMPTY, "Prints while pressed");
    public static final ConfigHotkey TOGGLE_PRINTING_MODE = new ConfigHotkey("togglePrintingMode", "CAPS_LOCK", KeybindSettings.PRESS_ALLOWEXTRA_EMPTY, "Allows quickly toggling on/off Printing mode");

    //	public static final ConfigHotkey BEDROCK_MODE = new ConfigHotkey("破基岩模式", "J", "切换为破基岩模式 此模式下 y轴会从上往下判定.");
//	public static final ConfigHotkey EXE_MODE= new ConfigHotkey("挖掘模式", "K", "挖掘所选区的方块");
    public static final ConfigHotkey SYNC_INVENTORY = new ConfigHotkey("容器同步", "", "按下热键后会记录看向容器的物品。\n将投影选区内的同类型容器中的物品，同步至记录的容器。");
    public static final ConfigBooleanHotkeyed SYNC_INVENTORY_CHECK = new ConfigBooleanHotkeyed("同步时检查背包", false,"", "容器同步时检查背包，如果填充物不足，则不会打开容器");
    public static final ConfigHotkey PRINTER_INVENTORY= new ConfigHotkey("打印机库存", "", "如果远程取物的目标是未加载的区块将会增加取物品的时间，用投影选区后按下热键\n" +
            "打印机工作时将会使用该库存内的物品\n" +
            "建议库存区域内放置假人来常加载区块");
    public static final ConfigHotkey REMOVE_PRINT_INVENTORY = new ConfigHotkey("清空打印机库存", "", "清空打印机库存");


    public static List<IConfigBase> getHotkeyList() {
        List<IConfigBase> list = new java.util.ArrayList<>(Hotkeys.HOTKEY_LIST);
        list.add(PRINT);
        list.add(TOGGLE_PRINTING_MODE);

        list.add(CLOSE_ALL_MODE);
        if(MODE_SWITCH.getOptionListValue() == State.ModeType.SINGLE) {
            list.add(SWITCH_PRINTER_MODE);
        }
        return ImmutableList.copyOf(list);
    }
    public static final ConfigColor SYNC_INVENTORY_COLOR = new ConfigColor("容器同步与打印机添加库存高亮颜色","#4CFF4CE6", "");

    public static ImmutableList<IConfigBase> getColorsList() {
        List<IConfigBase> list = new java.util.ArrayList<>(Configs.Colors.OPTIONS);
        list.add(SYNC_INVENTORY_COLOR);
        return ImmutableList.copyOf(list);
    }

    @Override
    public void onInitialize() {
        reSetConfig();
//		KeyCallbackHotkeys keyCallbackHotkeys = new KeyCallbackHotkeys(MinecraftClient.getInstance());
        OpenInventoryPacket.init();
        OpenInventoryPacket.registerReceivePacket();
        OpenInventoryPacket.registerClientReceivePacket();
        //#if MC >= 12001
        if(loadChestTracker) MemoryUtils.setup();
        //#endif

        TOGGLE_PRINTING_MODE.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(PRINT_SWITCH));
//		SYNC_INVENTORY.getKeybind().setCallback(keyCallbackHotkeys);
//		SWITCH_PRINTER_MODE.getKeybind().setCallback(keyCallbackHotkeys);
//		if(loadChestTracker){
//			PRINTER_INVENTORY.getKeybind().setCallback(keyCallbackHotkeys);
//			REMOVE_PRINT_INVENTORY.getKeybind().setCallback(keyCallbackHotkeys);
//			//#if MC > 12001
//			LAST.getKeybind().setCallback(keyCallbackHotkeys);
//			NEXT.getKeybind().setCallback(keyCallbackHotkeys);
//			DELETE.getKeybind().setCallback(keyCallbackHotkeys);
//			//#endif
//		}
        me.aleksilassila.litematica.printer.config.Configs.init();
        HighlightBlockRenderer.init();
//		BEDROCK_MODE.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(BEDROCK_SWITCH));
//		EXE_MODE.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(EXCAVATE));
    }

    @Override
    public void onInitializeClient() {

    }
    private void reSetConfig(){
        if(!loadChestTracker){
            AUTO_INVENTORY.setBooleanValue(false);
            INVENTORY.setBooleanValue(false);
        }
    }
}