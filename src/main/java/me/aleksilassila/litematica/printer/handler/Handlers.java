package me.aleksilassila.litematica.printer.handler;

import com.google.common.collect.ImmutableList;
import me.aleksilassila.litematica.printer.handler.handlers.*;

public class Handlers {
    public static final GuiHandler GUI = new GuiHandler();
    public static final PrintHandler PRINT = new PrintHandler();
    public static final FillHandler FILL = new FillHandler();
    public static final MineHandler MINE = new MineHandler();
    public static final FluidHandler FLUID = new FluidHandler();
    public static final BedrockHandler BEDROCK = new BedrockHandler();

    public static final ImmutableList<ClientPlayerTickHandler> VALUES = ImmutableList.of(
            GUI,
            PRINT,
            FILL,
            FLUID,
            MINE,
            BEDROCK
    );

    public static void tick() {
        for (ClientPlayerTickHandler handler : VALUES) {
            handler.tick();
        }
    }
}
