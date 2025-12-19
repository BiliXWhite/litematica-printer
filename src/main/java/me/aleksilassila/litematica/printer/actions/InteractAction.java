package me.aleksilassila.litematica.printer.actions;


import me.aleksilassila.litematica.printer.Debug;
import me.aleksilassila.litematica.printer.implementation.PrinterPlacementContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

abstract public class InteractAction extends Action {
    public final PrinterPlacementContext context;

    public InteractAction(PrinterPlacementContext context) {
        this.context = context;
    }

    protected abstract void interact(Minecraft client, LocalPlayer player, InteractionHand hand, BlockHitResult hitResult);

    @Override
    public void send(Minecraft client, LocalPlayer player) {
        boolean wasSneak = player.isShiftKeyDown();
        if (context.shouldSneak && !wasSneak) {
            PrepareAction.sendShift(player, true);
        } else if (!context.shouldSneak && wasSneak) {
            PrepareAction.sendShift(player, false);
        }
        interact(client, player, InteractionHand.MAIN_HAND, context.hitResult);
        Debug.write("InteractAction.send: Blockpos: {} Side: {} HitPos: {}", context.getClickedPos(), context.getClickedFace(), context.getClickLocation());
    }

    @Override
    public String toString() {
        return "InteractAction{" +
                "context=" + context +
                '}';
    }
}