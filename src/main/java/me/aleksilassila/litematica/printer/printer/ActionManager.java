package me.aleksilassila.litematica.printer.printer;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.interfaces.IMultiPlayerGameMode;
import me.aleksilassila.litematica.printer.interfaces.Implementation;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.SwitchItem;
import me.aleksilassila.litematica.printer.utils.DirectionUtils;
import me.aleksilassila.litematica.printer.utils.InventoryUtils;
import me.aleksilassila.litematica.printer.utils.NetworkUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//#if MC > 12105
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.entity.player.Input;
//#else
//$$ import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
//#endif

public class ActionManager {
    public static final ActionManager INSTANCE = new ActionManager();

    public BlockPos target;
    public Direction side;
    public Vec3 hitModifier;
    public boolean useShift = false;
    public boolean useProtocol = false;
    @Nullable
    public Look look = null;
    public boolean needWait = false;

    private ActionManager() {
    }

    public void queueClick(@NotNull BlockPos target, @NotNull Direction side, @NotNull Vec3 hitModifier, boolean useShift) {
        if (Configs.Placement.PLACE_INTERVAL.getIntegerValue() != 0) {
            if (this.target != null) {
                System.out.println("Was not ready yet.");
                return;
            }
        }
        this.target = target;
        this.side = side;
        this.hitModifier = hitModifier;
        this.useShift = useShift;
    }


    public void sendQueue(LocalPlayer player) {
        if (target == null || side == null || hitModifier == null) {
            // 会刷屏污染日志
            // Debug.write("放置所需信息缺少！ Target:" + (target == null) + " Side:" + (side == null) + " HitModifier:" + (hitModifier == null));
            clearQueue();
            return;
        }
        if (!useProtocol && !needWait) {
            if (look != null) {
                if (DirectionUtils.orderedByNearest(look.yaw, look.pitch)[0].getAxis().isHorizontal()) {
                    needWait = true;
                    return;
                }
            }
        }
        if (needWait) {
            needWait = false;
        }
        Direction direction;
        if (look == null) {
            direction = side;
        } else {
            direction = DirectionUtils.getHorizontalDirection(look.yaw);
        }

        Vec3 hitVec;
        if (!useProtocol) {
            Vec3 targetCenter = Vec3.atCenterOf(target);
            Vec3 sideOffset = Vec3.atLowerCornerOf(DirectionUtils.getVector(side)).scale(0.5);
            Vec3 rotatedHitModifier = hitModifier.yRot((direction.toYRot() + 90) % 360).scale(0.5);
            hitVec = targetCenter.add(sideOffset).add(rotatedHitModifier);
        } else {
            hitVec = hitModifier;
        }

        if (InventoryUtils.getOrderlyStoreItem() != null) {
            if (InventoryUtils.getOrderlyStoreItem().isEmpty()) {
                SwitchItem.removeItem(InventoryUtils.getOrderlyStoreItem());
            } else {
                SwitchItem.syncUseTime(InventoryUtils.getOrderlyStoreItem());
            }
        }

        boolean wasSneak = player.isShiftKeyDown();

        if (useShift && !wasSneak) {
            setShift(player, true);
        } else if (!useShift && wasSneak) {
            setShift(player, false);
        }


        if (Configs.Placement.PRINT_USE_PACKET.getBooleanValue()) {
            NetworkUtils.sendPacket(sequence -> new ServerboundUseItemOnPacket(
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(hitVec, side, target, false)
                    //#if MC > 11802
                    , sequence
                    //#endif
            ));
        } else {
            if (PrinterUtils.client.gameMode != null) {
                ((IMultiPlayerGameMode) PrinterUtils.client.gameMode).litematica_printer$rightClickBlock(target, side, hitVec);
            }
        }

        if (useShift && !wasSneak) {
            setShift(player, false);
        } else if (!useShift && wasSneak) {
            setShift(player, true);
        }

        clearQueue();
    }

    public void sendLook(LocalPlayer player, Look look) {
        this.look = look;
        Implementation.sendLookPacket(player, look);
    }

    public void setShift(LocalPlayer player, boolean shift) {
        //#if MC > 12105
        Input input = new Input(player.input.keyPresses.forward(), player.input.keyPresses.backward(), player.input.keyPresses.left(), player.input.keyPresses.right(), player.input.keyPresses.jump(), shift, player.input.keyPresses.sprint());
        ServerboundPlayerInputPacket packet = new ServerboundPlayerInputPacket(input);
        //#else
        //$$ ServerboundPlayerCommandPacket packet = new ServerboundPlayerCommandPacket(player, shift ? ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY : ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY);
        //#endif

        player.setShiftKeyDown(shift);
        NetworkUtils.sendPacket(packet);
    }

    public void clearQueue() {
        this.target = null;
        this.side = null;
        this.hitModifier = null;
        this.useShift = false;
        this.useProtocol = false;
        this.needWait = false;
        this.look = null;
    }
}
