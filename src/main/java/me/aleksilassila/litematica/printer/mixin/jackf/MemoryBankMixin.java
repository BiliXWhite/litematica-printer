package me.aleksilassila.litematica.printer.mixin.jackf;

import carpet.script.language.Sys;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import red.jackf.chesttracker.memory.Memory;
import red.jackf.chesttracker.memory.MemoryBank;
import red.jackf.whereisit.api.SearchRequest;
import red.jackf.whereisit.api.SearchResult;

import java.util.List;
import java.util.Map;

@Mixin(MemoryBank.class)
public abstract class MemoryBankMixin {
    @Mutable
    @Final
    @Shadow(remap = false)
    private final Map<Identifier, Map<BlockPos, Memory>> memories;
    public MemoryBankMixin(Map<Identifier, Map<BlockPos, Memory>> memories) {
        this.memories = memories;
    }
    @Unique
    boolean search = true;
    @Unique
    public void search(Identifier key1){
        search = false;
        MinecraftClient client = MinecraftClient.getInstance();
        memories.keySet().forEach(key -> {
            if(key1.equals(key) && MemoryBank.INSTANCE != null) {
                List<SearchResult> positions = MemoryBank.INSTANCE.getPositions(key, new SearchRequest());
                RegistryKeys.WORLD.getRegistry();
                if (client.world != null) {
                    RegistryKey<World> dimensionKey = client.world.getRegistryKey();
                }
                RegistryKey<World> key2 = RegistryKey.of(RegistryKeys.WORLD, key);
            }
        });
        search = true;
    }
    @Inject(at = @At("RETURN"),method = "getPositions")
    private void getPositions(Identifier key, SearchRequest request, CallbackInfoReturnable<List<SearchResult>> cir){
        System.out.println("key= "+key);
        memories.keySet().forEach(e -> System.out.println(e.toString()));

        List<SearchResult> returnValue = cir.getReturnValue();
        if (returnValue.isEmpty() && search) {
            search(key);
        }
    }
}
