package me.aleksilassila.litematica.printer.mixin.jackf;

import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import red.jackf.chesttracker.memory.MemoryBank;
import red.jackf.whereisit.api.SearchRequest;
import red.jackf.whereisit.api.SearchResult;

import java.util.List;

@Mixin(MemoryBank.class)
public class MemoryBankMixin {
    @Inject(at = @At("HEAD"),method = "getPositions")
    private void getPositions(Identifier key, SearchRequest request, CallbackInfoReturnable<List<SearchResult>> cir){

    }
}
