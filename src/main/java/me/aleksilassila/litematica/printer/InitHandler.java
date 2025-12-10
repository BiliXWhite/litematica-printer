package me.aleksilassila.litematica.printer;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.*;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeyCallbackToggleBooleanConfigWithMessage;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.util.restrictions.UsageRestriction;
import me.aleksilassila.litematica.printer.bilixwhite.utils.BedrockUtils;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.config.InputHandler;
import me.aleksilassila.litematica.printer.printer.Printer;
import me.aleksilassila.litematica.printer.printer.State;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.HighlightBlockRenderer;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics;

import java.util.ArrayList;
import java.util.List;

import static me.aleksilassila.litematica.printer.config.ConfigFactory.*;

public class InitHandler implements IInitializationHandler {
    // @formatter:off

    public static final ConfigStringList FLUID_BLOCK_LIST = stringList(I18n.FLUID_BLOCK_LIST, ImmutableList.of("minecraft:sand"));
    public static final ConfigStringList FLUID_LIST = stringList(I18n.FLUID_LIST, ImmutableList.of("minecraft:water", "minecraft:lava"));

    public static final ConfigOptionList FILL_BLOCK_MODE = optionList(I18n.FILL_BLOCK_MODE, State.FileBlockModeType.WHITELIST);
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

    public static final ConfigOptionList ITERATOR_SHAPE     = optionList(I18n.PRINTER_ITERATOR_SHAPE    , State.RadiusShapeType.SPHERE);
    public static final ConfigOptionList QUICK_SHULKER_MODE = optionList(I18n.PRINTER_QUICK_SHULKER_MODE, State.QuickShulkerModeType.INVOKE);
    public static final ConfigOptionList ITERATION_ORDER    = optionList(I18n.PRINTER_ITERATOR_MODE     , State.IterationOrderType.XZY);
    public static final ConfigOptionList FILL_BLOCK_FACING  = optionList(I18n.FILL_MODE_FACING          , State.FillModeFacingType.DOWN);

    public static final ConfigOptionList MODE_SWITCH        = optionList(I18n.MODE_SWITCH               , State.ModeType.SINGLE);
    public static final ConfigOptionList PRINTER_MODE       = optionList(I18n.PRINTER_MODE              , State.PrintModeType.PRINTER);
    public static final ConfigOptionList EXCAVATE_LIMITER   = optionList(I18n.EXCAVATE_LIMITER          , State.ExcavateListMode.CUSTOM);
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
    public static final ConfigBoolean EASYPLACE_PROTOCOL        = bool(I18n.EASY_PLACE_PROTOCOL             , false);
    public static final ConfigBoolean MULTI_BREAK               = bool(I18n.MULTI_BREAK                     , true );
    public static final ConfigBoolean RENDER_LAYER_LIMIT        = bool(I18n.RENDER_LAYER_LIMIT              , false);
    public static final ConfigBoolean PRINT_IN_AIR              = bool(I18n.PRINT_IN_AIR                    , true );
    public static final ConfigBoolean PRINT_SWITCH              = bool(I18n.PRINT_SWITCH                    , false);
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
    public static final ConfigBooleanHotkeyed USE_EASYPLACE         = booleanHotkey(I18n.USE_EASYPLACE            ,false);
    public static final ConfigBooleanHotkeyed MINE                  = booleanHotkey(I18n.MINE                     ,false);
    public static final ConfigBooleanHotkeyed FLUID                 = booleanHotkey(I18n.FLUID                    ,false);
    public static final ConfigBooleanHotkeyed FILL                  = booleanHotkey(I18n.FILL                     ,false);
    public static final ConfigBooleanHotkeyed BEDROCK               = booleanHotkey(I18n.BEDROCK                  ,false);
    public static final ConfigBooleanHotkeyed SYNC_INVENTORY_CHECK  = booleanHotkey(I18n.SYNC_INVENTORY_CHECK     ,false);


    public static final ConfigHotkey OPEN_SCREEN            = hotkey(I18n.OPEN_SCREEN, "Z,Y");
    public static final ConfigHotkey PRINT                  = hotkey(I18n.PRINT, KeybindSettings.PRESS_ALLOWEXTRA_EMPTY);
    public static final ConfigHotkey TOGGLE_PRINTING_MODE   = hotkey(I18n.TOGGLE_PRINTING_MODE, "CAPS_LOCK", KeybindSettings.PRESS_ALLOWEXTRA_EMPTY);
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


    @Override
    public void registerModHandlers() {
        // 箱子追踪
        if (!Statistics.loadChestTracker) {
            AUTO_INVENTORY.setBooleanValue(false);  // 自动设置远程交互
            CLOUD_INVENTORY.setBooleanValue(false); // 远程交互容器
        }

        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerKeyboardInputHandler(InputHandler.getInstance());

        //#if MC >= 12001 && MC <= 12104
        //$$ if (Statistics.loadChestTracker) {
        //$$     me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils.setup();
        //$$ }
        //#endif

        // 切换打印状态热键
        TOGGLE_PRINTING_MODE.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(PRINT_SWITCH));

        // 打印状态值被修改
        PRINT_SWITCH.setValueChangeCallback(b -> {
            if (!b.getBooleanValue()) {
                Printer.getInstance().basePos = null;
                Printer.getInstance().clearQueue();
                Printer.getInstance().pistonNeedFix = false;
                Printer.getInstance().requiredState = null;
                if (Statistics.loadBedrockMiner) {
                    if (BedrockUtils.isWorking()){
                        BedrockUtils.setWorking(false);
                        BedrockUtils.setBedrockMinerFeatureEnable(true);
                    }
                }
            }
        });

        Configs.init();
        HighlightBlockRenderer.init();
    }


    public static List<IConfigBase> getHotkeyList() {
        List<IConfigBase> list = new ArrayList<>(Hotkeys.HOTKEY_LIST);
        list.add(PRINT);
        list.add(TOGGLE_PRINTING_MODE);
        list.add(CLOSE_ALL_MODE);
        if (MODE_SWITCH.getOptionListValue() == State.ModeType.SINGLE) {
            list.add(SWITCH_PRINTER_MODE);
        }
        return ImmutableList.copyOf(list);
    }

}

