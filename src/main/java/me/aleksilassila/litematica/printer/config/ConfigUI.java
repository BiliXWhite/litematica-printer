package me.aleksilassila.litematica.printer.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.LitematicaPrinterMod;
import me.aleksilassila.litematica.printer.printer.UpdateChecker;

import java.util.List;

public class ConfigUI extends GuiConfigsBase {
    private static Tab tab = Tab.ALL;

    public ConfigUI() {
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
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tab.getName(), tab.getComment());
        button.setEnabled(ConfigUI.tab != tab);
        this.addButton(button, new ButtonListener(tab, this));
        return button.getWidth() + 2;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        return ConfigOptionWrapper.createFor(ConfigUI.tab.getConfigs());
    }

    private record ButtonListener(Tab tab, ConfigUI parent) implements IButtonActionListener {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            ConfigUI.tab = this.tab;
            this.parent.reCreateListWidget();
            this.parent.getListWidget().resetScrollbarPosition();
            this.parent.initGui();
        }
    }

    public enum Tab {
        ALL(I18n.TAB_ALL),
        GENERAL(I18n.TAB_GENERAL),
        PUT(I18n.TAB_PUT),
        EXCAVATE(I18n.TAB_EXCAVATE),
        FILL(I18n.FILL),
//        BEDROCK(I18n.TAB_BEDROCK, false),
        HOTKEYS(I18n.TAB_HOTKEYS),
        COLOR(I18n.TAB_COLOR);

        private final I18n i18n;

        Tab(I18n i18n) {
            this.i18n = i18n;
        }

        public String getName() {
            return i18n.getConfigNameKeyComponent().getString();
        }

        public String getComment() {
            return i18n.getConfigCommentKeyComponent().getString();
        }

        public ImmutableList<IConfigBase> getConfigs() {
            return switch (this) {
                case ALL -> Configs.getAllConfigs();
                case GENERAL -> Configs.getGeneral();
                case PUT -> Configs.getPut();
                case EXCAVATE -> Configs.getExcavate();
                case FILL -> Configs.getFills();
//                case BEDROCK -> Configs.getBedrock();
                case HOTKEYS -> Configs.getHotkeys();
                case COLOR -> Configs.getColor();
            };
        }
    }
}
