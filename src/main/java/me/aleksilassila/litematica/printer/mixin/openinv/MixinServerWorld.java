package me.aleksilassila.litematica.printer.mixin.openinv;

import me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket;
import me.aleksilassila.litematica.printer.printer.zxy.inventory.TickList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.playerList;
import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.tickMap;

@Mixin(ServerLevel.class)
public class MixinServerWorld {
    @Inject(at = @At("HEAD"),method = "tick")
    public void tick(CallbackInfo ci){
        for (ServerPlayer s : playerList) {
            TickList list = tickMap.get(s);
            if (!list.world.areEntitiesLoaded(ChunkPos.asLong(list.pos))) {
                //#if MC > 11802
                list.world.shouldTickBlocksAt(list.pos);
                //#else
                //$$ list.world.shouldTickBlocksAt(ChunkPos.asLong(list.pos));
                //#endif
            }
            BlockState state2 = list.world.getBlockState(list.pos);
            if(state2.isAir()){
                OpenInventoryPacket.openReturn(s,state2,false);
            }
        }
    }
}
