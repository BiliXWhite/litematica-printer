package me.aleksilassila.litematica.printer.integration.containersync;

import net.minecraft.world.item.ItemStack;
import java.util.List;

/** One outstanding click. Only full server snapshots advance the plan, never menu prediction. */
public final class ContainerSyncTransfer {
    public interface Port {
        Object context();
        boolean acquire();
        void release();
        void click(int slot, int button);
        void refresh();
        boolean unchanged(List<ItemStack> items, ItemStack cursor);
    }
    private final Port port;
    private final Object context;
    private final ContainerSyncPlan plan;
    private int next;
    private boolean pending, cancelled, finished, waiting;
    private long sentAt;
    private ItemStack cursor = ItemStack.EMPTY;
    private List<ItemStack> unexpected;
    private ItemStack unexpectedCursor;
    private ContainerSyncPlan.Step recovery;
    private boolean cancellationRecovery;
    private String problem;

    public ContainerSyncTransfer(Port port, ContainerSyncPlan plan) {
        this.port = port;
        this.context = port.context();
        this.plan = plan;
        if (plan.problem() != null) { this.problem = plan.problem().name(); this.finished = true; }
    }

    public void cancel() { cancelled = true; }
    public boolean finished() { return finished; }
    public boolean completed() { return finished && problem == null && next == plan.steps().size(); }
    public String problem() { return problem; }
    public boolean needsAttention() { return "CURSOR_NEEDS_ATTENTION".equals(problem); }
    public boolean waitingForServer() { return waiting; }

    public static boolean newerRevision(int received, int previous) {
        int delta = (received - previous) & 32767;
        return delta > 0 && delta < 16384;
    }

    public void tick(long now, boolean enabled) {
        if (finished) return;
        if (context == null || port.context() != context) { finish("CONTEXT_CHANGED"); return; }
        if (!enabled) cancel();
        if (!port.acquire()) return;
        if (pending) {
            if (now - sentAt >= 5000) {
                waiting = true;
                if (unexpected != null) {
                    if (unexpectedCursor.isEmpty()) { finish("SERVER_CHANGED"); return; }
                    // Only return our own stack to its original slot. Never overwrite an occupied slot.
                    var step = plan.steps().get(next);
                    ItemStack owned = next == 0 ? step.cursor() : plan.steps().get(next - 1).cursor();
                    int back = step.returnSlot();
                    if (recovery == null && !needsAttention() && !owned.isEmpty() && ContainerSyncPlan.same(owned, unexpectedCursor)
                            && unexpectedCursor.getCount() <= owned.getCount()
                            && unexpected.get(back).isEmpty()) {
                        var restored = new java.util.ArrayList<>(ContainerSyncPlan.copy(unexpected));
                        restored.set(back, unexpectedCursor.copy());
                        recovery = new ContainerSyncPlan.Step(back, 0, back, restored, ItemStack.EMPTY);
                        unexpected = null;
                        port.click(back, 0);
                    } else problem = "CURSOR_NEEDS_ATTENTION";
                }
                // A timeout is not permission to repeat the original click.
                sentAt = now;
                if (needsAttention()) port.refresh();
            }
            return;
        }
        if (next == plan.steps().size()) {
            var expected = next == 0 ? plan.initial() : plan.steps().get(next - 1).items();
            finish(port.unchanged(expected, ItemStack.EMPTY) ? null : "SERVER_CHANGED");
            return;
        }
        if (cancelled && cursor.isEmpty()) { finish(null); return; }
        var before = next == 0 ? plan.initial() : plan.steps().get(next - 1).items();
        if (!port.unchanged(before, cursor)) {
            // Do not execute an old plan after an intervening slot update. Ask for a fresh snapshot.
            pending = true;
            cancelled = true;
            sentAt = now;
            port.refresh();
            return;
        }
        if (cancelled && !cursor.isEmpty()) {
            int back = plan.steps().get(next - 1).returnSlot();
            var restored = new java.util.ArrayList<>(ContainerSyncPlan.copy(before));
            restored.set(back, cursor.copy());
            recovery = new ContainerSyncPlan.Step(back, 0, back, restored, ItemStack.EMPTY);
            cancellationRecovery = true;
            pending = true; unexpected = null; sentAt = now;
            port.click(back, 0);
            return;
        }
        var step = plan.steps().get(next);
        pending = true;
        unexpected = null;
        sentAt = now;
        port.click(step.slot(), step.button());
        me.aleksilassila.litematica.printer.integration.quickshulker.QuickShulkerDiagnostics.log(
                "container-sync click step=" + next + " slot=" + step.slot() + " button=" + step.button());
    }

    public void observe(Object context, List<ItemStack> items, ItemStack carried) {
        if (finished || !pending || this.context != context || port.context() != context) return;
        if (needsAttention()) {
            // Once handed back to the user, never resume a partially matching automatic plan.
            if (carried.isEmpty()) finish("SERVER_CHANGED");
            return;
        }
        var expected = recovery == null ? plan.steps().get(next) : recovery;
        if (ContainerSyncPlan.matches(expected.items(), items) && ItemStack.matches(expected.cursor(), carried)) {
            cursor = carried.copy();
            waiting = false;
            pending = false;
            unexpected = null;
            if (recovery != null) finish(cancellationRecovery ? null : "SERVER_CHANGED");
            else {
                me.aleksilassila.litematica.printer.integration.quickshulker.QuickShulkerDiagnostics.log(
                        "container-sync confirmed step=" + next);
                next++;
            }
        } else if (items.size() == expected.items().size()) {
            unexpected = ContainerSyncPlan.copy(items);
            unexpectedCursor = carried.copy();
        }
    }

    private void finish(String reason) {
        if (reason != null) problem = reason;
        finished = true;
        port.release();
    }
}
