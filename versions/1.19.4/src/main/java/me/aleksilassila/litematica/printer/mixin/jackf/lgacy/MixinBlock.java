package me.aleksilassila.litematica.printer.mixin.jackf.lgacy;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryDatabase;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin({Block.class})
public abstract class MixinBlock {
    public MixinBlock() {
    }

    @Inject(
            method = {"playerWillDestroy"},
            at = {@At("TAIL")}
    )
    private void chestTracker$handleBlockBreak(Level world, BlockPos pos, BlockState state, Player player, CallbackInfo ci) {
        if(!Configs.General.CLOUD_INVENTORY.getBooleanValue()) return;
        MemoryDatabase database = MemoryDatabase.getCurrent();
        if (database != null) {
            database.removePos(world.dimension().location(), pos);
        }
    }
}
