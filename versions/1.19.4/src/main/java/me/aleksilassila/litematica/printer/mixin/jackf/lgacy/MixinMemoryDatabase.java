package me.aleksilassila.litematica.printer.mixin.jackf.lgacy;

import me.aleksilassila.litematica.printer.printer.zxy.memory.MemoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static me.aleksilassila.litematica.printer.printer.zxy.inventory.OpenInventoryPacket.*;


@Mixin(MemoryDatabase.class)
public abstract class MixinMemoryDatabase {

    @Shadow(remap = false)
    private ConcurrentMap<ResourceLocation, ConcurrentMap<BlockPos, Memory>> locations = new ConcurrentHashMap<>();

    @Shadow @Final private static CompoundTag FULL_DURABILITY_TAG;
    @Shadow(remap = false)
    private transient ConcurrentMap<ResourceLocation, ConcurrentMap<BlockPos, Memory>> namedLocations;

    /**
     * @author 1
     * @reason 1
     */

    @Overwrite
    public List<Memory> findItems(ItemStack toFind, ResourceLocation worldId) {
        List<Memory> found = new ArrayList<>();
        Map<BlockPos, Memory> location = locations.get(worldId);
        LocalPlayer playerEntity = Minecraft.getInstance().player;
        if (location == null || playerEntity == null) {
            return found;
        }

        double maxRange = ChestTracker.getSquareSearchRange();
        BlockPos playerPos = playerEntity.blockPosition();

        for (Map.Entry<BlockPos, Memory> entry : location.entrySet()) {
            BlockPos pos = entry.getKey();
            Memory memory = entry.getValue();
            if (pos == null || memory == null) continue;

            boolean matches = memory.getItems().stream()
                    .anyMatch(candidate -> MemoryUtils.areStacksEquivalent(
                            toFind, candidate, toFind.getTag() == null || toFind.getTag().equals(FULL_DURABILITY_TAG)
                    ));

            if (!matches) continue;

            if (memory.getPosition() != null && maxRange != Integer.MAX_VALUE) {
                if (memory.getPosition().distSqr(playerPos) > maxRange) {
                    continue;
                }
            }

            found.add(memory);
        }
        return found;
    }
    /**
     * @author 2
     * @reason 2
     */
    @Overwrite
    public void removePos(ResourceLocation worldId, BlockPos pos) {
//        System.out.println(key);
//        System.out.println(worldId);
        if(key!=null) worldId = key.location();
//        MinecraftClient.getInstance().player.closeHandledScreen();
        Map<BlockPos, Memory> location = this.locations.get(worldId);
        if (location != null) {
            location.remove(pos);
        }

        Map<BlockPos, Memory> namedLocation = this.namedLocations.get(worldId);
        if (namedLocation != null) {
            namedLocation.remove(pos);
        }

    }
    @Inject(at = @At("HEAD"),method = "getAllMemories")
    public void getAllMemories(ResourceLocation worldId, CallbackInfoReturnable<List<ItemStack>> cir) {
//        System.out.println(worldId);
    }
}
