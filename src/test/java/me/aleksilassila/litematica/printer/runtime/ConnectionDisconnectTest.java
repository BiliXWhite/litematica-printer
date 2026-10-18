//#if MC == 260100
package me.aleksilassila.litematica.printer.runtime;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.core.runtime.*;
import me.aleksilassila.litematica.printer.mixin.printer.mc.MixinConnection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ConnectionDisconnectTest {
    @BeforeAll static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }
    static class Client extends Minecraft {
        Thread owner;
        Queue<Runnable> pending;
        ClientPacketListener listener;
        Client() { super(null); }
        @Override public ClientPacketListener getConnection() { return listener; }
        @Override public void execute(Runnable task) {
            if (Thread.currentThread() == owner) task.run();
            else pending.add(task);
        }
    }

    static class Listener extends ClientPacketListener {
        Connection network;
        Listener() { super(null, null, null); }
        @Override public Connection getConnection() { return network; }
    }

    static <T> T allocate(Class<T> type) throws Exception {
        var field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((sun.misc.Unsafe) field.get(null)).allocateInstance(type));
    }
    static Object get(Object target, Class<?> type, String name) throws Exception {
        var field = type.getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }
    static void set(Object target, Class<?> type, String name, Object value) throws Exception {
        var field = type.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }

    static class Fixture implements AutoCloseable {
        Object oldClient = get(null, Minecraft.class, "instance");
        Object oldRuntime = get(null, RuntimeAccess.class, "current");
        boolean oldWork = Configs.Core.WORK_SWITCH.getBooleanValue();
        boolean oldAuto = Configs.Core.AUTO_DISABLE_PRINTER.getBooleanValue();
        Client client = allocate(Client.class);
        Listener listener = allocate(Listener.class);
        PrinterRuntime runtime = allocate(PrinterRuntime.class);
        RuntimeScope scope = new RuntimeScope();
        AtomicInteger resets = new AtomicInteger();
        Fixture() throws Exception {
            client.owner = Thread.currentThread();
            client.pending = new ArrayDeque<>();
            listener.network = new Connection(PacketFlow.CLIENTBOUND);
            client.listener = listener;
            set(null, Minecraft.class, "instance", client);
            set(runtime, PrinterRuntime.class, "scope", scope);
            set(runtime, PrinterRuntime.class, "events", new RuntimeEventBus());
            set(runtime, PrinterRuntime.class, "epoch", RuntimeEpoch.INITIAL);
            scope.register(new RuntimeComponent() {
                @Override public void onEpochChanged(RuntimeEvent.EpochChanged event) {
                    assertSame(client.owner, Thread.currentThread(), "All component resets must run on the client thread");
                    resets.incrementAndGet();
                }
            });
            RuntimeAccess.install(runtime);
            Configs.Core.WORK_SWITCH.setBooleanValue(true);
            Configs.Core.AUTO_DISABLE_PRINTER.setBooleanValue(true);
        }
        void drain() { while (!client.pending.isEmpty()) client.pending.remove().run(); }
        @Override public void close() throws Exception {
            set(null, Minecraft.class, "instance", oldClient);
            set(null, RuntimeAccess.class, "current", oldRuntime);
            Configs.Core.WORK_SWITCH.setBooleanValue(oldWork);
            Configs.Core.AUTO_DISABLE_PRINTER.setBooleanValue(oldAuto);
        }
    }

    @Test void unrelatedConnectionCallbackCannotResetOrDisableCurrentSession() throws Exception {
        try (var f = new Fixture()) {
            // Invoke the real injection callback; this object is not the active gameplay Connection.
            new MixinConnection().disconnect(null);
            f.drain();
            assertEquals(0, f.resets.get());
            assertTrue(Configs.Core.WORK_SWITCH.getBooleanValue());
        }
    }

    @Test void lanPeersAndMissingGameplayConnectionsDoNotResetTheRuntime() throws Exception {
        try (var f = new Fixture()) {
            f.runtime.onConnectionDisconnected(new Connection(PacketFlow.SERVERBOUND));
            f.client.listener = null;
            f.runtime.onConnectionDisconnected(f.listener.network);
            assertEquals(0, f.resets.get());
            assertTrue(Configs.Core.WORK_SWITCH.getBooleanValue());
        }
    }

    @Test void networkThreadDisconnectWaitsForClientThreadBeforeResettingComponents() throws Exception {
        try (var f = new Fixture()) {
            var thread = new Thread(() -> f.runtime.onConnectionDisconnected(f.listener.network));
            thread.start(); thread.join();
            assertEquals(0, f.resets.get());
            assertTrue(Configs.Core.WORK_SWITCH.getBooleanValue());
            f.drain();
            assertEquals(1, f.resets.get());
            assertFalse(Configs.Core.WORK_SWITCH.getBooleanValue());
        }
    }

    @Test void queuedOldDisconnectCannotDisableOrResetANewServerSession() throws Exception {
        try (var f = new Fixture()) {
            var oldConnection = f.listener.network;
            var thread = new Thread(() -> f.runtime.onConnectionDisconnected(oldConnection));
            thread.start(); thread.join();
            f.listener.network = new Connection(PacketFlow.CLIENTBOUND);
            f.drain();
            assertEquals(0, f.resets.get());
            assertTrue(Configs.Core.WORK_SWITCH.getBooleanValue());
        }
    }

    @Test void disablingAutomaticShutdownStillResetsTheDisconnectedSessionsState() throws Exception {
        try (var f = new Fixture()) {
            Configs.Core.AUTO_DISABLE_PRINTER.setBooleanValue(false);
            f.runtime.onConnectionDisconnected(f.listener.network);
            assertEquals(1, f.resets.get());
            assertTrue(Configs.Core.WORK_SWITCH.getBooleanValue());
        }
    }

    @Test void repeatedDisconnectCallbacksForOneConnectionResetOnlyOnce() throws Exception {
        try (var f = new Fixture()) {
            Configs.Core.AUTO_DISABLE_PRINTER.setBooleanValue(false);
            f.runtime.onConnectionDisconnected(f.listener.network);
            f.runtime.onConnectionDisconnected(f.listener.network);
            assertEquals(1, f.resets.get(), "Disconnect overloads and channelInactive can report the same connection");
        }
    }
}
//#endif
