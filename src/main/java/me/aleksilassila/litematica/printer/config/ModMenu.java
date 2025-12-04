package me.aleksilassila.litematica.printer.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return (screen)->{
            ConfigUI ui = new ConfigUI();
            ui.setParent(screen);
            return ui;
        };
    }
}
