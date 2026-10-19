//#if MC >= 12111
package me.aleksilassila.litematica.printer.integration.voidwarehouse;

import io.netty.buffer.Unpooled;
import neko.voidwarehouse.restock.PrinterTakePayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VoidWarehouseRestockNetworkTest {
    @Test
    void clientAloneRegistersAnEncodableTakePayloadAndInitializationIsIdempotent() throws Exception {
        //#if MC >= 260100
        var registry = (PayloadTypeRegistryImpl<RegistryFriendlyByteBuf>) PayloadTypeRegistry.serverboundPlay();
        //#else
        //$$ var registry = (PayloadTypeRegistryImpl<RegistryFriendlyByteBuf>) PayloadTypeRegistry.playC2S();
        //#endif
        assertNull(registry.get(PrinterTakePayload.TYPE), "No server mod initializes this test process");
        VoidWarehouseRestockNetwork.init();
        var registered = registry.get(PrinterTakePayload.TYPE);
        assertNotNull(registered, "Remote channel support does not register the client's encoder");
        var payload = new PrinterTakePayload(42, List.of("minecraft:stone", "minecraft:cobblestone"));
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            registered.codec().encode(buffer, payload);
            assertEquals(payload, PrinterTakePayload.CODEC.decode(buffer));
            assertFalse(buffer.isReadable());
        } finally {
            buffer.release();
        }
        assertDoesNotThrow(VoidWarehouseRestockNetwork::init);
        assertSame(registered, registry.get(PrinterTakePayload.TYPE));
        // An integrated server can have registered both payload types before client init.
        var initialized = VoidWarehouseRestockNetwork.class.getDeclaredField("initialized");
        initialized.setAccessible(true);
        initialized.setBoolean(null, false);
        assertDoesNotThrow(VoidWarehouseRestockNetwork::init);
        assertSame(registered, registry.get(PrinterTakePayload.TYPE));
    }
}
//#endif
