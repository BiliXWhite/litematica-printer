//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package me.aleksilassila.litematica.printer.mixin.jackf.lgacy;

//#if MC < 12002
import me.aleksilassila.litematica.printer.LitematicaMixinMod;
import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryDatabase;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
            method = {"onBreak"},
            at = {@At("TAIL")}
    )
    private void chestTracker$handleBlockBreak(World world, BlockPos pos, BlockState state, PlayerEntity player, CallbackInfo ci) {
        if(!LitematicaMixinMod.INVENTORY.getBooleanValue()) return;
        MemoryDatabase database = MemoryDatabase.getCurrent();
        if (database != null) {
            database.removePos(world.getRegistryKey().getValue(), pos);
        }
    }
}
//#endif