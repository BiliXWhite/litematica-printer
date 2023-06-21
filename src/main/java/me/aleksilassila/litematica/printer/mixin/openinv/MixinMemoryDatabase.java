package me.aleksilassila.litematica.printer.mixin.openinv;

import me.aleksilassila.litematica.printer.printer.memory.MemoryUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import red.jackf.chesttracker.ChestTracker;
import red.jackf.chesttracker.memory.Memory;
import red.jackf.chesttracker.memory.MemoryDatabase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static me.aleksilassila.litematica.printer.printer.zxy.OpenInventoryPacket.key;

@Mixin(MemoryDatabase.class)
public abstract class MixinMemoryDatabase {

    @Shadow(remap = false)
    private ConcurrentMap<Identifier, ConcurrentMap<BlockPos, Memory>> locations = new ConcurrentHashMap<>();

    @Shadow @Final private static NbtCompound FULL_DURABILITY_TAG;
    @Shadow(remap = false)
    private transient ConcurrentMap<Identifier, ConcurrentMap<BlockPos, Memory>> namedLocations;

    /**
     * @author 1
     * @reason 1
     */

    @Overwrite
    public List<Memory> findItems(ItemStack toFind, Identifier worldId) {
        List<Memory> found = new ArrayList();
        Map<BlockPos, Memory> location = locations.get(worldId);
        ClientPlayerEntity playerEntity = MinecraftClient.getInstance().player;
        if (location != null && playerEntity != null) {
            Iterator var6 = location.entrySet().iterator();

            while(true) {
                Map.Entry entry;
                do {
                    while(true) {
                        do {
                            do {
                                if (!var6.hasNext()) {
                                    return found;
                                }

                                entry = (Map.Entry)var6.next();
                            } while(entry.getKey() == null);
                        } while(!((Memory)entry.getValue()).getItems().stream().anyMatch((candidate) -> {
                            return MemoryUtils.areStacksEquivalent(toFind, candidate, toFind.getNbt() == null || toFind.getNbt().equals(FULL_DURABILITY_TAG));
                        }));
                        break;
                    }
                } while(((Memory)entry.getValue()).getPosition() != null && ChestTracker.getSquareSearchRange() != Integer.MAX_VALUE && !(((Memory)entry.getValue()).getPosition().getSquaredDistance(playerEntity.getBlockPos()) <= (double)ChestTracker.getSquareSearchRange()));
                found.add((Memory)entry.getValue());
            }
        } else {
            return found;
        }
    }
    /**
     * @author 2
     * @reason 2
     */
    @Overwrite
    public void removePos(Identifier worldId, BlockPos pos) {
//        System.out.println(key);
//        System.out.println(worldId);
        if(key!=null) worldId = key.getValue();
//        MinecraftClient.getInstance().player.closeHandledScreen();
        Map<BlockPos, Memory> location = (Map)this.locations.get(worldId);
        if (location != null) {
            location.remove(pos);
        }

        Map<BlockPos, Memory> namedLocation = (Map)this.namedLocations.get(worldId);
        if (namedLocation != null) {
            namedLocation.remove(pos);
        }

    }
    @Inject(at = @At("HEAD"),method = "getAllMemories")
    public void getAllMemories(Identifier worldId, CallbackInfoReturnable<List<ItemStack>> cir) {
//        System.out.println(worldId);
    }
}
