package me.aleksilassila.litematica.printer;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import me.aleksilassila.litematica.printer.bilixwhite.utils.BedrockUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.config.options.*;
import me.aleksilassila.litematica.printer.hotkeys.KeyCallbackToggleBooleanConfigWithMessage;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.State;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.HighlightBlockRenderer;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

import java.util.List;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics.loadChestTracker;

public class LitematicaPrinterMod implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "litematica_printer";
    public static final String I18N_PREFIX = MOD_ID + ".config";

    //========================================
    //     Config Settings (配置设置)
    //========================================
    public static final OptionInteger PRINTER_SPEED = OptionInteger.create(I18n.PRINTER_SPEED, 1, 0, 20);
    public static final OptionInteger BLOCKS_PER_TICK = OptionInteger.create(I18n.PRINTER_BLOCKS_PER_TICK, 4, 0, 24);
    public static final OptionInteger PLACE_COOLDOWN = OptionInteger.create(I18n.PRINTER_PLACE_COOLDOWN, 3, 0, 64);
    public static final OptionInteger PRINTER_RANGE = OptionInteger.create(I18n.PRINTER_RANGE, 6, 1, 256);
    public static final OptionInteger QUICK_SHULKER_COOLDOWN = OptionInteger.create(I18n.PRINTER_QUICK_SHULKER_COOLDOWN, 10, 0, 20);
    public static final OptionInteger ITERATOR_USE_TIME = OptionInteger.create(I18n.PRINTER_ITERATOR_USE_TIME, 8, 0, 128);

    public static final OptionBoolean PLACE_USE_PACKET = OptionBoolean.create(I18n.PRINTER_USE_PACKET, false);
    public static final OptionBoolean STRIP_LOGS = OptionBoolean.create(I18n.PRINTER_AUTO_STRIP_LOGS, false);
    public static final OptionBoolean QUICK_SHULKER = OptionBoolean.create(I18n.PRINTER_QUICK_SHULKER, false);
    public static final OptionBoolean LAG_CHECK = OptionBoolean.create(I18n.PRINTER_LAG_CHECK, true);
    public static final OptionBoolean X_REVERSE = OptionBoolean.create(I18n.PRINTER_X_AXIS_REVERSE, false);
    public static final OptionBoolean Y_REVERSE = OptionBoolean.create(I18n.PRINTER_Y_AXIS_REVERSE, false);
    public static final OptionBoolean Z_REVERSE = OptionBoolean.create(I18n.PRINTER_Z_AXIS_REVERSE, false);
    public static final OptionBoolean FALLING_CHECK = OptionBoolean.create(I18n.PRINTER_FALLING_BLOCK_CHECK, true);
    public static final OptionBoolean BREAK_WRONG_BLOCK = OptionBoolean.create(I18n.PRINT_BREAK_WRONG_BLOCK, false);
    public static final OptionBoolean DEBUG_OUTPUT = OptionBoolean.create(I18n.DEBUG_OUTPUT, false);
    public static final OptionBoolean NOTE_BLOCK_TUNING = OptionBoolean.create(I18n.PRINTER_AUTO_TUNING, true);
    public static final OptionBoolean SAFELY_OBSERVER = OptionBoolean.create(I18n.PRINTER_SAFELY_OBSERVER, true);
    public static final OptionBoolean BREAK_EXTRA_BLOCK = OptionBoolean.create(I18n.PRINTER_BREAK_EXTRA_BLOCK, false);
    public static final OptionBoolean BREAK_WRONG_STATE_BLOCK = OptionBoolean.create(I18n.PRINTER_BREAK_WRONG_STATE_BLOCK, false);
    public static final OptionBoolean SKIP_WATERLOGGED_BLOCK = OptionBoolean.create(I18n.PRINTER_SKIP_WATERLOGGED, false);
    public static final OptionBoolean UPDATE_CHECK = OptionBoolean.create(I18n.UPDATE_CHECK, true);
    public static final OptionBoolean FILL_COMPOSTER = OptionBoolean.create(I18n.PRINTER_AUTO_FILL_COMPOSTER, false);
    public static final OptionBoolean FILL_FLOWING_FLUID = OptionBoolean.create(I18n.FLUID_MODE_FILL_FLOWING, true);
    public static final OptionBoolean AUTO_DISABLE_PRINTER = OptionBoolean.create(I18n.PRINTER_AUTO_DISABLE, true);
    public static final OptionBoolean EASYPLACE_PROTOCOL = OptionBoolean.create(I18n.EASY_PLACE_PROTOCOL, false);

    public static final OptionList ITERATOR_SHAPE = OptionList.create(I18n.PRINTER_ITERATOR_SHAPE, State.RadiusShapeType.SPHERE);
    public static final OptionList QUICK_SHULKER_MODE = OptionList.create(I18n.PRINTER_QUICK_SHULKER_MODE, State.QuickShulkerModeType.INVOKE);
    public static final OptionList ITERATION_ORDER = OptionList.create(I18n.PRINTER_ITERATOR_MODE, State.IterationOrderType.XZY);
    public static final OptionList FILL_BLOCK_FACING = OptionList.create(I18n.FILL_MODE_FACING, State.FillModeFacingType.DOWN);

    public static final OptionStringList FLUID_BLOCK_LIST = OptionStringList.create(I18n.FLUID_BLOCK_LIST, ImmutableList.of("minecraft:sand"));
    public static final OptionStringList FLUID_LIST = OptionStringList.create(I18n.FLUID_LIST, ImmutableList.of("minecraft:water", "minecraft:lava"));
    public static final OptionStringList FILL_BLOCK_LIST = OptionStringList.create(I18n.FILL_BLOCK_LIST, ImmutableList.of("minecraft:cobblestone"));
    public static final OptionStringList INVENTORY_LIST = OptionStringList.create(
            I18n.INVENTORY_LIST,
            ImmutableList.of("minecraft:chest"),
            "\n",
            StringUtils.literal("打印机库存的白名单，只有白名单内的容器才会被记录。"),
            I18n.INVENTORY_LIST.getConfigComment()
    );
    public static final OptionStringList EXCAVATE_WHITELIST = OptionStringList.create(I18n.EXCAVATE_WHITELIST, ImmutableList.of(""));
    public static final OptionStringList EXCAVATE_BLACKLIST = OptionStringList.create(I18n.EXCAVATE_BLACKLIST, ImmutableList.of(""));
    public static final OptionStringList PUT_SKIP_LIST = OptionStringList.create(I18n.PUT_SKIP_LIST, ImmutableList.of(""));
    public static final OptionStringList REPLACEABLE_LIST = OptionStringList.create(
            I18n.REPLACEABLE_LIST,
            ImmutableList.of(
                    "minecraft:snow", "minecraft:lava", "minecraft:water",
                    "minecraft:bubble_column", "minecraft:short_grass"
            ),
            "\n",
            StringUtils.literal("打印时将忽略这些错误方块，直接在该位置打印。"),
            I18n.REPLACEABLE_LIST.getConfigComment()
    );


    public static final OptionList MODE_SWITCH = OptionList.create(I18n.MODE_SWITCH, State.ModeType.SINGLE);
    public static final OptionList PRINTER_MODE = OptionList.create(I18n.PRINTER_MODE, State.PrintModeType.PRINTER);

    public static final OptionBoolean MULTI_BREAK = OptionBoolean.create(I18n.MULTI_BREAK, true);
    public static final OptionBoolean RENDER_LAYER_LIMIT = OptionBoolean.create(I18n.RENDER_LAYER_LIMIT, false);
    public static final OptionBoolean PRINT_IN_AIR = OptionBoolean.create(I18n.PRINT_IN_AIR, true);

    public static final OptionBooleanHotkeyed PRINT_WATER = OptionBooleanHotkeyed.create(I18n.PRINT_WATER, false);
    public static final OptionBoolean PRINT_SWITCH = OptionBoolean.create(I18n.PRINT_SWITCH, false);
    public static final OptionBooleanHotkeyed USE_EASYPLACE = OptionBooleanHotkeyed.create(I18n.USE_EASYPLACE, false);
    public static final OptionBoolean FORCED_SNEAK = OptionBoolean.create(I18n.FORCED_SNEAK, false);
    public static final OptionBoolean REPLACE = OptionBoolean.create(I18n.REPLACE, true);

    public static final OptionHotkey SWITCH_PRINTER_MODE = OptionHotkey.create(I18n.SWITCH_PRINTER_MODE);
    public static final OptionBooleanHotkeyed MINE = OptionBooleanHotkeyed.create(I18n.MINE, false);
    public static final OptionBooleanHotkeyed FLUID = OptionBooleanHotkeyed.create(I18n.FLUID, false);
    public static final OptionBooleanHotkeyed FILL = OptionBooleanHotkeyed.create(I18n.FILL, false);
    public static final OptionBooleanHotkeyed BEDROCK = OptionBooleanHotkeyed.create(I18n.BEDROCK, false);
    public static final OptionHotkey CLOSE_ALL_MODE = OptionHotkey.create(I18n.CLOSE_ALL_MODE, "LEFT_CONTROL,G");
    public static final OptionBoolean PUT_SKIP = OptionBoolean.create(I18n.PUT_SKIP, false);
    public static final OptionBoolean CLOUD_INVENTORY = OptionBoolean.create(I18n.CLOUD_INVENTORY, false);
    public static final OptionBoolean AUTO_INVENTORY = OptionBoolean.create(I18n.AUTO_INVENTORY, false);
    public static final OptionBoolean STORE_ORDERLY = OptionBoolean.create(I18n.STORE_ORDERLY, false, "\n",
            I18n.STORE_ORDERLY.getConfigComment()
            //#if MC == 11802
            //$$ ,StringUtils.literal("在1.18.2版本表现不好，可能会导致卡顿，建议关闭。")
            //#endif
    );

    public static final OptionList EXCAVATE_LIMITER = OptionList.create(I18n.EXCAVATE_LIMITER, State.ExcavateListMode.CUSTOM);
    public static final OptionList EXCAVATE_LIMIT = OptionList.create(I18n.EXCAVATE_LIMIT, UsageRestriction.ListType.NONE);
    public static final OptionColor SYNC_INVENTORY_COLOR = OptionColor.create(I18n.SYNC_INVENTORY_COLOR, "#4CFF4CE6");
    public static final OptionBoolean REPLACE_CORAL = OptionBoolean.create(I18n.REPLACE_CORAL, false);
    public static final OptionBoolean RENDER_HUD = OptionBoolean.create(I18n.RENDER_HUD, false);

    //========================================
    //                  热键
    //========================================
    public static final OptionHotkey PRINT = OptionHotkey.create(I18n.PRINT, KeybindSettings.PRESS_ALLOWEXTRA_EMPTY);
    public static final OptionHotkey TOGGLE_PRINTING_MODE = OptionHotkey.create(I18n.TOGGLE_PRINTING_MODE, "CAPS_LOCK", KeybindSettings.PRESS_ALLOWEXTRA_EMPTY);
    public static final OptionHotkey SYNC_INVENTORY = OptionHotkey.create(I18n.SYNC_INVENTORY);
    public static final OptionBooleanHotkeyed SYNC_INVENTORY_CHECK = OptionBooleanHotkeyed.create(I18n.SYNC_INVENTORY_CHECK, false);
    public static final OptionHotkey PRINTER_INVENTORY = OptionHotkey.create(I18n.PRINTER_INVENTORY);
    public static final OptionHotkey REMOVE_PRINT_INVENTORY = OptionHotkey.create(I18n.REMOVE_PRINT_INVENTORY);

    //#if MC >= 12001
    private static final KeybindSettings GUI_NO_ORDER = KeybindSettings.create(
            KeybindSettings.Context.GUI, KeyAction.PRESS, false, false, false, true
    );
    public static final OptionHotkey LAST = OptionHotkey.create(I18n.LAST, GUI_NO_ORDER);
    public static final OptionHotkey NEXT = OptionHotkey.create(I18n.NEXT, GUI_NO_ORDER);
    public static final OptionHotkey DELETE = OptionHotkey.create(I18n.DELETE, GUI_NO_ORDER);
    //#endif

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

    //========================================
    //           Initialization
    //========================================
    @Override
    public void onInitialize() {
        reSetConfig();
        OpenInventoryPacket.init();
        OpenInventoryPacket.registerReceivePacket();
        OpenInventoryPacket.registerClientReceivePacket();

        //#if MC >= 12001 && MC <= 12104
        //$$ if (loadChestTracker) {
        //$$     me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils.setup();
        //$$ }
        //#endif


        TOGGLE_PRINTING_MODE.getKeybind().setCallback(
                new KeyCallbackToggleBooleanConfigWithMessage(PRINT_SWITCH)
        );

        PRINT_SWITCH.setValueChangeCallback(b -> {
            if (!b.getBooleanValue()) {
                Printer.getPrinter().basePos = null;
                Printer.getPrinter().clearQueue();
                Printer.pistonNeedFix = false;
                Printer.requiredState = null;
                if (Statistics.loadBedrockMiner) {
                    if (BedrockUtils.isWorking()) {
                        BedrockUtils.setWorking(false);
                        BedrockUtils.setBedrockMinerFeatureEnable(true);
                    }
                }
            }
        });

        Configs.init();
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

