package me.aleksilassila.litematica.printer.printer.action;

import me.aleksilassila.litematica.printer.Reference;
import me.aleksilassila.litematica.printer.mixin.extension.MultiPlayerGameModeExtension;
import me.aleksilassila.litematica.printer.printer.PlayerLook;
import me.aleksilassila.litematica.printer.utils.PacketUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// 其实应该把PlacementGuide这坨臭狗屎狠狠重构解耦一下的，把他的自由性和拓展性拉高一些
public class LilyPadAction extends Action {
    @Override
    public Action queueAction(@NotNull BlockPos blockPos, @NotNull Direction side, boolean useShift, @NotNull LocalPlayer player) {
        PlayerLook look = getPlayerLook();
        if (look == null) return this;
        // 1.21.1之前的使用物品包不带视角，服务端会用玩家视角做射线，所以先发转头包
        //#if MC < 12101
        //$$ PacketUtils.sendLookPacket(player, look);
        //#endif
        MultiPlayerGameModeExtension gameMode = (MultiPlayerGameModeExtension) Reference.MINECRAFT.gameMode;
        if (gameMode != null) {
            gameMode.litematica_printer$startPrediction(sequence -> createPacket(sequence, look));
        }
        return this;
    }

    private static ServerboundUseItemPacket createPacket(int sequence, PlayerLook look) {
        //#if MC >= 12101
        return new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, sequence, look.yaw(), look.pitch());
        //#elseif MC > 11802
        //$$ return new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, sequence);
        //#else
        //$$ return new ServerboundUseItemPacket(InteractionHand.MAIN_HAND);
        //#endif
    }

    @Override
    public @Nullable Direction getValidSide(ClientLevel world, BlockPos pos) {
        return Direction.DOWN;
    }
}
