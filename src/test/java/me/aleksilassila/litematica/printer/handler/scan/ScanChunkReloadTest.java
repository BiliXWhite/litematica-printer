package me.aleksilassila.litematica.printer.handler.scan;

import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeEpoch;
import me.aleksilassila.litematica.printer.printer.PrinterBox;
import me.aleksilassila.litematica.printer.runtime.PrinterRuntime;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScanChunkReloadTest {
    @BeforeAll
    static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void fullChunkReplacementRefreshesZeroClassificationWithoutChangingWorldIdentity() throws Exception {
        Fixture fixture = new Fixture();
        BlockPos target = new BlockPos(3, 20, 5);
        fixture.source.world.put(target, Blocks.STONE.defaultBlockState());
        fixture.source.schematic.put(target, Blocks.STONE.defaultBlockState());
        SectionScanSession previous = fixture.session("print", target, ScanIntent.PRINT);
        assertNull(fixture.next(previous, 10L));
        assertEquals(1, fixture.source.readsAt(target));

        // A full chunk packet changes the live chunk, while both world objects remain the same.
        fixture.source.world.put(target, Blocks.AIR.defaultBlockState());
        fixture.cache.invalidateChunk(0, 0, -64, 319);
        fixture.cache.beginTick(fixture.level, fixture.schematic, 11L, RuntimeEpoch.INITIAL);

        assertFalse(previous.canScan(11L), "The previous iterator must not survive its chunk replacement");
        SectionScanSession current = fixture.session("print", target, ScanIntent.PRINT);
        assertNotSame(previous, current);
        SectionScanSession.Candidate candidate = fixture.next(current, 11L);
        assertNotNull(candidate, "The previous zero classification must be observed again");
        assertEquals(target, candidate.pos());
        assertTrue(candidate.acceptedByFlags(ScanIntent.PRINT));
        assertEquals(2, fixture.source.readsAt(target));
    }

    @Test
    void unloadedChunkDoesNotRetainItsCandidateDecisionAfterReload() throws Exception {
        Fixture fixture = new Fixture();
        BlockPos target = new BlockPos(3, 20, 5);
        fixture.source.world.put(target, Blocks.STONE.defaultBlockState());
        fixture.source.schematic.put(target, Blocks.STONE.defaultBlockState());
        SectionScanSession beforeUnload = fixture.session("print", target, ScanIntent.PRINT);
        assertNull(fixture.next(beforeUnload, 10L));

        fixture.source.unloadedChunks.add(chunkKey(0, 0));
        fixture.cache.invalidateChunk(0, 0, -64, 319);
        SectionScanSession unloaded = fixture.session("print", target, ScanIntent.PRINT);
        assertNull(fixture.next(unloaded, 10L));

        fixture.source.unloadedChunks.clear();
        fixture.source.world.remove(target);
        fixture.cache.invalidateChunk(0, 0, -64, 319);
        SectionScanSession loaded = fixture.session("print", target, ScanIntent.PRINT);
        SectionScanSession.Candidate candidate = fixture.next(loaded, 10L);

        assertNotNull(candidate, "Chunk arrival must wake the session in the current tick");
        assertEquals(target, candidate.pos());
        assertFalse(unloaded.hasPendingSource(10L));
    }

    @Test
    void delayedChunkEventFromAnotherClientWorldDoesNotInvalidateCurrentWorld() throws Exception {
        Fixture fixture = new Fixture();
        BlockPos target = new BlockPos(3, 20, 5);
        fixture.source.schematic.put(target, Blocks.STONE.defaultBlockState());
        fixture.classify(target, ScanIntent.PRINT);
        SectionScanSession session = fixture.session("print", target, ScanIntent.PRINT);
        long version = fixture.cache.dirtyVersion();
        Field instance = Minecraft.class.getDeclaredField("instance");
        instance.setAccessible(true);
        Object previousClient = instance.get(null);
        Minecraft client = allocate(Minecraft.class);
        client.level = fixture.level;
        try {
            instance.set(null, client);
            ScanEngine engine = new ScanEngine(allocate(PrinterRuntime.class), fixture.cache);
            engine.invalidateChunk(allocate(ClientLevel.class), allocate(LevelChunk.class));

            assertEquals(version, fixture.cache.dirtyVersion());
            assertSame(session, fixture.session("print", target, ScanIntent.PRINT));
            fixture.classify(target, ScanIntent.PRINT);
            assertEquals(1, fixture.source.readsAt(target));
        } finally {
            instance.set(null, previousClient);
        }
    }

    @Test
    void fullChunkReplacementRefreshesNeighborSupportButPreservesOtherCachedCells() throws Exception {
        Fixture fixture = new Fixture();
        BlockPos target = new BlockPos(15, 20, 5);
        BlockPos support = target.east();
        BlockPos sameNeighborInterior = new BlockPos(14, 20, 5);
        BlockPos distant = new BlockPos(67, 20, 5);
        assertEquals(0, fixture.classify(target, ScanIntent.FILL));
        fixture.classify(sameNeighborInterior, ScanIntent.FILL);
        fixture.classify(distant, ScanIntent.FILL);
        int neighborReads = fixture.source.readsAt(sameNeighborInterior);
        int distantReads = fixture.source.readsAt(distant);

        fixture.source.world.put(support, Blocks.STONE.defaultBlockState());
        fixture.cache.invalidateChunk(1, 0, -64, 319);

        assertEquals(ScanFlags.BASE_FILL_TARGET, fixture.classify(target, ScanIntent.FILL));
        fixture.classify(sameNeighborInterior, ScanIntent.FILL);
        fixture.classify(distant, ScanIntent.FILL);
        assertEquals(neighborReads, fixture.source.readsAt(sameNeighborInterior),
                "The adjacent chunk's interior classification must remain cached");
        assertEquals(distantReads, fixture.source.readsAt(distant),
                "An unrelated chunk must not be globally cleared");
    }

    @Test
    void invalidationCoversEveryVerticalSectionAndBothSidesOfNegativeChunkBoundaries() throws Exception {
        Fixture fixture = new Fixture();
        List<BlockPos> changed = List.of(
                new BlockPos(-15, -64, -15), new BlockPos(-1, 319, -1));
        List<BlockPos> neighbors = List.of(
                new BlockPos(-17, 20, -10), new BlockPos(0, 20, -10),
                new BlockPos(-10, 20, -17), new BlockPos(-10, 20, 0));
        for (BlockPos pos : changed) {
            fixture.source.world.put(pos, Blocks.STONE.defaultBlockState());
            fixture.source.schematic.put(pos, Blocks.STONE.defaultBlockState());
            assertEquals(0, fixture.classify(pos, ScanIntent.PRINT));
        }
        for (BlockPos pos : neighbors) assertEquals(0, fixture.classify(pos, ScanIntent.FILL));

        for (BlockPos pos : changed) fixture.source.world.remove(pos);
        fixture.source.world.put(new BlockPos(-16, 20, -10), Blocks.STONE.defaultBlockState());
        fixture.source.world.put(new BlockPos(-1, 20, -10), Blocks.STONE.defaultBlockState());
        fixture.source.world.put(new BlockPos(-10, 20, -16), Blocks.STONE.defaultBlockState());
        fixture.source.world.put(new BlockPos(-10, 20, -1), Blocks.STONE.defaultBlockState());
        fixture.cache.invalidateChunk(-1, -1, -64, 319);

        for (BlockPos pos : changed) assertTrue(ScanIntent.PRINT.acceptsByFlags(fixture.classify(pos, ScanIntent.PRINT)));
        for (BlockPos pos : neighbors) assertEquals(ScanFlags.BASE_FILL_TARGET, fixture.classify(pos, ScanIntent.FILL));
    }

    @Test
    void chunkInvalidationReopensOnlyIntersectingSessionsAndPublishesBoundedDirtyRegions() throws Exception {
        Fixture fixture = new Fixture();
        BlockPos target = new BlockPos(3, 20, 5);
        BlockPos boundary = new BlockPos(16, 20, 5);
        BlockPos distant = new BlockPos(67, 20, 5);
        SectionScanSession changed = fixture.session("changed", target, ScanIntent.PRINT);
        SectionScanSession neighbor = fixture.session("neighbor", boundary, ScanIntent.FILL);
        SectionScanSession untouched = fixture.session("untouched", distant, ScanIntent.PRINT);
        assertNull(fixture.next(changed, 10L));
        assertNull(fixture.next(neighbor, 10L));
        assertNull(fixture.next(untouched, 10L));
        long previousVersion = fixture.cache.dirtyVersion();

        fixture.cache.invalidateChunk(0, 0, -64, 319);

        assertFalse(changed.canScan(10L));
        assertFalse(neighbor.canScan(10L));
        assertNotSame(changed, fixture.session("changed", target, ScanIntent.PRINT));
        assertNotSame(neighbor, fixture.session("neighbor", boundary, ScanIntent.FILL));
        assertSame(untouched, fixture.session("untouched", distant, ScanIntent.PRINT));
        assertFalse(untouched.canScan(10L), "Unrelated sessions must retain their completed-pass timing");

        DirtyRegionTracker.DirtySnapshot dirty = fixture.cache.dirtySnapshotAfter(previousVersion, null);
        assertTrue(dirty.version() > previousVersion, "Lazy scan admission must observe the chunk replacement");
        assertTrue(contains(dirty.boxes(), new BlockPos(0, -64, 0)));
        assertTrue(contains(dirty.boxes(), new BlockPos(15, 319, 15)));
        assertTrue(contains(dirty.boxes(), new BlockPos(-1, 20, 5)));
        assertTrue(contains(dirty.boxes(), new BlockPos(16, 20, 5)));
        assertTrue(contains(dirty.boxes(), new BlockPos(5, 20, -1)));
        assertTrue(contains(dirty.boxes(), new BlockPos(5, 20, 16)));
        assertFalse(contains(dirty.boxes(), distant));
        assertTrue(fixture.cache.dirtySnapshotAfter(previousVersion, box(distant)).boxes().isEmpty());
    }

    private static boolean contains(List<PrinterBox> boxes, BlockPos pos) {
        return boxes.stream().anyMatch(box -> box.contains(pos));
    }

    private static PrinterBox box(BlockPos pos) {
        return new PrinterBox(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    private static long chunkKey(int x, int z) {
        return (long) x << 32 | z & 0xffffffffL;
    }

    private static final class Fixture {
        private final ScanCache cache = new ScanCache();
        private final ClientLevel level = allocate(ClientLevel.class);
        private final WorldSchematic schematic = allocate(WorldSchematic.class);
        private final SectionSnapshotStore snapshots = field(cache, "snapshots", SectionSnapshotStore.class);
        private final ScanSessionStore sessions = field(cache, "sessions", ScanSessionStore.class);
        private final MutableObservation source = new MutableObservation();
        private final WorldObservationPort observation = new SnapshotWorldObservation(snapshots, source);
        private final AtomicLong generations = new AtomicLong();

        private Fixture() throws Exception {
            this.cache.beginTick(this.level, this.schematic, 10L, RuntimeEpoch.INITIAL);
        }

        private SectionScanSession session(String owner, BlockPos pos, ScanIntent intent) {
            PrinterBox bounds = box(pos);
            return this.sessions.getOrCreate(owner, this.sessions.metrics(owner), intent,
                    bounds, List.of(bounds), null, RuntimeEpoch.INITIAL, () -> 0L, this.generations::incrementAndGet);
        }

        private SectionScanSession.Candidate next(SectionScanSession session, long tick) {
            return session.next(this.observation, tick, () -> false, pos -> true, true);
        }

        private byte classify(BlockPos pos, ScanIntent intent) {
            return this.snapshots.classify(pos, intent, false, this.source);
        }
    }

    private static final class MutableObservation implements WorldObservationPort {
        private final Map<BlockPos, BlockState> world = new HashMap<>();
        private final Map<BlockPos, BlockState> schematic = new HashMap<>();
        private final Map<BlockPos, Integer> worldReads = new HashMap<>();
        private final Set<Long> unloadedChunks = new HashSet<>();

        private int readsAt(BlockPos pos) { return this.worldReads.getOrDefault(pos, 0); }

        @Override public boolean hasChunk(int x, int z) { return !this.unloadedChunks.contains(chunkKey(x, z)); }

        @Override public BlockState worldState(BlockPos pos) {
            this.worldReads.merge(pos.immutable(), 1, Integer::sum);
            return this.world.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override public BlockState schematicState(BlockPos pos) {
            return this.schematic.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override public boolean hasFillSupport(BlockPos pos) {
            for (Direction direction : Direction.values()) {
                if (!this.world.getOrDefault(pos.relative(direction), Blocks.AIR.defaultBlockState()).isAir()) return true;
            }
            return false;
        }
    }

    private static <T> T field(Object instance, String name, Class<T> type) throws Exception {
        Field field = instance.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(instance));
    }

    private static <T> T allocate(Class<T> type) throws Exception {
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((sun.misc.Unsafe) field.get(null)).allocateInstance(type));
    }
}
