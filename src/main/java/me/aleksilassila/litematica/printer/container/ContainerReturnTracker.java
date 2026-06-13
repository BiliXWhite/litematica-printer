package me.aleksilassila.litematica.printer.container;

import me.aleksilassila.litematica.printer.printer.MissingMaterialTracker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ContainerReturnTracker {
    public static final ContainerReturnTracker INSTANCE = new ContainerReturnTracker();

    public record ReturnEntry(BlockPos pos, String itemId, int pass) {}

    private final Deque<ReturnEntry> queue = new ArrayDeque<>();

    private ContainerReturnTracker() {}

    public void track(BlockPos pos, String itemId) {
        queue.addLast(new ReturnEntry(pos.immutable(), itemId,
                MissingMaterialTracker.getInstance().getGeneration()));
    }

    public boolean isEmpty() { return queue.isEmpty(); }
    public int size() { return queue.size(); }

    public void remove(ReturnEntry target) { queue.removeIf(e -> e.equals(target)); }

    public List<ReturnEntry> peekAll() { return new ArrayList<>(queue); }

    public void clear() { queue.clear(); }
}
