package me.aleksilassila.litematica.printer.printer;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import me.aleksilassila.litematica.printer.utils.LitematicaUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SchematicSnapshot {
    public static final SchematicSnapshot INSTANCE = new SchematicSnapshot();

    private WorldSchematic lastSchematic = null;
    private final Set<BlockPos> schematicPositions = new HashSet<>();
    private final Map<BlockPos, BlockState> stateCache = new HashMap<>();

    private SchematicSnapshot() {}

    private void refreshIfNeeded() {
        WorldSchematic current = SchematicWorldHandler.getSchematicWorld();
        if (current != lastSchematic) {
            schematicPositions.clear();
            stateCache.clear();
            lastSchematic = current;
        }
    }

    public boolean contains(BlockPos pos) {
        refreshIfNeeded();
        if (lastSchematic == null) return false;

        if (schematicPositions.contains(pos)) return true;

        if (LitematicaUtils.isSchematicBlock(pos)) {
            schematicPositions.add(pos);
            return true;
        }
        return false;
    }

    public BlockState getRequiredState(BlockPos pos) {
        refreshIfNeeded();
        if (lastSchematic == null) return null;

        BlockState cached = stateCache.get(pos);
        if (cached != null) return cached;

        boolean isSchematicPos = schematicPositions.contains(pos);
        if (!isSchematicPos) {
            if (!LitematicaUtils.isSchematicBlock(pos)) return null;
            schematicPositions.add(pos);
        }

        BlockState state = lastSchematic.getBlockState(pos);
        stateCache.put(pos, state);
        return state;
    }

    public boolean isSchematicLoaded() {
        return SchematicWorldHandler.getSchematicWorld() != null;
    }

    public void invalidate() {
        schematicPositions.clear();
        stateCache.clear();
        lastSchematic = null;
    }
}
