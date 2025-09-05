package me.aleksilassila.litematica.printer.mixin.openinv;

import net.minecraft.server.world.ChunkTicketType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkTicketType.class)
public interface ChunkTicketTypeMixin {
    @Invoker("register")
    static ChunkTicketType register(String id, long expiryTicks, boolean persist, ChunkTicketType.Use use) {
        return null;
    }
}
