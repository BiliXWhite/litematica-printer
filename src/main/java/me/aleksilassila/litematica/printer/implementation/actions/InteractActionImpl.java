package me.aleksilassila.litematica.printer.implementation.actions;

import me.aleksilassila.litematica.printer.actions.InteractAction;
import me.aleksilassila.litematica.printer.implementation.PrinterPlacementContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

public class InteractActionImpl extends InteractAction {
    public InteractActionImpl(PrinterPlacementContext context) {
        super(context);
    }

    @Override
    protected void interact(Minecraft client, LocalPlayer player, InteractionHand hand, BlockHitResult hitResult) {
        //#if MC > 11802
        if (client.gameMode != null) {
            client.gameMode.useItemOn(player, hand, hitResult);
            client.gameMode.useItem(player, hand);
        }
        //#else
        //$$
        //$$ if (client.gameMode != null && client.level != null) {
        //$$     client.gameMode.useItemOn(player, client.level, hand, hitResult);
        //$$     client.gameMode.useItem(player, client.level, hand);
        //$$ }
        //#endif
    }
}
