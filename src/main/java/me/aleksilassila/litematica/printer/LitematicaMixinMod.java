package me.aleksilassila.litematica.printer;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.*;
import fi.dy.masa.malilib.hotkeys.KeyCallbackToggleBooleanConfigWithMessage;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;
import me.aleksilassila.litematica.printer.config.KeyCallbackHotkeys;
import me.aleksilassila.litematica.printer.printer.State;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.HighlightBlockRenderer;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.OpenInventoryPacket;
import me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;

import java.util.List;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.Statistics.loadChestTracker;

public class LitematicaMixinMod implements ModInitializer, ClientModInitializer {

	// Config settings
	public static final ConfigInteger PRINT_INTERVAL = new ConfigInteger( "打印机工作间隔", 0,   0, 20, "以游戏刻度为单位工作间隔。值越低意味着打印速度越快");
	public static final ConfigInteger PRINTING_RANGE = new ConfigInteger("打印机工作半径", 3,     1,   256,   "若服务器未修改交互距离 请勿设置太大");
	public static final ConfigOptionList RANGE_MODE = new ConfigOptionList("半径模式", State.ListType.SPHERE,"立方体建议3，球体建议设置6，破基岩在立方体模式下无法正常使用");
	//    public static final ConfigBoolean PRINT_WATER    = new ConfigBoolean("printWater",    false, "Whether or not the printer should place water\n source blocks or make blocks waterlogged.");
	public static final ConfigBoolean PRINT_IN_AIR = new ConfigBoolean("printInAir",    true, "Whether or not the printer should place blocks without anything to build on.\nBe aware that some anti-cheat plugins might notice this.");
	public static final ConfigBoolean PRINT_MODE = new ConfigBoolean("printingMode",  false, "Autobuild / print loaded selection.\nBe aware that some servers and anticheat plugins do not allow printing.");
	public static final ConfigBoolean REPLACE = new ConfigBoolean("替换列表方块", true, "可以直接在一些可替换方块放置，例如 草 雪片");
	public static final ConfigBoolean STRIP_LOGS = new ConfigBoolean("stripLogs", false, "Whether or not the printer should use normal logs if stripped\nversions are not available and then strip them with an axe.");
	public static boolean shouldPrintInAir = PRINT_IN_AIR.getBooleanValue();
	public static final ConfigBooleanHotkeyed BEDROCK_SWITCH = new ConfigBooleanHotkeyed("破基岩模式", false,"J", "啊吧啊吧");
	public static final ConfigBooleanHotkeyed EXCAVATE = new ConfigBooleanHotkeyed("挖掘", false,"K", "挖掘所选区内的方块");
	public static final ConfigBooleanHotkeyed FLUID = new ConfigBooleanHotkeyed("排流体", false,"L", "在岩浆源、水源处放方块默认是沙子");

	public static final ConfigHotkey CLOSE_ALL_MODE = new ConfigHotkey("关闭全部模式", "LEFT_CONTROL,G","");

	public static final ConfigStringList FLUID_BLOCK_LIST = new ConfigStringList("排流体方块名单", ImmutableList.of("minecraft:sand"), "");
	public static final ConfigBoolean SKIP = new ConfigBoolean("是否放置侦测器和红石块", true, "关闭后会跳过侦测器和红石块的放置");
	//	public static final ConfigBoolean NO_FACING = new ConfigBoolean("忽略朝向", false, "会忽略朝向放置 建造间隔拉到0会有更快的速度");
	public static final ConfigBoolean QUICKSHULKER = new ConfigBoolean("快捷潜影盒", false, "在有快捷潜影盒mod的情况下可以直接从背包内的潜影盒取出物品\n替换的位置为投影的预设位置,如果所有预设位置都有濳影盒则不会替换。");
	public static final ConfigBoolean INVENTORY = new ConfigBoolean("远程交互容器", false, "在服务器有远程交互容器mod的情况下可以远程交互\n替换的位置为投影的预设位置。");

	public static final ConfigStringList INVENTORY_LIST = new ConfigStringList("库存白名单", ImmutableList.of("minecraft:chest"), "");
	public static final ConfigStringList BEDROCK_LIST = new ConfigStringList("基岩模式白名单", ImmutableList.of("minecraft:bedrock"), "");
	public static final ConfigStringList REPLACEABLE_LIST = new ConfigStringList("可替换方块",
			ImmutableList.of("minecraft:air","minecraft:snow","minecraft:lava","minecraft:water","minecraft:bubble_column","minecraft:grass_block"), "打印时将忽略这些错误方块 直接替换。");
	public static final ConfigHotkey TEST = new ConfigHotkey("test", "V", KeybindSettings.PRESS_ALLOWEXTRA_EMPTY, "测试用的，别乱按");

	public static ImmutableList<IConfigBase> getConfigList() {
		List<IConfigBase> list = new java.util.ArrayList<>(Configs.Generic.OPTIONS);
		list.add(PRINT_MODE);
		list.add(PRINT_INTERVAL);
		list.add(PRINTING_RANGE);
		list.add(RANGE_MODE);
		list.add(PRINT_IN_AIR);
		list.add(REPLACE);
		list.add(STRIP_LOGS);

		list.add(FLUID_BLOCK_LIST);
		if(loadChestTracker) list.add(INVENTORY_LIST);
		list.add(BEDROCK_LIST);
		list.add(REPLACEABLE_LIST);
		list.add(TEST);
		list.add(0, SKIP);
//		list.add(0, FLUID);

		if(Statistics.loadQuickShulker) list.add(0, QUICKSHULKER);
		if(loadChestTracker) list.add(0, INVENTORY);

		return ImmutableList.copyOf(list);
	}

	// Hotkeys
	public static final ConfigHotkey PRINT = new ConfigHotkey("print", "V", KeybindSettings.PRESS_ALLOWEXTRA_EMPTY, "Prints while pressed");
	public static final ConfigHotkey TOGGLE_PRINTING_MODE = new ConfigHotkey("togglePrintingMode", "CAPS_LOCK", KeybindSettings.PRESS_ALLOWEXTRA_EMPTY, "Allows quickly toggling on/off Printing mode");

//	public static final ConfigHotkey BEDROCK_MODE = new ConfigHotkey("破基岩模式", "J", "切换为破基岩模式 此模式下 y轴会从上往下判定.");
//	public static final ConfigHotkey EXE_MODE= new ConfigHotkey("挖掘模式", "K", "挖掘所选区的方块");
	public static final ConfigHotkey SYNC_INVENTORY = new ConfigHotkey("容器同步", "Y", "对准你想要达成的容器方块。按下热键将开始同步投影所选区域内的所选容器");
	public static final ConfigBooleanHotkeyed SYNC_INVENTORY_CHECK = new ConfigBooleanHotkeyed("同步时检查背包", false,"", "容器同步时检查背包，如果填充物不足，则不会打开容器");
	public static final ConfigHotkey PRINTER_INVENTORY= new ConfigHotkey("打印机库存", "G", "如果远程取物的目标是未加载的区块将会增加取物品的时间，用投影选区后按下热键\n" +
			"打印机工作时将会使用该库存内的物品\n" +
			"建议库存区域内放置假人来常加载区块");
	public static final ConfigHotkey REMOVE_PRINT_INVENTORY = new ConfigHotkey("清空打印机库存", "C,G", "清空打印机库存");


	public static List<IConfigBase> getHotkeyList() {
		List<IConfigBase> list = new java.util.ArrayList<>(Hotkeys.HOTKEY_LIST);
		list.add(PRINT);
		list.add(TOGGLE_PRINTING_MODE);
		list.add(BEDROCK_SWITCH);
		list.add(EXCAVATE);
		list.add(FLUID);
		list.add(CLOSE_ALL_MODE);
//		list.add(BEDROCK_MODE);
//		list.add(EXE_MODE);
		if(loadChestTracker) list.add(PRINTER_INVENTORY);
		if(loadChestTracker) list.add(SYNC_INVENTORY);
		if(loadChestTracker) list.add(REMOVE_PRINT_INVENTORY);
		list.add(TEST);

		return ImmutableList.copyOf(list);
	}
	public static final ConfigColor SYNC_INVENTORY_COLOR = new ConfigColor("容器同步和打印机添加库存高亮颜色",          "#4CFF4CE6", "");


	public static ImmutableList<IConfigBase> getColorsList() {
		List<IConfigBase> list = new java.util.ArrayList<>(Configs.Colors.OPTIONS);
		list.add(SYNC_INVENTORY_COLOR);
		return ImmutableList.copyOf(list);
	}

	@Override
	public void onInitialize() {
		KeyCallbackHotkeys keyCallbackHotkeys = new KeyCallbackHotkeys(MinecraftClient.getInstance());
		OpenInventoryPacket.registerReceivePacket();
		OpenInventoryPacket.registerClientReceivePacket();
		//#if MC > 12001
//$$ 		if(loadChestTracker) MemoryUtils.setup();
		//#endif

		TOGGLE_PRINTING_MODE.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(PRINT_MODE));

		SYNC_INVENTORY.getKeybind().setCallback(keyCallbackHotkeys);
		PRINTER_INVENTORY.getKeybind().setCallback(keyCallbackHotkeys);
		HighlightBlockRenderer.init();
//		BEDROCK_MODE.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(BEDROCK_SWITCH));
//		EXE_MODE.getKeybind().setCallback(new KeyCallbackToggleBooleanConfigWithMessage(EXCAVATE));
	}

	@Override
	public void onInitializeClient() {

	}
}