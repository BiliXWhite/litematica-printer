package me.aleksilassila.litematica.printer.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.*;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.util.JsonUtils;
import me.aleksilassila.litematica.printer.printer.State;
import net.fabricmc.loader.api.FabricLoader;

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

    public static ImmutableList<IConfigBase> addGeneral(){
        List<IConfigBase> list = new ArrayList<>();
        if(loadChestTracker) list.add(INVENTORY);
        if(loadChestTracker) list.add(AUTO_INVENTORY);
        if(loadQuickShulker) list.add(QUICKSHULKER);
        list.add(PRINT_SWITCH);
        list.add(PRINT_INTERVAL);
        list.add(PRINTING_RANGE);
        list.add(COMPULSION_RANGE);
        list.add(RANGE_MODE);
        list.add(MODE_SWITCH);
        if(MODE_SWITCH.getOptionListValue().equals(State.ModeType.SINGLE)) list.add(PRINTER_MODE);
        else list.add(MULTI_BREAK);
        list.add(RENDER_LAYER_LIMIT);
        list.add(FLUID_BLOCK_LIST);
        if(loadChestTracker) list.add(INVENTORY_LIST);
        list.add(BEDROCK_LIST);

        return ImmutableList.copyOf(list);
    }

    public static ImmutableList<IConfigBase> addPut(){
        List<IConfigBase> list = new ArrayList<>();

        list.add(PUT_SKIP);
        list.add(PUT_SKIP_LIST);
        list.add(PUT_TESTING);
        list.add(PRINT_CHECK);
        list.add(EASY_MODE);
        list.add(FORCED_PLACEMENT);
        list.add(PRINT_IN_AIR);
        list.add(PRINT_WATER_LOGGED_BLOCK);
        list.add(BREAK_ERROR_BLOCK);
        list.add(REPLACE);
        list.add(REPLACEABLE_LIST);
        list.add(STRIP_LOGS);

        return ImmutableList.copyOf(list);
    }
    public static ImmutableList<IConfigBase> addExcavate(){
        List<IConfigBase> list = new ArrayList<>();
        if(MODE_SWITCH.getOptionListValue().equals(State.ModeType.MULTI)) list.add(EXCAVATE);
        list.add(EXCAVATE_LIMITER);
        if(EXCAVATE_LIMITER.getOptionListValue().equals(State.ExcavateListMode.ME)){
            list.add(EXCAVATE_LIMIT);
            list.add(EXCAVATE_WHITELIST);
            list.add(EXCAVATE_BLACKLIST);
        }

        return ImmutableList.copyOf(list);
    }

    //热键列表
    public static ImmutableList<IConfigBase> addHotkeys(){
        List<IConfigBase> list = new ArrayList<>();
        list.add(PRINTER);
        list.add(PRINT);
        list.add(TOGGLE_PRINTING_MODE);

        if(MODE_SWITCH.getOptionListValue().equals(State.ModeType.SINGLE)) {
            list.add(SWITCH_PRINTER_MODE);
        } else if(MODE_SWITCH.getOptionListValue().equals(State.ModeType.MULTI)){
            list.add(BEDROCK_SWITCH);
            list.add(EXCAVATE);
            list.add(FLUID);
        }

        list.add(CLOSE_ALL_MODE);
        list.add(SYNC_INVENTORY);
        if(loadChestTracker){
            list.add(PRINTER_INVENTORY);
            list.add(REMOVE_PRINT_INVENTORY);
            //#if MC >= 12001
            //$$ list.add(LAST);
            //$$ list.add(NEXT);
            //$$ list.add(DELETE);
            //#endif
        }
        if(FabricLoader.getInstance().isDevelopmentEnvironment()) list.add(TEST);

        return ImmutableList.copyOf(list);
    }

    public static ImmutableList<IConfigBase> addColor(){
        List<IConfigBase> list = new ArrayList<>();
        list.add(SYNC_INVENTORY_COLOR);

        return ImmutableList.copyOf(list);
    }

    //按下时激活
    public static ImmutableList<ConfigHotkey> addKeyList(){
        ArrayList<ConfigHotkey> list = new ArrayList<>();
        list.add(PRINTER);
        list.add(SYNC_INVENTORY);
        list.add(SWITCH_PRINTER_MODE);


		if(loadChestTracker){
            list.add(PRINTER_INVENTORY);
            list.add(REMOVE_PRINT_INVENTORY);
            //#if MC >= 12001
            //$$ list.add(LAST);
            //$$ list.add(NEXT);
            //$$ list.add(DELETE);
            //#endif
        }
        if(FabricLoader.getInstance().isDevelopmentEnvironment()) list.add(TEST);
        return ImmutableList.copyOf(list);
    }
    //切换型开关
    public static ImmutableList<IHotkeyTogglable> addSwitchKey(){
        ArrayList<IHotkeyTogglable> list = new ArrayList<>();
        list.add(BEDROCK_SWITCH);
        list.add(EXCAVATE);
        list.add(FLUID);

        return ImmutableList.copyOf(list);
    }

    public static ImmutableList<IConfigBase> addAllConfigs(){
        List<IConfigBase> list = new ArrayList<>();
        list.addAll(addGeneral());
        list.addAll(addPut());
        list.addAll(addExcavate());
        list.addAll(addHotkeys());
        list.addAll(addColor());

        return ImmutableList.copyOf(list);
    }
    @Override
    public void load() {
        File settingFile = new File(FILE_PATH);
        if (settingFile.isFile() && settingFile.exists()) {
            JsonElement jsonElement = JsonUtils.parseJsonFile(settingFile);
            if (jsonElement != null && jsonElement.isJsonObject()) {
                JsonObject obj = jsonElement.getAsJsonObject();
                ConfigUtils.readConfigBase(obj, MOD_ID, addAllConfigs());
            }
        }
    }

    @Override
    public void save() {
        if ((CONFIG_DIR.exists() && CONFIG_DIR.isDirectory()) || CONFIG_DIR.mkdirs()) {
            JsonObject configRoot = new JsonObject();
            ConfigUtils.writeConfigBase(configRoot, MOD_ID, addAllConfigs());
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
