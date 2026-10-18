package me.aleksilassila.litematica.printer.integration.containersync;

import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Simulates the entire edit before sending anything. Every step is an ordinary PICKUP. */
public record ContainerSyncPlan(List<ItemStack> initial, List<Step> steps, Problem problem) {
    public enum Problem { MISSING_ITEMS, NO_SPACE, UNSUPPORTED }
    public interface Rules {
        boolean movable(int slot);
        int capacity(int slot, ItemStack stack);
    }
    public record Step(int slot, int button, int returnSlot, List<ItemStack> items, ItemStack cursor) { }

    public static boolean same(ItemStack a, ItemStack b) {
        //#if MC >= 12006
        return ItemStack.isSameItemSameComponents(a, b);
        //#else
        //$$ return ItemStack.isSameItemSameTags(a, b);
        //#endif
    }

    public static List<ItemStack> copy(List<ItemStack> stacks) {
        return stacks.stream().map(ItemStack::copy).toList();
    }

    public static boolean matches(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) if (!ItemStack.matches(a.get(i), b.get(i))) return false;
        return true;
    }

    public static ContainerSyncPlan create(List<ItemStack> template, List<ItemStack> contents, Rules rules) {
        int size = template.size();
        if (size == 0 || contents.size() < size) return failed(Problem.UNSUPPORTED);
        var state = new ArrayList<>(copy(contents));
        var steps = new ArrayList<Step>();
        // Aggregate equal stacks, including those already in the target. Protected slots do not supply items.
        for (ItemStack wanted : template) {
            if (wanted.isEmpty()) continue;
            int required = 0, available = 0;
            for (ItemStack stack : template) if (same(stack, wanted)) required += stack.getCount();
            for (int i = 0; i < state.size(); i++) {
                if (rules.movable(i) && same(state.get(i), wanted)) available += state.get(i).getCount();
            }
            if (available < required) return failed(Problem.MISSING_ITEMS);
        }
        // Clear only surplus, first using it to satisfy another target slot, then returning it to inventory.
        for (int i = 0; i < size; i++) {
            int keep = same(state.get(i), template.get(i)) ? template.get(i).getCount() : 0;
            int extra = state.get(i).getCount() - keep;
            if (extra <= 0) continue;
            if (!rules.movable(i)) return failed(Problem.UNSUPPORTED);
            for (int j = 0; j < size && extra > 0; j++) {
                if (i == j || !same(state.get(i), template.get(j))) continue;
                int deficit = template.get(j).getCount() - state.get(j).getCount();
                int amount = Math.min(extra, Math.min(deficit, room(state, j, state.get(i), rules)));
                if (amount > 0) { move(state, steps, i, j, amount); extra -= amount; }
            }
            // Merge before occupying empty slots.
            for (int pass = 0; pass < 2; pass++) {
                for (int j = size; j < state.size() && extra > 0; j++) {
                    if (state.get(j).isEmpty() != (pass == 1)) continue;
                    int amount = Math.min(extra, room(state, j, state.get(i), rules));
                    if (amount > 0) { move(state, steps, i, j, amount); extra -= amount; }
                }
            }
            if (extra > 0) return failed(Problem.NO_SPACE);
        }
        for (int i = 0; i < size; i++) {
            int missing = template.get(i).getCount() - state.get(i).getCount();
            if (missing <= 0) continue;
            if (room(state, i, template.get(i), rules) < missing) return failed(Problem.UNSUPPORTED);
            for (int j = size; j < state.size() && missing > 0; j++) {
                if (!rules.movable(j) || !same(state.get(j), template.get(i))) continue;
                int amount = Math.min(missing, state.get(j).getCount());
                if (amount > 0) { move(state, steps, j, i, amount); missing -= amount; }
            }
            if (missing > 0) return failed(Problem.MISSING_ITEMS);
        }
        if (!matches(template, state.subList(0, size))) return failed(Problem.UNSUPPORTED);
        return new ContainerSyncPlan(copy(contents), List.copyOf(steps), null);
    }

    private static ContainerSyncPlan failed(Problem problem) { return new ContainerSyncPlan(List.of(), List.of(), problem); }

    private static int room(List<ItemStack> state, int slot, ItemStack stack, Rules rules) {
        ItemStack there = state.get(slot);
        if (!rules.movable(slot) || (!there.isEmpty() && !same(there, stack))) return 0;
        return Math.max(0, Math.min(stack.getMaxStackSize(), rules.capacity(slot, stack)) - there.getCount());
    }

    private static void move(List<ItemStack> state, List<Step> steps, int from, int to, int count) {
        ItemStack cursor = state.get(from).copy();
        state.set(from, ItemStack.EMPTY);
        steps.add(new Step(from, 0, from, copy(state), cursor.copy()));
        if (count == cursor.getCount()) {
            put(state, to, cursor, count);
            cursor = ItemStack.EMPTY;
            steps.add(new Step(to, 0, from, copy(state), cursor));
        } else {
            for (int i = 0; i < count; i++) {
                put(state, to, cursor, 1);
                cursor.shrink(1);
                steps.add(new Step(to, 1, from, copy(state), cursor.copy()));
            }
            state.set(from, cursor.copy());
            steps.add(new Step(from, 0, from, copy(state), ItemStack.EMPTY));
        }
    }

    private static void put(List<ItemStack> state, int slot, ItemStack stack, int count) {
        if (state.get(slot).isEmpty()) {
            ItemStack inserted = stack.copy(); inserted.setCount(count); state.set(slot, inserted);
        } else state.get(slot).grow(count);
    }
}
