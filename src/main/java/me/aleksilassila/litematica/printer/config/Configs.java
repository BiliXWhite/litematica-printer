package me.aleksilassila.litematica.printer.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.*;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.util.JsonUtils;
import me.aleksilassila.litematica.printer.printer.State;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static me.aleksilassila.litematica.printer.LitematicaMixinMod.*;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics.loadChestTracker;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics.loadQuickShulker;

public class Configs implements IConfigHandler {
    public static Configs INSTANCE = new Configs();
    private static final String FILE_PATH = "./config/" + MOD_ID + ".json";
    private static final File CONFIG_DIR = new File("./config");
    //mod
    public static final ConfigHotkey PRINTER = new ConfigHotkey( "打开设置菜单", "Z,Y","");

    public static final ImmutableList<IConfigBase> GENERAL = addGeneral();
    public static ImmutableList<IConfigBase> addGeneral(){
        List<IConfigBase> list = new ArrayList<>();
        if(loadChestTracker) list.add(INVENTORY);
        if(loadQuickShulker) list.add(QUICKSHULKER);
        list.add(PRINT_SWITCH);
        list.add(PRINT_INTERVAL);
        list.add(PRINTING_RANGE);
        list.add(COMPULSION_RANGE);
        list.add(RANGE_MODE);
        list.add(MODE_SWITCH);
        if(MODE_SWITCH.getOptionListValue().equals(State.ModeType.SINGLE)) list.add(PRINTER_MODE);
        list.add(RENDER_LAYER_LIMIT);
        list.add(FLUID_BLOCK_LIST);
        if(loadChestTracker) list.add(INVENTORY_LIST);
        list.add(BEDROCK_LIST);

        return ImmutableList.copyOf(list);
    }
    public static final ImmutableList<IConfigBase> PUT = addPut();
    public static ImmutableList<IConfigBase> addPut(){
        List<IConfigBase> list = new ArrayList<>();

        list.add(SKIP);
        list.add(EASY_MODE);
        list.add(FORCED_PLACEMENT);
        list.add(PRINT_IN_AIR);
        list.add(PRINT_WATER_LOGGED_BLOCK);
        list.add(REPLACE);
        list.add(REPLACEABLE_LIST);
        list.add(STRIP_LOGS);

        return ImmutableList.copyOf(list);
    }

    public static final ImmutableList<IConfigBase> HOTKEYS = addHotkeys();
    public static ImmutableList<IConfigBase> addHotkeys(){
        List<IConfigBase> list = new ArrayList<>();
        list.add(PRINTER);
        list.add(PRINT);
        list.add(TOGGLE_PRINTING_MODE);
        if(MODE_SWITCH.getOptionListValue() == State.ModeType.SINGLE) {
            list.add(SWITCH_PRINTER_MODE);
        } else if(MODE_SWITCH.getOptionListValue() == State.ModeType.MULTI){
            list.add(BEDROCK_SWITCH);
            list.add(EXCAVATE);
            list.add(FLUID);
        }
        list.add(CLOSE_ALL_MODE);
        if(loadChestTracker) list.add(PRINTER_INVENTORY);
        if(loadChestTracker) list.add(SYNC_INVENTORY);
        if(loadChestTracker) list.add(REMOVE_PRINT_INVENTORY);
        //#if MC > 12001
        if(loadChestTracker) list.add(LAST);
        if(loadChestTracker) list.add(NEXT);
        if(loadChestTracker) list.add(DELETE);
        //#endif
        list.add(TEST);
        return ImmutableList.copyOf(list);
    }

    public static final ImmutableList<IConfigBase> COLOR = addColor();
    public static ImmutableList<IConfigBase> addColor(){
        List<IConfigBase> list = new ArrayList<>();
        list.add(SYNC_INVENTORY_COLOR);

        return ImmutableList.copyOf(list);
    }

    //按下时激活
    public static final ImmutableList<ConfigHotkey> KEY_LIST = ImmutableList.of(
            PRINTER
    );
    //切换型开关
    public static final ImmutableList<IHotkeyTogglable> SWITCH_KEY = ImmutableList.of(

    );

    public static final ImmutableList<IConfigBase> ALL_CONFIGS = addAllConfigs();
    public static ImmutableList<IConfigBase> addAllConfigs(){
        List<IConfigBase> list = new ArrayList<>();
        list.addAll(GENERAL);
        list.addAll(HOTKEYS);
        list.addAll(COLOR);

        return ImmutableList.copyOf(list);
    }
    @Override
    public void load() {
        File settingFile = new File(FILE_PATH);
        if (settingFile.isFile() && settingFile.exists()) {
            JsonElement jsonElement = JsonUtils.parseJsonFile(settingFile);
            if (jsonElement != null && jsonElement.isJsonObject()) {
                JsonObject obj = jsonElement.getAsJsonObject();
                ConfigUtils.readConfigBase(obj, MOD_ID, ALL_CONFIGS);
            }
        }
    }

    @Override
    public void save() {
        if ((CONFIG_DIR.exists() && CONFIG_DIR.isDirectory()) || CONFIG_DIR.mkdirs()) {
            JsonObject configRoot = new JsonObject();
            ConfigUtils.writeConfigBase(configRoot, MOD_ID, ALL_CONFIGS);
            JsonUtils.writeJsonToFile(configRoot, new File(FILE_PATH));
        }
    }

    public static void init(){
        Configs.INSTANCE.load();
        ConfigManager.getInstance().registerConfigHandler(MOD_ID, Configs.INSTANCE);

        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerKeyboardInputHandler(InputHandler.getInstance());
        HotkeysCallback.init();
    }
}
