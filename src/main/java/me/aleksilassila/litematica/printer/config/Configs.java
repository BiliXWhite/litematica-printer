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

import static me.aleksilassila.litematica.printer.LitematicaPrinterMod.*;
import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics.loadChestTracker;

public class Configs implements IConfigHandler {
    public static Configs INSTANCE = new Configs();
    private static final String FILE_PATH = "./config/" + MOD_ID + ".json";
    private static final File CONFIG_DIR = new File("./config");
    //mod
    public static final ConfigHotkey OPEN_SCREEN = new ConfigHotkey( "打开菜单", "Z,Y","也就是你现在看到的这个界面的热键。");

    //===========通用设置===========
    public static ImmutableList<IConfigBase> addGeneral(){
        List<IConfigBase> list = new ArrayList<>();
        if(loadChestTracker) list.add(CLOUD_INVENTORY);
        if(loadChestTracker) list.add(AUTO_INVENTORY);
        list.add(PRINT_SWITCH);
        list.add(PRINTER_SPEED);
        if (PRINTER_SPEED.getIntegerValue() == 0) list.add(BLOCKS_PER_TICK);
        list.add(PRINTER_RANGE);
        list.add(PLACE_COOLDOWN);
        list.add(ITERATOR_USE_TIME);
        list.add(ITERATOR_SHAPE);
        list.add(LAG_CHECK);
        list.add(PLACE_USE_PACKET);
        list.add(RENDER_HUD);
        list.add(QUICK_SHULKER);
        list.add(QUICK_SHULKER_MODE);
        list.add(QUICK_SHULKER_COOLDOWN);
        list.add(ITERATION_ORDER);
        list.add(X_REVERSE);
        list.add(Y_REVERSE);
        list.add(Z_REVERSE);
        list.add(MODE_SWITCH);
        if(MODE_SWITCH.getOptionListValue().equals(State.ModeType.SINGLE)) list.add(PRINTER_MODE);
        else list.add(MULTI_BREAK);
        list.add(RENDER_LAYER_LIMIT);
        list.add(FLUID_BLOCK_LIST);
        list.add(FLUID_LIST);
        list.add(FILL_BLOCK_LIST);
        list.add(FILL_BLOCK_FACING);
        if(loadChestTracker) list.add(INVENTORY_LIST);
        list.add(DEBUG_OUTPUT);
        list.add(UPDATE_CHECK);
        list.add(AUTO_DISABLE_PRINTER);

        return ImmutableList.copyOf(list);
    }

    //===========放置设置===========
    public static ImmutableList<IConfigBase> addPut(){
        List<IConfigBase> list = new ArrayList<>();

        list.add(PUT_SKIP);
        list.add(PUT_SKIP_LIST);
        list.add(STORE_ORDERLY);
        list.add(EASYPLACE_PROTOCOL);
        list.add(USE_EASYPLACE);
        list.add(FORCED_SNEAK);
        list.add(PRINT_IN_AIR);
        list.add(PRINT_WATER);
        list.add(SKIP_WATERLOGGED_BLOCK);
        list.add(REPLACE);
        list.add(REPLACEABLE_LIST);
        list.add(STRIP_LOGS);
        list.add(REPLACE_CORAL);
        list.add(NOTE_BLOCK_TUNING);
        list.add(SAFELY_OBSERVER);
        list.add(FILL_COMPOSTER);
        list.add(FALLING_CHECK);
        list.add(BREAK_WRONG_BLOCK);
        list.add(BREAK_EXTRA_BLOCK);
        list.add(BREAK_WRONG_STATE_BLOCK);
        list.add(FILL_FLOWING_FLUID);

        return ImmutableList.copyOf(list);
    }

    //===========挖掘设置===========
    public static ImmutableList<IConfigBase> addExcavate(){
        List<IConfigBase> list = new ArrayList<>();
        list.add(EXCAVATE_LIMITER);
        if(EXCAVATE_LIMITER.getOptionListValue().equals(State.ExcavateListMode.CUSTOM)){
            list.add(EXCAVATE_LIMIT);
            list.add(EXCAVATE_WHITELIST);
            list.add(EXCAVATE_BLACKLIST);
        }

        return ImmutableList.copyOf(list);
    }

    //===========热键设置===========
    public static ImmutableList<IConfigBase> addHotkeys(){
        List<IConfigBase> list = new ArrayList<>();
        list.add(OPEN_SCREEN);
        list.add(PRINT);
        list.add(TOGGLE_PRINTING_MODE);
        if(MODE_SWITCH.getOptionListValue().equals(State.ModeType.SINGLE)){
            list.add(SWITCH_PRINTER_MODE);
        }else {
            list.add(MINE);
            list.add(FLUID);
            list.add(FILL);
            list.add(BEDROCK);
        }
        list.add(CLOSE_ALL_MODE);
        list.add(SYNC_INVENTORY);
        if(loadChestTracker){
            list.add(PRINTER_INVENTORY);
            list.add(REMOVE_PRINT_INVENTORY);
            //#if MC >= 12001
            list.add(LAST);
            list.add(NEXT);
            list.add(DELETE);
            //#endif
        }

        return ImmutableList.copyOf(list);
    }

    //===========颜色设置===========
    public static ImmutableList<IConfigBase> addColor(){
        List<IConfigBase> list = new ArrayList<>();
        list.add(SYNC_INVENTORY_COLOR);

        return ImmutableList.copyOf(list);
    }

    //按下时激活
    public static ImmutableList<ConfigHotkey> addKeyList(){
        ArrayList<ConfigHotkey> list = new ArrayList<>();
        list.add(OPEN_SCREEN);
        list.add(SYNC_INVENTORY);
        list.add(SWITCH_PRINTER_MODE);


		if(loadChestTracker){
            list.add(PRINTER_INVENTORY);
            list.add(REMOVE_PRINT_INVENTORY);
            //#if MC >= 12001
            list.add(LAST);
            list.add(NEXT);
            list.add(DELETE);
            //#endif
        }
        return ImmutableList.copyOf(list);
    }
    //切换型开关
    public static ImmutableList<IHotkeyTogglable> addSwitchKey(){
        ArrayList<IHotkeyTogglable> list = new ArrayList<>();
        list.add(MINE);
        list.add(FLUID);
        list.add(FILL);
        list.add(BEDROCK);
        list.add(PRINT_WATER);
        list.add(USE_EASYPLACE);

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
