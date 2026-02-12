package me.aleksilassila.litematica.printer.gui;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import me.aleksilassila.litematica.printer.I18n;
import me.aleksilassila.litematica.printer.Reference;
import me.aleksilassila.litematica.printer.mixin_extension.ConfigExtension;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.utils.UpdateCheckerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

public class ConfigUi extends GuiConfigsBase {
    private static Tab tab = Tab.CORE;

    public ConfigUi(@Nullable Screen parent) {
        super(10, 50, Reference.MOD_ID, parent, "Litematica Printer " + UpdateCheckerUtils.LOCAL_VERSION + "   " + I18n.FREE_NOTICE.getName().getString());
    }

    public ConfigUi() {
        this(Minecraft.getInstance().screen);
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

    public void reset() {
        reCreateListWidget();
        Objects.requireNonNull(getListWidget()).resetScrollbarPosition();
        initGui();
    }

    private int createButton(int x, int y, int width, Tab tab) {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tab.getName(), tab.getComment());
        button.setEnabled(ConfigUi.tab != tab);
        this.addButton(button, new ButtonListener(tab, this));
        return button.getWidth() + 2;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        ImmutableList.Builder<ConfigOptionWrapper> builder = ImmutableList.builder();
        for (IConfigBase config : ConfigUi.tab.getConfigs()) {
            if (config instanceof ConfigExtension extension) {
                @Nullable BooleanSupplier visible = extension.litematica_printer$getVisible();
                if (visible != null && visible.getAsBoolean()) {
                    builder.add(new ConfigOptionWrapper(config));
                }
            }
        }
        return builder.build();
    }

    public record ButtonListener(Tab tab, ConfigUi parent) implements IButtonActionListener {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            ConfigUi.tab = this.tab;
            this.parent.reset();
        }
    }

    public enum Tab {
        CORE(I18n.of("category.core")),
        PLACEMENT(I18n.of("category.placement")),
        BREAK(I18n.of("category.break")),
        HOTKEYS(I18n.of("category.hotkeys")),
        PRINT(I18n.of("category.print")),
        EXCAVATE(I18n.of("category.mine")),
        FILL(I18n.of("category.fill")),
        FLUID(I18n.of("category.fluid"));

        private final I18n i18n;

        Tab(I18n i18n) {
            this.i18n = i18n;
        }

        public String getName() {
            return i18n.getConfigName().getString();
        }

        public String getComment() {
            return i18n.getConfigDesc().getString();
        }

        public ImmutableList<IConfigBase> getConfigs() {
            return switch (this) {
                case CORE -> Configs.Core.OPTIONS;
                case PLACEMENT -> Configs.Placement.OPTIONS;
                case BREAK -> Configs.Break.OPTIONS;
                case PRINT -> Configs.Print.OPTIONS;
                case EXCAVATE -> Configs.Mine.OPTIONS;
                case FILL -> Configs.Fill.OPTIONS;
                case FLUID -> Configs.Fluid.OPTIONS;
                case HOTKEYS -> Configs.Hotkeys.OPTIONS;
            };
        }
    }
}