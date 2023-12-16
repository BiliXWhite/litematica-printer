package me.aleksilassila.litematica.printer.mixin.jackf;

import carpet.script.language.Sys;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
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
    public void search(Identifier key){
        search = false;
        MinecraftClient client = MinecraftClient.getInstance();

    }
    @Inject(at = @At("RETURN"),method = "getPositions")
    private void getPositions(Identifier key, SearchRequest request, CallbackInfoReturnable<List<SearchResult>> cir){
        System.out.println("key= "+key);
        memories.keySet().forEach(e -> System.out.println(e.toString()));

        List<SearchResult> returnValue = cir.getReturnValue();
    }
}
