package me.aleksilassila.litematica.printer.mixin.printer.mc;

import fi.dy.masa.litematica.util.PlacementHandler;
import me.aleksilassila.litematica.printer.config.Configs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockItem.class, priority = 981)
public abstract class MixinBlockItem extends Item {
    private MixinBlockItem(Item.Properties builder) {
        super(builder);
    }

    @Shadow
    protected abstract boolean canPlace(BlockPlaceContext context, BlockState state);

    @Shadow
    public abstract Block getBlock();

    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void modifyPlacementState(BlockPlaceContext ctx, CallbackInfoReturnable<BlockState> cir) {
        if (Configs.EASYPLACE_PROTOCOL.getBooleanValue()) {
            BlockState stateOrig = this.getBlock().getStateForPlacement(ctx);
            if (stateOrig != null && this.canPlace(ctx, stateOrig)) {
                PlacementHandler.UseContext context = PlacementHandler.UseContext.from(ctx, ctx.getHand());
                cir.setReturnValue(PlacementHandler.applyPlacementProtocolToPlacementState(stateOrig, context));
            }
        }
    }
}