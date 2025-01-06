package me.aleksilassila.litematica.printer.printer.zxy.Utils;

import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.State;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;

import static me.aleksilassila.litematica.printer.printer.zxy.Utils.ZxyUtils.client;

public class TPPos {
    static boolean run = true;
    public static BlockPos targetPos;
    static BlockPos playerPos;
    public static void init() {
//        ClientTickEvents.END_CLIENT_TICK.register(client -> {
//            tick();
//        });
    }

    public static void tick() {
        if (targetPos != null && !run) {
            playerPos = client.player.getBlockPos();
            client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(targetPos.getX(), targetPos.getY(), targetPos.getZ(), true));
            targetPos = null;
            run = true;
            return;
        }
        if (run) {
            if (playerPos != null)
                client.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(playerPos.getX(), playerPos.getY(), playerPos.getZ(), true));
        }
        run = false;
    }
    public static boolean bedrockModeState() {
        return (LitematicaMixinMod.PRINT_SWITCH.getBooleanValue() || LitematicaMixinMod.PRINT.getKeybind().isPressed()) &&
                (((LitematicaMixinMod.MODE_SWITCH.getOptionListValue() == State.ModeType.SINGLE) &&
                        LitematicaMixinMod.PRINTER_MODE.getOptionListValue() == State.PrintModeType.BEDROCK) ||
                        ((LitematicaMixinMod.MODE_SWITCH.getOptionListValue() != State.ModeType.SINGLE) &&
                                LitematicaMixinMod.BEDROCK_SWITCH.getBooleanValue()));
    }
}
