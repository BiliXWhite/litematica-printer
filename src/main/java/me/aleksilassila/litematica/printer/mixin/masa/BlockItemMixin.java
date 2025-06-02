package me.aleksilassila.litematica.printer.mixin.masa;

import fi.dy.masa.litematica.util.PlacementHandler;
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockItem.class, priority = 981)
public abstract class BlockItemMixin extends Item
{
    private BlockItemMixin(Item.Settings builder)
    {
        super(builder);
    }

    @Shadow protected abstract boolean canPlace(ItemPlacementContext context, BlockState state);
    @Shadow public abstract Block getBlock();

    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void modifyPlacementState(ItemPlacementContext ctx, CallbackInfoReturnable<BlockState> cir)
    {
        if (LitematicaMixinMod.PRECISE_PLACE.getBooleanValue())
        {
            BlockState stateOrig = this.getBlock().getPlacementState(ctx);

            if (stateOrig != null && this.canPlace(ctx, stateOrig))
            {
                PlacementHandler.UseContext context = PlacementHandler.UseContext.from(ctx, ctx.getHand());
                cir.setReturnValue(PlacementHandler.applyPlacementProtocolToPlacementState(stateOrig, context));
            }
        }
    }
}
