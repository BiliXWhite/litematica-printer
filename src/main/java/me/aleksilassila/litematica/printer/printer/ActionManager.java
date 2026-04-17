package me.aleksilassila.litematica.printer.printer;

import lombok.Setter;
import me.aleksilassila.litematica.printer.Reference;
import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.mixin_extension.MultiPlayerGameModeExtension;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.SwitchItem;
import me.aleksilassila.litematica.printer.utils.BlockUtils;
import me.aleksilassila.litematica.printer.utils.InventoryUtils;
import me.aleksilassila.litematica.printer.utils.PacketUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

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
    @Setter
    @Nullable
    public PlayerLook look;
    public boolean needWaitModifyLook = false;
    private boolean actionRequiresWaitModifyLook = false;

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

    public ActionManager sendQueue(LocalPlayer player) {
        if (target == null || side == null || hitModifier == null) {
            clearQueue();
            return this;
        }
        if (look != null) {
            PacketUtils.sendLookPacket(player, look);
        }

        if (!useProtocol && !needWaitModifyLook && actionRequiresWaitModifyLook) {
            if (look != null) {
                Direction lookDirection = BlockUtils.orderedByNearest(look.yaw(), look.pitch())[0];
                if (lookDirection.getAxis().isHorizontal()) {
                    needWaitModifyLook = true;
                    return this;
                }
            }
        }

        if (needWaitModifyLook) {
            needWaitModifyLook = false;
        }

        Direction direction;
        if (look == null) {
            direction = side;
        } else {
            direction = BlockUtils.getHorizontalDirection(look.yaw());
        }
        Vec3 hitVec;
        if (!useProtocol) {
            Vec3 targetCenter = Vec3.atCenterOf(target);
            Vec3 sideOffset = Vec3.atLowerCornerOf(BlockUtils.getVector(side)).scale(0.5);
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
        MultiPlayerGameModeExtension gameModeExtension = (MultiPlayerGameModeExtension) Reference.MINECRAFT.gameMode;
        if (gameModeExtension != null) {
            boolean localPrediction = !Configs.Placement.PRINT_USE_PACKET.getBooleanValue();
            BlockHitResult blockHitResult = new BlockHitResult(hitVec, side, target, false);
            gameModeExtension.litematica_printer$useItemOn(localPrediction, InteractionHand.MAIN_HAND, blockHitResult);
        }
        if (useShift && !wasSneak) {
            setShift(player, false);
        } else if (!useShift && wasSneak) {
            setShift(player, true);
        }
        clearQueue();
        return this;
    }

    public void setNeedWaitModifyLookFromAction(boolean needWaitModifyLook) {
        this.actionRequiresWaitModifyLook = needWaitModifyLook;
    }

    public void setShift(LocalPlayer player, boolean shift) {
        //#if MC > 12105
        Input input = new Input(player.input.keyPresses.forward(), player.input.keyPresses.backward(), player.input.keyPresses.left(), player.input.keyPresses.right(), player.input.keyPresses.jump(), shift, player.input.keyPresses.sprint());
        ServerboundPlayerInputPacket packet = new ServerboundPlayerInputPacket(input);
        //#else
        //$$ ServerboundPlayerCommandPacket packet = new ServerboundPlayerCommandPacket(player, shift ? ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY : ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY);
        //#endif
        player.setShiftKeyDown(shift);
        PacketUtils.sendPacket(packet);
    }

    public void clearQueue() {
        this.target = null;
        this.side = null;
        this.hitModifier = null;
        this.useShift = false;
        this.useProtocol = false;
        this.needWaitModifyLook = false;
        this.actionRequiresWaitModifyLook = false;
        this.look = null;
    }
}