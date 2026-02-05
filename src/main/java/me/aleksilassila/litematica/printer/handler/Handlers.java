package me.aleksilassila.litematica.printer.handler;

import com.google.common.collect.ImmutableList;
import me.aleksilassila.litematica.printer.handler.handlers.FillHandler;
import me.aleksilassila.litematica.printer.handler.handlers.MineHandler;

public class Handlers {
    public static final FillHandler FILL = new FillHandler();
    public static final MineHandler MINE = new MineHandler();

    public static final ImmutableList<ClientPlayerTickHandler> VALUES = ImmutableList.of(
            FILL,
            MINE
    );

    public static void tick() {
        for (ClientPlayerTickHandler handler : VALUES) {
            handler.tick();
        }
    }
}
