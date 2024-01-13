package me.aleksilassila.litematica.printer.mixin.jackf;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.jackf.chesttracker.memory.Memory;
import red.jackf.chesttracker.memory.MemoryBank;
import red.jackf.chesttracker.memory.metadata.Metadata;
import red.jackf.chesttracker.util.MemoryUtil;
import red.jackf.whereisit.api.SearchRequest;
import red.jackf.whereisit.api.SearchResult;

import java.util.*;

import static me.aleksilassila.litematica.printer.printer.zxy.chesttracker.MemoryUtils.PRINTER_MEMORY;

@Mixin(MemoryBank.class)
public abstract class MemoryBankMixin {
    protected MemoryBankMixin(Map<Identifier, Map<BlockPos, Memory>> memories) {
        this.memories = memories;
    }

    @Inject(at = @At("TAIL"), method = "loadOrCreate",remap = false)
    private static void loadOrCreate(String id, Metadata creationMetadata, CallbackInfo ci) {
        String[] split = id.split("-");
        if ("printer".equals(split[split.length - 1])) {
            MemoryBank.INSTANCE = PRINTER_MEMORY;
        }
    }
    @Mutable
    @Final
    @Shadow(remap = false)
    private final Map<Identifier, Map<BlockPos, Memory>> memories;
    @Shadow(remap = false)
    private Metadata metadata;
    /**
     * @author  zxy
     * @reason  原方法的无限并不是真正的无限
     */
    @Overwrite
    public List<SearchResult> getPositions(Identifier key, SearchRequest request) {
        if (this.memories.containsKey(key)) {
            ArrayList<SearchResult> results = new ArrayList();
            Vec3d startPos = MinecraftClient.getInstance().player != null ? MinecraftClient.getInstance().player.getPos() : null;
            if (startPos == null) {
                return Collections.emptyList();
            } else {
                int range = this.metadata.getSearchSettings().searchRange;
                double rangeSquared = (double) range * range;
                Iterator var8 = ((Map)this.memories.get(key)).entrySet().iterator();

                while(var8.hasNext()) {
                    Map.Entry<BlockPos, Memory> entry = (Map.Entry)var8.next();
                    if (!(((BlockPos)entry.getKey()).getSquaredDistance(startPos) > rangeSquared) || range == Integer.MAX_VALUE) {
                        Optional<ItemStack> matchedItem = ((Memory)entry.getValue()).items().stream().filter((item) -> {
                            return SearchRequest.check(item, request);
                        }).findFirst();
                        if (!matchedItem.isEmpty()) {
                            Vec3d offset = MemoryUtil.getAverageNameOffset((BlockPos)entry.getKey(), ((Memory)entry.getValue()).otherPositions());
                            results.add(SearchResult.builder((BlockPos)entry.getKey()).item((ItemStack)matchedItem.get()).name(((Memory)entry.getValue()).name(), offset).otherPositions(((Memory)entry.getValue()).otherPositions()).build());
                        }
                    }
                }

                return results;
            }
        } else {
            return Collections.emptyList();
        }
    }

//    @Inject(at = @At("HEAD"), method = "unload", cancellable = true,remap = false)
//    private static void unLoad(CallbackInfo ci) {
//        if(MemoryBank.INSTANCE == null) return;
//        String[] split = MemoryBank.INSTANCE.getId().split("-");
//        if ("printer".equals(split[split.length - 1])) {
//            ci.cancel();
//        }
//    }
}
