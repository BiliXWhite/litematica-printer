package me.aleksilassila.litematica.printer.config;

import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import fi.dy.masa.malilib.hotkeys.IKeyboardInputHandler;
import me.aleksilassila.litematica.printer.LitematicaPrinterMod;


//注册按键
public class InputHandler implements IKeybindProvider, IKeyboardInputHandler {
    private static final InputHandler INSTANCE = new InputHandler();

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        //没被添加到的,按下按键时不会被识别
        for (IHotkey hotkey : Configs.addKeyList()) {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
        for (IHotkey hotkey : Configs.addSwitchKey()) {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        manager.addHotkeysForCategory(LitematicaPrinterMod.MOD_ID, "按下式",Configs.addKeyList());
        manager.addHotkeysForCategory(LitematicaPrinterMod.MOD_ID, "切换式",Configs.addSwitchKey());
    }

    public static InputHandler getInstance(){
        return INSTANCE;
    }
}
