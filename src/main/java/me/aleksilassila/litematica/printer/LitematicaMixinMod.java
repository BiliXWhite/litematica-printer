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

    private static final KeybindSettings GUI_NO_ORDER = KeybindSettings.create(
            KeybindSettings.Context.GUI, KeyAction.PRESS, false, false, false, true
    );

    //========================================
    //           Config Settings
    //========================================
    public static final ConfigInteger PRINT_INTERVAL = new ConfigInteger(
            "打印机工作间隔", 1, 0, 20,
            "每次放置的间隔，以§b游戏刻§r为单位。数值越低意味着打印速度越快。\n" +
                    "在值为§b0§r时可能在服务器中表现效果不佳，需开启§6§l使用数据包放置方块§r。"
    );

    public static final ConfigInteger COMPULSION_RANGE = new ConfigInteger(
            "打印机工作半径", 6, 1, 256,
            "每次放置到的最远距离，以玩家中心为半径。数值越高意味着打印的范围越大。\n" +
                    "此参数与§6§l半径模式§r有关联。"
    );

    public static final ConfigInteger PUT_COOLING = new ConfigInteger(
            "放置冷却", 2, 0, 256,
            "当同一位置的方块没有成功放置时，等待§6所设置的时长§r(游戏刻为单位)才会再次放置。"
    );

    public static final ConfigBoolean PLACE_USE_PACKET = new ConfigBoolean(
            "使用数据包放置方块", false,
            "可以得到更快的放置速度，并且不会出现'幽灵方块'的情况。§6§l但是无法听到放置方块的声音。§r"
    );

    public static final ConfigBoolean SWITCH_ITEM_USE_PACKET = new ConfigBoolean(
            "使用数据包切换物品", false,
            "避免在开启§b使用数据包放置方块§r时因客户端同步不及时导致误放方块。\n" +
                    "对于不使用数据包放置且§6§l打印机工作间隔§r大于§61§r的用户无需开启。"
    );

    public static final ConfigOptionList RANGE_MODE = new ConfigOptionList(
            "半径模式", State.ListType.SPHERE,
            "立方体建议§63§r，球体建议设置§66§r，破基岩在立方体模式下无法正常使用。"
    );

    public static final ConfigOptionList MODE_SWITCH = new ConfigOptionList(
            "模式切换", State.ModeType.SINGLE,
            "单模：仅运行一个模式。多模：可多个模式同时运行。"
    );

    public static final ConfigOptionList PRINTER_MODE = new ConfigOptionList(
            "打印机模式", State.PrintModeType.PRINTER,
            "打印机的工作模式。"
    );

    public static final ConfigBoolean MULTI_BREAK = new ConfigBoolean(
            "多模阻断", true,
            "启用后将按模式优先级运行，同时启用多个模式时优先级低的无法执行。"
    );

    public static final ConfigBoolean RENDER_LAYER_LIMIT = new ConfigBoolean(
            "渲染层数限制", false,
            "根据§6Litematica§r的渲染层数限制来限制打印机的工作范围。"
    );

    public static final ConfigBoolean PRINT_IN_AIR = new ConfigBoolean(
            "凭空放置", true,
            "无视是否有方块面支撑，直接放置方块。"
    );

    public static final ConfigBooleanHotkeyed PRINT_WATER_LOGGED_BLOCK = new ConfigBooleanHotkeyed(
            "启用打印水",  false, "",
            "启用后会自动放置冰,破坏冰来生成水。"
    );

    public static final ConfigBooleanHotkeyed BREAK_ERROR_BLOCK = new ConfigBooleanHotkeyed(
            "破坏错误方块", false, "",
            "打印过程中自动破坏投影中错误的方块。"
    );

    public static final ConfigBoolean PRINT_SWITCH = new ConfigBoolean(
            "打印状态", false,
            "打印的开关状态。"
    );

    public static final ConfigBoolean EASY_MODE = new ConfigBoolean(
            "精准放置", false,
            "根据投影的设置使用对应的协议。"
    );

    public static final ConfigBooleanHotkeyed USE_EASY_MODE = new ConfigBooleanHotkeyed(
            "轻松放置模式", false, "",
            "启用后会调用轻松放置来进行放置，因为轻松放置本身会使用放置协议，" +
                    "所以在开启了这个功能后无需启用§6§l精准放置§r。"
    );

    public static final ConfigBoolean FORCED_PLACEMENT = new ConfigBoolean(
            "打印时潜行", false,
            "在打印过程中，会自动潜行以防止对方块进行交互。"
    );

    public static final ConfigBoolean REPLACE = new ConfigBoolean(
            "替换列表方块", true,
            "无视列表中的方块直接替换防止，例如:草、雪片等。"
    );

    public static final ConfigBoolean STRIP_LOGS = new ConfigBoolean(
            "自动去树皮", false,
            "在打印去皮原木的时候，会选择原木并用背包里的斧头进行去皮操作。"
    );

    public static final ConfigHotkey SWITCH_PRINTER_MODE = new ConfigHotkey(
            "切换工作模式", "J",
            "切换打印机工作模式。"
    );

    public static final ConfigBooleanHotkeyed BEDROCK_SWITCH = new ConfigBooleanHotkeyed(
            "破基岩", false, "",
            "切换为破基岩模式,此模式下y轴会从上往下判定。"
    );

    public static final ConfigBooleanHotkeyed EXCAVATE = new ConfigBooleanHotkeyed(
            "挖掘", false, "",
            "挖掘所选区内的方块。"
    );

    public static final ConfigBooleanHotkeyed FLUID = new ConfigBooleanHotkeyed(
            "排流体", false, "",
            "在岩浆源、水源处放方块默认是§6排流体方块名单§r里的方块。"
    );

    public static final ConfigHotkey CLOSE_ALL_MODE = new ConfigHotkey(
            "关闭全部模式", "LEFT_CONTROL,G",
            "关闭全部模式，若此时为单模模式将模式恢复为打印。"
    );

    //#if MC >= 12001
    public static final ConfigHotkey LAST = new ConfigHotkey(
            "上一个箱子", "", GUI_NO_ORDER, ""
    );

    public static final ConfigHotkey NEXT = new ConfigHotkey(
            "下一个箱子", "", GUI_NO_ORDER, ""
    );

    public static final ConfigHotkey DELETE = new ConfigHotkey(
            "删除当前容器", "", GUI_NO_ORDER, ""
    );
    //#endif

    public static final ConfigStringList FLUID_BLOCK_LIST = new ConfigStringList(
            "排流体方块名单", ImmutableList.of("minecraft:sand"),
            "可填入方块或物品ID，例如: minecraft:stone。\n" +
                    "如果在前面加上#，则会采用标签匹配，列表会根据标签名称智能匹配对应的方块，例如：§l#stone§r。\n" +
                    "如果同时传入文本和过滤条件（以英文逗号分隔），例如：§lstone,c§r。\n" +
                    "其中 “c” 表示使用包含匹配方式，会先尝试拼音或直接匹配。"
    );

    public static final ConfigBoolean PUT_SKIP = new ConfigBoolean(
            "跳过放置", false,
            "开启后会跳过列表内的方块"
    );

    public static final ConfigBoolean PUT_TESTING = new ConfigBoolean(
            "侦测器放置检测", false,
            "检测侦测器看向的方块是否和投影方块一致，若不一致测跳过放置"
    );

    public static final ConfigBoolean QUICK_SHULKER = new ConfigBoolean(
            "快捷潜影盒", false,
            "在服务器装有AxShulkers的情况下可以直接从背包内的潜影盒取出物品\n" +
                    "替换的位置为Litematica的预设位置,如果所有预设位置都有濳影盒则不会替换。"
    );

    public static final ConfigBoolean INVENTORY = new ConfigBoolean(
            "远程交互容器", false,
            "在服务器支持远程交互容器或单机的情况下可以远程交互\n" +
                    "替换的位置为投影的预设位置。"
    );

    public static final ConfigBoolean AUTO_INVENTORY = new ConfigBoolean(
            "自动设置远程交互", false,
            "在服务器若允许使用则自动开启远程交互容器，反之则自动关闭"
    );

    public static final ConfigBoolean PRINT_CHECK = new ConfigBoolean(
            "有序存放", false,
            "在背包满时将从快捷盒子或打印机库存中取出的物品还原到之前位置，" +
                    "关闭后将会打乱打印机以及濳影盒"
    );

    public static final ConfigStringList INVENTORY_LIST = new ConfigStringList(
            "库存白名单", ImmutableList.of("minecraft:chest"),
            "打印机库存的白名单，只有白名单内的容器才会被记录。"
    );

    public static final ConfigOptionList EXCAVATE_LIMITER = new ConfigOptionList(
            "挖掘模式限制器", State.ExcavateListMode.CUSTOM,
            "使用Tweakeroo挖掘限制预设或自定义挖掘限制预设。\n" +
                    "Tweakeroo预设：使用Tweakeroo的挖掘限制预设，" +
                    "自定义：使用自定义的挖掘限制预设，"
    );

    public static final ConfigOptionList EXCAVATE_LIMIT = new ConfigOptionList(
            "挖掘模式限制", UsageRestriction.ListType.NONE,
            "挖掘模式限制的过滤方式，有无限制、白名单、黑名单三种。\n" +
                    "无限制：不限制任何方块\n" +
                    "白名单：仅允许白名单内的方块被挖掘\n" +
                    "黑名单：禁止黑名单内的方块被挖掘\n" +
                    "§l使用Tweakeroo挖掘限制预设时，此项会被忽略。§r"
    );

    public static final ConfigStringList EXCAVATE_WHITELIST = new ConfigStringList(
            "挖掘白名单", ImmutableList.of(""),
            "可填入方块或物品ID，例如: minecraft:stone。\n" +
                    "如果在前面加上#，则会采用标签匹配，列表会根据标签名称智能匹配对应的方块，例如：§l#stone§r。\n" +
                    "如果同时传入文本和过滤条件（以英文逗号分隔），例如：§lstone,c§r。\n" +
                    "其中 “c” 表示使用包含匹配方式，会先尝试拼音或直接匹配。"
    );

    public static final ConfigStringList EXCAVATE_BLACKLIST = new ConfigStringList(
            "挖掘黑名单", ImmutableList.of(""),
            "可填入方块或物品ID，例如: minecraft:stone。\n" +
                    "如果在前面加上#，则会采用标签匹配，列表会根据标签名称智能匹配对应的方块，例如：§l#stone§r。\n" +
                    "如果同时传入文本和过滤条件（以英文逗号分隔），例如：§lstone,c§r。\n" +
                    "其中 “c” 表示使用包含匹配方式，会先尝试拼音或直接匹配。"
    );

    public static final ConfigStringList PUT_SKIP_LIST = new ConfigStringList(
            "跳过放置名单", ImmutableList.of(),
            "可填入方块或物品ID，例如: minecraft:stone。\n" +
                    "如果在前面加上#，则会采用标签匹配，列表会根据标签名称智能匹配对应的方块，例如：§l#stone§r。\n" +
                    "如果同时传入文本和过滤条件（以英文逗号分隔），例如：§lstone,c§r。\n" +
                    "其中 “c” 表示使用包含匹配方式，会先尝试拼音或直接匹配。"
    );

    public static final ConfigStringList BEDROCK_LIST = new ConfigStringList(
            "基岩模式白名单", ImmutableList.of("minecraft:bedrock"),
            "破基岩模式的白名单，只有白名单内的方块会被破坏。\n" +
                    "可填入方块或物品ID，例如: minecraft:stone。\n" +
                    "如果在前面加上#，则会采用标签匹配，列表会根据标签名称智能匹配对应的方块，例如：§l#stone§r。\n" +
                    "如果同时传入文本和过滤条件（以英文逗号分隔），例如：§lstone,c§r。\n" +
                    "其中 “c” 表示使用包含匹配方式，会先尝试拼音或直接匹配。"
    );

    public static final ConfigStringList REPLACEABLE_LIST = new ConfigStringList(
            "可替换方块", ImmutableList.of(
            "minecraft:snow", "minecraft:lava", "minecraft:water",
            "minecraft:bubble_column", "minecraft:short_grass"
    ),
            "打印时将忽略这些错误方块，直接在该位置打印。\n" +
                    "可填入方块或物品ID，例如: minecraft:stone。\n" +
                    "如果在前面加上#，则会采用标签匹配，列表会根据标签名称智能匹配对应的方块，例如：§l#stone§r。\n" +
                    "如果同时传入文本和过滤条件（以英文逗号分隔），例如：§lstone,c§r。\n" +
                    "其中 “c” 表示使用包含匹配方式，会先尝试拼音或直接匹配。"
    );

    public static final ConfigColor SYNC_INVENTORY_COLOR = new ConfigColor(
            "容器同步与打印机添加库存高亮颜色", "#4CFF4CE6",
            "给容器同步和打印机添加库存的方块添加高亮颜色\n" +
                    "如果不需要高亮颜色可以设置为§l#00000000§r。"
    );

    public static final ConfigBoolean REPLACE_CORAL = new ConfigBoolean(
            "替换珊瑚", false,
            "启用后打印失活珊瑚时也会自动使用活珊瑚替换。如果背包里同时有活珊瑚和失活珊瑚，则会优先使用失活珊瑚替换。"
    );

    public static final ConfigBoolean RENDER_PROGRESS = new ConfigBoolean(
            "显示打印进度", true,
            "在打印机工作时在HUD中下的位置显示打印进度。"
    );

    //========================================
    //              Hotkeys
    //========================================
    public static final ConfigHotkey PRINT = new ConfigHotkey(
            "打印", "", KeybindSettings.PRESS_ALLOWEXTRA_EMPTY,
            "在按着热键的时候打印方块"
    );

    public static final ConfigHotkey TOGGLE_PRINTING_MODE = new ConfigHotkey(
            "打印状态开关", "CAPS_LOCK", KeybindSettings.PRESS_ALLOWEXTRA_EMPTY,
            "切换打印状态，开启或关闭打印机的工作状态。"
    );

    public static final ConfigHotkey SYNC_INVENTORY = new ConfigHotkey(
            "容器同步", "",
            "按下热键后会记录看向容器的物品。\n" +
                    "将投影选区内的同类型容器中的物品，同步至记录的容器。"
    );

    public static final ConfigBooleanHotkeyed SYNC_INVENTORY_CHECK = new ConfigBooleanHotkeyed(
            "同步时检查背包", false, "",
            "容器同步时检查背包，如果填充物不足，则不会打开容器"
    );

    public static final ConfigHotkey PRINTER_INVENTORY = new ConfigHotkey(
            "打印机库存", "",
            "如果远程取物的目标是未加载的区块将会增加取物品的时间，用投影选区后按下热键\n" +
                    "打印机工作时将会使用该库存内的物品\n" +
                    "建议库存区域内放置假人来常加载区块"
    );

    public static final ConfigHotkey REMOVE_PRINT_INVENTORY = new ConfigHotkey(
            "清空打印机库存", "",
            "清空打印机库存"
    );

    //========================================
    //           Initialization
    //========================================
    @Override
    public void onInitialize() {
        reSetConfig();
        OpenInventoryPacket.init();
        OpenInventoryPacket.registerReceivePacket();
        OpenInventoryPacket.registerClientReceivePacket();

        //#if MC >= 12001
        if (loadChestTracker) {
            MemoryUtils.setup();
        }
        //#endif

        TOGGLE_PRINTING_MODE.getKeybind().setCallback(
                new KeyCallbackToggleBooleanConfigWithMessage(PRINT_SWITCH)
        );

        me.aleksilassila.litematica.printer.config.Configs.init();
        HighlightBlockRenderer.init();
    }

    @Override
    public void onInitializeClient() {
        // Client-side initialization
    }

    //========================================
    //           Utility Methods
    //========================================
    private void reSetConfig() {
        if (!loadChestTracker) {
            AUTO_INVENTORY.setBooleanValue(false);
            INVENTORY.setBooleanValue(false);
        }
    }

    public static ImmutableList<IConfigBase> getConfigList() {
        List<IConfigBase> list = new java.util.ArrayList<>(Configs.Generic.OPTIONS);
        list.add(PRINT_SWITCH);
        list.add(EASY_MODE);
        list.add(PRINT_INTERVAL);
        list.add(COMPULSION_RANGE);

        if (PRINTER_MODE.getOptionListValue().equals(State.ModeType.SINGLE)) {
            list.add(PRINTER_MODE);
        }

        if (EXCAVATE_LIMITER.getOptionListValue().equals(State.ExcavateListMode.CUSTOM)) {
            list.add(EXCAVATE_LIMIT);
            list.add(EXCAVATE_WHITELIST);
            list.add(EXCAVATE_BLACKLIST);
        }

        list.add(PRINT_IN_AIR);
        list.add(PRINT_WATER_LOGGED_BLOCK);
        list.add(BREAK_ERROR_BLOCK);

        return ImmutableList.copyOf(list);
    }

    public static List<IConfigBase> getHotkeyList() {
        List<IConfigBase> list = new java.util.ArrayList<>(Hotkeys.HOTKEY_LIST);
        list.add(PRINT);
        list.add(TOGGLE_PRINTING_MODE);
        list.add(CLOSE_ALL_MODE);

        if (MODE_SWITCH.getOptionListValue() == State.ModeType.SINGLE) {
            list.add(SWITCH_PRINTER_MODE);
        }

        return ImmutableList.copyOf(list);
    }

    public static ImmutableList<IConfigBase> getColorsList() {
        List<IConfigBase> list = new java.util.ArrayList<>(Configs.Colors.OPTIONS);
        list.add(SYNC_INVENTORY_COLOR);
        return ImmutableList.copyOf(list);
    }
}