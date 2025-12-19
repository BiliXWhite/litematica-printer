package me.aleksilassila.litematica.printer.actions;

import me.aleksilassila.litematica.printer.bilixwhite.utils.PlaceUtils;
import me.aleksilassila.litematica.printer.implementation.PrinterPlacementContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.item.ItemStack;

//#if MC > 12105
import net.minecraft.world.entity.player.Input;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
//#else
//$$ import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
//#endif

public class PrepareAction extends Action {
    public final PrinterPlacementContext context;
    public boolean modifyYaw = true;
    public boolean modifyPitch = true;
    public float yaw = 0;
    public float pitch = 0;

    public PrepareAction(PrinterPlacementContext context) {
        this.context = context;
        Direction lookDirection = context.lookDirection;
        if (lookDirection != null && lookDirection.getAxis().isHorizontal()) {
            this.yaw = lookDirection.toYRot();
        } else {
            this.modifyYaw = false;
        }
        if (lookDirection == Direction.UP) {
            this.pitch = -90;
        } else if (lookDirection == Direction.DOWN) {
            this.pitch = 90;
        } else if (lookDirection != null) {
            this.pitch = 0;
        } else {
            this.modifyPitch = false;
        }
    }

    public PrepareAction(PrinterPlacementContext context, float yaw, float pitch) {
        this.context = context;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public void send(Minecraft client, LocalPlayer player) {
        ItemStack requiredItemStack = context.getItemInHand();
        PlaceUtils.setPickedItemToHand(requiredItemStack, client);

        if (modifyPitch || modifyYaw) {
            float yaw = modifyYaw ? this.yaw : player.getYRot();
            float pitch = modifyPitch ? this.pitch : player.getXRot();
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();
            boolean onGround = player.onGround();

            //#if MC > 12101
            ServerboundMovePlayerPacket packet = new ServerboundMovePlayerPacket.PosRot(x, y, z, yaw, pitch, onGround, player.horizontalCollision);
            //#else
            //$$ ServerboundMovePlayerPacket packet = new ServerboundMovePlayerPacket.PosRot(x, y, z, yaw, pitch, onGround);
            //#endif

            player.connection.send(packet);
        }
        boolean wasSneak = player.isShiftKeyDown();
        if (context.shouldSneak && !wasSneak) {
            sendShift(player, true);
        } else if (!context.shouldSneak && wasSneak) {
            sendShift(player, false);
        }
    }

    public static void sendShift(LocalPlayer player, boolean shift) {
        //#if MC > 12105
        net.minecraft.world.entity.player.Input input = new Input(player.input.keyPresses.forward(), player.input.keyPresses.backward(), player.input.keyPresses.left(), player.input.keyPresses.right(), player.input.keyPresses.jump(), shift, player.input.keyPresses.sprint());
        ServerboundPlayerInputPacket packet = new ServerboundPlayerInputPacket(input);
        //#else
        //$$ net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket packet = new net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket(player, shift ? ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY : ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY);
        //#endif
        player.connection.send(packet);
    }


    @Override
    public String toString() {
        return "PrepareAction{" + "context=" + context + '}';
    }
}