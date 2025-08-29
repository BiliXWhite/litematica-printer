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

import static me.aleksilassila.litematica.printer.bilixwhite.utils.I18nUtils.*;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics.loadChestTracker;

public class LitematicaMixinMod implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "litematica_printer";
    public static final String I18N_PREFIX = MOD_ID + ".config";
    //========================================
    //           Config Settings
    //========================================
    public static final ConfigInteger PRINTER_SPEED = new ConfigInteger(
            "printerSpeed", 1, 0, 20, "printerSpeed"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;
    public static final ConfigInteger BLOCKS_PER_TICK = new ConfigInteger(
            "printerBlocksPerTick", 4, 0, 24, "printerBlocksPerTick"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;
    public static final ConfigInteger PLACE_COOLDOWN = new ConfigInteger(
            "printerPlaceCooldown", 2, 0, 256, "printerPlaceCooldown"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;
    public static final ConfigInteger PRINTER_RANGE = new ConfigInteger(
            "printerRange", 6, 1, 256, "printerRange"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;
    public static final ConfigBoolean PLACE_USE_PACKET = new ConfigBoolean(
            "printerUsePacket", false, "printerUsePacket"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;
    public static final ConfigOptionList RANGE_SHAPE = new ConfigOptionList(
            "printerIteratorShape", State.RadiusShapeType.SPHERE, "printerIteratorShape"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;
    public static final ConfigOptionList MODE_SWITCH = new ConfigOptionList(
            "模式切换", State.ModeType.SINGLE,
            "§d单模§r：仅运行一个模式。\n§d多模§r：可多个模式同时运行。\n" +
                    "切换后需要重新打开设置菜单才能显示模式的。"
    );
    public static final ConfigOptionList PRINTER_MODE = new ConfigOptionList(
            "打印机模式", State.PrintModeType.PRINTER,
            "打印机的工作模式。\n" +
                    "§d打印§r：还原投影里的方块，包括方块的部分数据(如方向，开关等)。\n" +
                    "§d挖掘§r：挖掘选区范围内的方块，不包括液体。\n" +
                    "§d基岩§r：利用bug破坏选区范围内的基岩，可能只在特定条件下有效。\n" +
                    "§d流体§r：在选区范围内放置方块，流体会被替换为§6排流体方块名单§r里的方块。\n" +
                    "§d填充§r：在范围内打印§6填充方块列表§r里的方块。\n" +
                    "\n§b§lTips:§r 除了§d打印§r模式外，操作区域范围是基于Litematica的选区范围进行的。"
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
    public static final ConfigBooleanHotkeyed PRINT_WATER = new ConfigBooleanHotkeyed(
            "printWater",  false, "", "printWater"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;
    public static final ConfigBoolean PRINT_SWITCH = new ConfigBoolean(
            "打印状态", false,
            "打印的开关状态。"
    );
    public static final ConfigBoolean EASYPLACE_PROTOCOL = new ConfigBoolean(
            "easyPlaceProtocol", false, "easyPlaceProtocol"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;
    public static final ConfigBooleanHotkeyed USE_EASYPLACE = new ConfigBooleanHotkeyed(
            "轻松放置模式", false, "",
            "启用后会调用轻松放置来进行放置，因为轻松放置本身会使用放置协议，" +
                    "所以在开启了这个功能后无需启用§6§l精准放置§r。"
    );
    public static final ConfigBoolean FORCED_SNEAK = new ConfigBoolean(
            "打印时潜行", false,
            "在打印过程中，会自动潜行以防止对方块进行交互。"
    );
    public static final ConfigBoolean REPLACE = new ConfigBoolean(
            "覆盖打印", true,
            "无视§6§l覆盖方块列表§r中的方块直接进行打印，例如:草、雪片等。"
    );
    public static final ConfigBoolean STRIP_LOGS = new ConfigBoolean(
            "自动去树皮", false,
            "在打印去皮原木的时候，会选择原木并用背包里的斧头进行去皮操作。"
    );
    public static final ConfigHotkey SWITCH_PRINTER_MODE = new ConfigHotkey(
            "切换模式", "",
            "切换打印机的工作模式。"
    );
    public static final ConfigBooleanHotkeyed EXCAVATE = new ConfigBooleanHotkeyed(
            "挖掘", false, "",
            "挖掘所选区内的方块。"
    );
    public static final ConfigBooleanHotkeyed FLUID = new ConfigBooleanHotkeyed(
            "排流体", false, "",
            "在岩浆源、水源处放方块默认是§6排流体方块名单§r里的方块。"
    );
    public static final ConfigBooleanHotkeyed FILL = new ConfigBooleanHotkeyed(
            "填充", false, "",
            "在选区范围内放置§6填充方块名单§r里的方块。"
    );
    public static final ConfigHotkey CLOSE_ALL_MODE = new ConfigHotkey(
            "关闭全部模式", "LEFT_CONTROL,G",
            "关闭全部模式，若此时为单模模式将模式恢复为打印。"
    );
    public static final ConfigStringList FLUID_BLOCK_LIST = new ConfigStringList(
            "排流体方块名单", ImmutableList.of("minecraft:sand"), getComment("blocklist")
    );
    public static final ConfigStringList FILL_BLOCK_LIST = new ConfigStringList(
            "填充方块名单", ImmutableList.of("minecraft:cobblestone"), getComment("blocklist")
    );
    public static final ConfigBoolean PUT_SKIP = new ConfigBoolean(
            "跳过放置", false,
            "开启后会§6§l跳过放置列表§r内的方块。"
    );
    public static final ConfigBoolean QUICK_SHULKER = new ConfigBoolean(
            "quickShulker", false, "quickShulker"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;
    public static final ConfigOptionList QUICK_SHULKER_MODE = new ConfigOptionList(
            "quickShulkerMode", State.QuickShulkerModeType.CLICK_SLOT, "quickShulkerMode"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;
    public static final ConfigInteger QUICK_SHULKER_COOLDOWN = new ConfigInteger(
            "quickShulkerCooldown", 10, 0, 20, "quickShulkerCooldown"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;
    public static final ConfigBoolean CLOUD_INVENTORY = new ConfigBoolean(
            "远程交互容器", false,
            "在服务器支持远程交互容器或单机的情况下可以远程交互\n" +
                    "替换的位置为投影的预设位置。"
    );
    public static final ConfigBoolean AUTO_INVENTORY = new ConfigBoolean(
            "自动设置远程交互", false,
            "在服务器若允许使用则自动开启远程交互容器，反之则自动关闭"
    );
    public static final ConfigBoolean STORE_ORDERLY = new ConfigBoolean(
            "有序存放", false,
            "在背包满时尝试将从远程交互容器或快捷潜影盒中取出的物品还原到之前位置。"
                    //#if MC == 11802
                    + "\n在1.18.2版本表现不好，可能会会导致卡顿，建议关闭。"
                    //#endif
    );
    public static final ConfigStringList INVENTORY_LIST = new ConfigStringList(
            "库存白名单", ImmutableList.of("minecraft:chest"),
            "打印机库存的白名单，只有白名单内的容器才会被记录。\n" + getComment("blocklist")
    );
    public static final ConfigOptionList EXCAVATE_LIMITER = new ConfigOptionList(
            "挖掘模式限制器", State.ExcavateListMode.CUSTOM,
            "使用Tweakeroo挖掘限制预设或自定义挖掘限制预设。\n" +
                    "§dTweakeroo预设§r：使用Tweakeroo的挖掘限制预设，" +
                    "§d自定义§r：使用自定义的挖掘限制预设。"
    );
    public static final ConfigOptionList EXCAVATE_LIMIT = new ConfigOptionList(
            "挖掘模式限制", UsageRestriction.ListType.NONE,
            "挖掘模式限制的过滤方式，有无限制、白名单、黑名单三种。\n" +
                    "§d无限制§r：不限制任何方块\n" +
                    "§d白名单§r：仅允许白名单内的方块被挖掘\n" +
                    "§d黑名单§r：禁止黑名单内的方块被挖掘\n" +
                    "§l使用Tweakeroo挖掘限制预设时，此项会被忽略。§r"
    );
    public static final ConfigStringList EXCAVATE_WHITELIST = new ConfigStringList(
            "挖掘白名单", ImmutableList.of(""), getComment("blocklist")
    );
    public static final ConfigStringList EXCAVATE_BLACKLIST = new ConfigStringList(
            "挖掘黑名单", ImmutableList.of(""), getComment("blocklist")
    );
    public static final ConfigStringList PUT_SKIP_LIST = new ConfigStringList(
            "跳过放置名单", ImmutableList.of(), getComment("blocklist")
    );
    public static final ConfigStringList REPLACEABLE_LIST = new ConfigStringList(
            "覆盖方块列表", ImmutableList.of(
            "minecraft:snow", "minecraft:lava", "minecraft:water",
            "minecraft:bubble_column", "minecraft:short_grass"
    ),
            "打印时将忽略这些错误方块，直接在该位置打印。\n" +
                    getComment("blocklist")
    );
    public static final ConfigColor SYNC_INVENTORY_COLOR = new ConfigColor(
            "容器同步与打印机添加库存高亮颜色", "#4CFF4CE6",
            "给容器同步和打印机添加库存的方块添加高亮颜色\n" +
                    "如果不需要高亮颜色可以设置为§l#00000000§r。"
    );
    public static final ConfigBoolean REPLACE_CORAL = new ConfigBoolean(
            "替换珊瑚", false,
            "启用后打印失活珊瑚时，如果背包里没有失活珊瑚，会自动使用活珊瑚替换。如果背包里同时有活珊瑚和失活珊瑚，则会优先使用失活珊瑚打印。"
    );
    public static final ConfigBoolean RENDER_HUD = new ConfigBoolean(
            "显示打印HUD", false,
            "在打印机工作时显示工作模式和处理的方块（排流体与填充除外）。\n在开启§6§l延迟检测§r时也显示接收数据包间的延迟。"
    );
    public static final ConfigBoolean LAG_CHECK = new ConfigBoolean(
            "延迟检测", true,
            "打印机工作时会检测数据包的接收时间，如果超过20个游戏刻仍没收到新数据包，打印机会自动暂停，直到收到数据包后再继续。"
    );
    public static final ConfigOptionList ITERATION_ORDER = new ConfigOptionList(
            "iteratorMode", State.IterationOrderType.XZY, "iteratorMode"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;
    public static final ConfigBoolean X_REVERSE = new ConfigBoolean(
            "xReverse", false, "xReverse"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;
    public static final ConfigBoolean Y_REVERSE = new ConfigBoolean(
            "yReverse", false, "yReverse"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;
    public static final ConfigBoolean Z_REVERSE = new ConfigBoolean(
            "zReverse", false, "zReverse"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;
    public static final ConfigBoolean FALLING_CHECK = new ConfigBoolean(
            "fallingBlockCheck", true, "fallingBlockCheck"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;
    public static final ConfigBoolean BREAK_WRONG_BLOCK = new ConfigBoolean(
            "breakWrongBlock", false, "breakWrongBlock"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;
    public static final ConfigBoolean DEBUG_OUTPUT = new ConfigBoolean(
            "debugOutput", false,
            "debugOutput"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;
    public static final ConfigBoolean NOTE_BLOCK_TUNING = new ConfigBoolean(
            "autoTuning", true, "autoTuning"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;
    public static final ConfigBoolean SAFELY_OBSERVER = new ConfigBoolean(
            "safelyObserver", true, "safelyObserver"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;
    public static final ConfigBoolean BREAK_EXTRA_BLOCK = new ConfigBoolean(
            "breakExtraBlock", false, "breakExtraBlock"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;

    public static final ConfigBoolean SKIP_WATERLOGGED_BLOCK = new ConfigBoolean(
            "printSkipWaterlogged", false, "printSkipWaterlogged"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;

    public static final ConfigInteger ITERATOR_USE_TIME = new ConfigInteger(
            "iteratorUseTime", 4, 0 , 128, "iteratorUseTime"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;

    public static final ConfigBoolean UPDATE_CHECK = new ConfigBoolean(
            "updateCheck", true, "updateCheck"
    )
            //#if MC > 12006
            .apply(I18N_PREFIX)
            //#endif
            ;
    //========================================
    //                  热键
    //========================================
    public static final ConfigHotkey PRINT = new ConfigHotkey(
            "打印", "", KeybindSettings.PRESS_ALLOWEXTRA_EMPTY,
            "在按着绑定按键的时候打印方块。"
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
    private static final KeybindSettings GUI_NO_ORDER = KeybindSettings.create(
            KeybindSettings.Context.GUI, KeyAction.PRESS, false, false, false, true
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

    public static ImmutableList<IConfigBase> getConfigList() {
        List<IConfigBase> list = new java.util.ArrayList<>(Configs.Generic.OPTIONS);
        list.add(PRINT_SWITCH);
        list.add(PRINTER_SPEED);
        if (PRINTER_SPEED.getIntegerValue() == 0) list.add(BLOCKS_PER_TICK);
        list.add(PRINTER_RANGE);
        list.add(PLACE_COOLDOWN);
        list.add(PLACE_USE_PACKET);
        //list.add(RENDER_PROGRESS);
        list.add(QUICK_SHULKER);
        list.add(QUICK_SHULKER_MODE);
        list.add(QUICK_SHULKER_COOLDOWN);
        list.add(LAG_CHECK);
        list.add(ITERATION_ORDER);
        list.add(X_REVERSE);
        list.add(Y_REVERSE);
        list.add(Z_REVERSE);

        if (PRINTER_MODE.getOptionListValue().equals(State.ModeType.SINGLE)) {
            list.add(PRINTER_MODE);
        }

        if (EXCAVATE_LIMITER.getOptionListValue().equals(State.ExcavateListMode.CUSTOM)) {
            list.add(EXCAVATE_LIMIT);
            list.add(EXCAVATE_WHITELIST);
            list.add(EXCAVATE_BLACKLIST);
        }

        list.add(PRINT_IN_AIR);
        list.add(PRINT_WATER);

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
            CLOUD_INVENTORY.setBooleanValue(false);
        }
    }
}

