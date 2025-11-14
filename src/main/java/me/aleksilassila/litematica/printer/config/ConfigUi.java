package me.aleksilassila.litematica.printer.config;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import me.aleksilassila.litematica.printer.LitematicaPrinterMod;
import me.aleksilassila.litematica.printer.printer.UpdateChecker;

import java.util.List;
import static me.aleksilassila.litematica.printer.config.ConfigUi.Tab.*;
import static me.aleksilassila.litematica.printer.config.Configs.addGeneral;
import static me.aleksilassila.litematica.printer.config.Configs.addHotkeys;

public class ConfigUi extends GuiConfigsBase {
    private static Tab tab = Tab.ALL;

    public ConfigUi() {
        super(10, 50, LitematicaPrinterMod.MOD_ID, null, "投影打印机 " + UpdateChecker.version);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.clearOptions();

        int x = 10;
        int y = 26;
        for (Tab tab : Tab.values()) {
            x += this.createButton(x, y, -1, tab);
        }

    }

    private int createButton(int x, int y, int width, Tab tab) {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tab.name);
        button.setEnabled(ConfigUi.tab != tab);
        this.addButton(button, new ButtonListener(tab, this));

        return button.getWidth() + 2;
    }

    //按钮宽度
//    @Override
//    protected int getConfigWidth()
//    {
//        Tab tab = ConfigUi.tab;
//
//        if (tab == Tab.ALL)
//        {
//            return 120;
//        }
//        else if (tab == Tab.GENERAL)
//        {
//            return 60;
//        }
//        return 260;
//    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        List<? extends IConfigBase> configs;
        Tab tab = ConfigUi.tab;
        if (tab == Tab.ALL) {
            configs = Configs.addAllConfigs();
        } else if(tab == GENERAL) {
            configs = addGeneral();
        } else if (tab == PUT) {
            configs = Configs.addPut();
        } else if (tab == EXCAVATE) {
            configs = Configs.addExcavate();
        } else if (tab == HOTKEYS) {
            configs = addHotkeys();
        } else if (tab == COLOR) {
            configs = Configs.addColor();
        } else if(tab == null){
            return null;
        } else {
            configs = Configs.addAllConfigs();
        }
        return ConfigOptionWrapper.createFor(configs);
    }

    private record ButtonListener(Tab tab, ConfigUi parent) implements IButtonActionListener {

        @Override
            public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
                ConfigUi.tab = this.tab;
                this.parent.reCreateListWidget();
                this.parent.getListWidget().resetScrollbarPosition();
                this.parent.initGui();
            }
        }

    public enum Tab {
        ALL("全部"),
        GENERAL("通用"),
        PUT("放置"),
        EXCAVATE("挖掘"),
        HOTKEYS("热键"),
        COLOR("颜色"),
        ;

        public final String name;

        Tab(String str) {
            name = str;
        }
    }
}
