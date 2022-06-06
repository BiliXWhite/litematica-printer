package me.aleksilassila.litematica.printer;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.hotkeys.KeyCallbackToggleBooleanConfigWithMessage;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import net.fabricmc.api.ModInitializer;

import java.util.List;

public class LitematicaMixinMod implements ModInitializer {

	// Config settings
	public static final ConfigInteger PRINT_INTERVAL = new ConfigInteger( "printInterval", 4,   0, 20, "Print interval in game ticks. Lower values mean faster printing speed.\nIf the printer creates \"ghost blocks\", raise this value.");
	public static final ConfigInteger PRINTING_RANGE = new ConfigInteger("printingRange", 5,     1,   20,   "Printing block place range\nLower values are recommended for servers.");
	//    public static final ConfigBoolean PRINT_WATER    = new ConfigBoolean("printWater",    false, "Whether or not the printer should place water\n source blocks or make blocks waterlogged.");
	public static final ConfigBoolean PRINT_IN_AIR = new ConfigBoolean("printInAir",    true, "Whether or not the printer should place blocks without anything to build on.\nBe aware that some anti-cheat plugins might notice this.");
	public static final ConfigBoolean PRINT_MODE 	 = new ConfigBoolean("printingMode",  false, "Autobuild / print loaded selection.\nBe aware that some servers and anticheat plugins do not allow printing.");
	public static final ConfigBoolean REPLACE_FLUIDS = new ConfigBoolean("replaceFluids", false, "Whether or not fluid source blocks should be replaced by the printer.");
	public static final ConfigBoolean STRIP_LOGS = new ConfigBoolean("stripLogs", false, "Whether or not the printer should use normal logs if stripped\nversions are not available and then strip them with an axe.");
	public static boolean shouldPrintInAir = PRINT_IN_AIR.getBooleanValue();
	public static boolean shouldReplaceFluids = REPLACE_FLUIDS.getBooleanValue();

	public static final ConfigBoolean BEDROCK = new ConfigBoolean("破基岩模式", false, "啊吧啊吧");
	public static final ConfigBoolean MINING = new ConfigBoolean("挖掘", false, "会挖掘玻璃投影所覆盖的方块");
	public static final ConfigBoolean SKIP = new ConfigBoolean("是否放置侦测器和红石块", true, "关闭后会跳过侦测器和红石块的放置");
	//	public static final ConfigBoolean NO_FACING = new ConfigBoolean("忽略朝向", false, "会忽略朝向放置 建造间隔拉到0会有更快的速度");
	public static final ConfigBoolean FLUID = new ConfigBoolean("排流体", false, "会在岩浆源、水源处放沙子");

//	public static final ConfigStringList BLOCKS = new ConfigStringList("排流体的方块",ImmutableList.of("minecraft:sand"), "先择排流体的方块");


	public static ImmutableList<IConfigBase> getConfigList() {
		List<IConfigBase> list = new java.util.ArrayList<>(Configs.Generic.OPTIONS);
		list.add(PRINT_MODE);
		list.add(PRINT_INTERVAL);
		list.add(PRINTING_RANGE);
		list.add(PRINT_IN_AIR);
		list.add(REPLACE_FLUIDS);
		list.add(STRIP_LOGS);

		list.add(BEDROCK);
		list.add(MINING);
//		list.add(BLOCKS);
		list.add(0, SKIP);
//		list.add(0,NO_FACING);
		list.add(0, FLUID);

		return ImmutableList.copyOf(list);
	}

	// Hotkeys
	public static final ConfigHotkey PRINT = new ConfigHotkey("print", "V", KeybindSettings.PRESS_ALLOWEXTRA_EMPTY, "Prints while pressed");
	public static final ConfigHotkey TOGGLE_PRINTING_MODE = new ConfigHotkey("togglePrintingMode", "CAPS_LOCK", KeybindSettings.PRESS_ALLOWEXTRA_EMPTY, "Allows quickly toggling on/off Printing mode");

	public static final ConfigHotkey BEDROCK_MODE = new ConfigHotkey("破基岩模式", "J", "切换为破基岩模式 此模式下 y轴会从上往下判定");
	public static final ConfigHotkey EXE_MODE= new ConfigHotkey("挖掘模式", "K", "自动挖掘玻璃投影覆盖的方块");


	public static List<ConfigHotkey> getHotkeyList() {
		List<ConfigHotkey> list = new java.util.ArrayList<>(Hotkeys.HOTKEY_LIST);
		list.add(PRINT);
		list.add(TOGGLE_PRINTING_MODE);
		list.add(BEDROCK_MODE);
		list.add(EXE_MODE);

		return ImmutableList.copyOf(list);
	}

	@Override
	public void onInitialize() {
		TOGGLE_PRINTING_MODE.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(PRINT_MODE));
		BEDROCK_MODE.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(BEDROCK));
		EXE_MODE.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(MINING));
	}
}
