package me.aleksilassila.litematica.printer.integration.containersync;

import me.aleksilassila.litematica.printer.config.Configs;
import me.aleksilassila.litematica.printer.core.action.ResourceLease;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeComponent;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeEvent;
import me.aleksilassila.litematica.printer.integration.quickshulker.QuickShulkerClicks;
import me.aleksilassila.litematica.printer.integration.quickshulker.HighlightBlockRenderer;
import me.aleksilassila.litematica.printer.runtime.PrinterRuntime;
import me.aleksilassila.litematica.printer.printer.PrinterBox;
import me.aleksilassila.litematica.printer.mixin.printer.litematica.InventoryUtilsAccessor;
import me.aleksilassila.litematica.printer.utils.minecraft.MessageUtils;
import me.aleksilassila.litematica.printer.utils.minecraft.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Selection/template orchestration. Inventory transport remains alive while ordinary modules pause. */
public final class ContainerSyncController implements RuntimeComponent {
    private static final String OWNER = "container_sync";
    private static final EnumSet<ResourceLease> RESOURCES = EnumSet.of(ResourceLease.INVENTORY,
            ResourceLease.CONTAINER, ResourceLease.MAIN_HAND, ResourceLease.INTERACTION);
    private enum Phase { IDLE, SCANNING, READY, OPENING, CONTENT, TRANSFER, RESYNC, PAUSED }
    private final PrinterRuntime runtime;
    private final Minecraft client;
    private Phase phase = Phase.IDLE;
    private Object level, connection;
    private Block block;
    private BlockPos reference, target;
    private int size, candidate = -1;
    private int sentRevision;
    private int openingInventoryRevision;
    private boolean cancelling, closing;
    private long requestedAt;
    private AbstractContainerMenu menu;
    private AbstractContainerMenu resyncMenu;
    private int resyncRevision;
    private boolean resyncRequested, resyncNeedsAttention;
    private ContainerSyncTransfer transfer;
    private List<ItemStack> template;
    private final LinkedHashSet<BlockPos> remaining = new LinkedHashSet<>();
    private Set<BlockPos> highlights;
    private Iterator<PrinterBox> boxes;
    private List<PrinterBox> selection;
    private boolean scanPaused;
    private Iterator<BlockPos> positions = Collections.emptyIterator();

    public ContainerSyncController(PrinterRuntime runtime) { this.runtime = runtime; this.client = runtime.client(); }

    public void toggle() {
        if (phase == Phase.PAUSED) {
            if (!Configs.Core.WORK_SWITCH.getBooleanValue()) { message("enable"); return; }
            cancelling = false;
            if (scanPaused) {
                remaining.clear(); boxes = selection.iterator(); positions = Collections.emptyIterator();
                scanPaused = false; phase = Phase.SCANNING;
            } else phase = Phase.READY;
            message("resuming"); return;
        }
        if (phase != Phase.IDLE) { cancel(); return; }
        if (!Configs.Core.WORK_SWITCH.getBooleanValue()) { message("enable"); return; }
        if (client.player == null || client.level == null || client.gameMode == null
                || client.player.containerMenu != client.player.inventoryMenu
                || !client.player.containerMenu.getCarried().isEmpty()
                || !(client.hitResult instanceof BlockHitResult hit)) { message("select"); return; }
        var state = client.level.getBlockState(hit.getBlockPos());
        block = state.getBlock();
        if (!(block instanceof ChestBlock || block instanceof BarrelBlock || block instanceof ShulkerBoxBlock)) {
            message("unsupported"); return;
        }
        reference = canonical(hit.getBlockPos());
        size = block instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE ? 54 : 27;
        selection = runtime.litematica().createSelectionBoxes();
        if (selection.isEmpty()) { message("selection"); return; }
        if (!acquire()) { message("busy"); return; }
        level = client.level; connection = client.getConnection();
        template = null; remaining.clear(); cancelling = false;
        boxes = selection.iterator(); positions = Collections.emptyIterator();
        HighlightBlockRenderer.createHighlightBlockList(OWNER, new fi.dy.masa.malilib.config.options.ConfigColor(OWNER, "#4CFF4CE6", ""));
        highlights = HighlightBlockRenderer.getHighlightBlockPosList(OWNER);
        highlights.clear();
        phase = Phase.SCANNING;
        message("scanning");
    }

    public void tick() {
        if (phase == Phase.IDLE) return;
        if (client.level != level || client.getConnection() != connection || client.player == null) { discard(); return; }
        if (!Configs.Core.WORK_SWITCH.getBooleanValue() && phase != Phase.PAUSED) cancel();
        if (phase == Phase.SCANNING) { scan(); return; }
        if (phase == Phase.PAUSED || phase == Phase.IDLE) return;
        if (phase == Phase.TRANSFER && client.player.containerMenu != menu) {
            // The server sends its close packet before returning the cursor to inventory.
            // The old click may have run or been rejected; never replay it in the new menu.
            transfer.cancel(); transfer = null; menu = null; candidate = -1;
            phase = Phase.RESYNC;
        }
        if (phase == Phase.RESYNC) {
            if (!acquire()) return;
            var current = client.player.containerMenu;
            if (resyncMenu != current || !resyncRequested) {
                resyncMenu = current; resyncRevision = current.getStateId();
                resyncRequested = true; resyncNeedsAttention = false;
                me.aleksilassila.litematica.printer.integration.quickshulker.QuickShulkerDiagnostics.log(
                        "container-sync closed-menu resync menu=" + current.containerId);
                // Ordered after all previously sent clicks. One no-op, with no local prediction.
                QuickShulkerClicks.requestFullSync(current);
            }
            message(resyncNeedsAttention ? "cursor" : "waiting");
            return;
        }
        if (phase == Phase.TRANSFER) {
            transfer.tick(now(), !cancelling);
            if (transfer.needsAttention()) message("cursor");
            else if (transfer.waitingForServer()) message("waiting");
            if (transfer.finished()) {
                boolean complete = transfer.completed();
                String problem = transfer.problem();
                transfer = null;
                if (problem != null) { pause("changed"); return; }
                closeOwned();
                if (complete) { remaining.remove(target); highlights.remove(target); }
                if (cancelling) { discard(); message("cancelled"); }
                else phase = Phase.READY;
            }
            return;
        }
        if (phase == Phase.OPENING || phase == Phase.CONTENT) {
            // A server-forced close bypasses the manual close guard. No item click has been
            // sent yet, and a late initial snapshot for the lost menu can no longer be used.
            if (phase == Phase.CONTENT && client.player.containerMenu.containerId != candidate) {
                pause("changed"); return;
            }
            // Time alone cannot prove that a queued open was rejected. Keep ownership until its barrier reply.
            if (now() - requestedAt > 10000) message("waiting");
            return;
        }
        if (cancelling) { discard(); return; }
        if (template != null && remaining.isEmpty()) { discard(); message("complete"); return; }
        if (!acquire()) return;
        if (client.player.containerMenu != client.player.inventoryMenu
                || !client.player.containerMenu.getCarried().isEmpty()
                || runtime.inventorySwitchGuard().isWaiting()) return;
        // A blocked chest must not fall through to placing the held block beside it.
        if (!client.player.getMainHandItem().isEmpty()) { message("empty_hand"); return; }
        target = template == null ? reference : remaining.stream()
                .filter(this::reachable).findFirst().orElse(null);
        if (target == null || !reachable(target)) { message("range"); return; }
        if (!client.level.getBlockState(target).is(block) || containerSize(target) != size) { pause("changed"); return; }
        if (!acquire()) return;
        candidate = -1; menu = null; phase = Phase.OPENING; requestedAt = now();
        openingInventoryRevision = client.player.inventoryMenu.getStateId();
        me.aleksilassila.litematica.printer.integration.quickshulker.QuickShulkerDiagnostics.log("container-sync open target=" + target);
        //#if MC < 11904
        //$$ client.gameMode.useItemOn(client.player, client.level, InteractionHand.MAIN_HAND,
        //#else
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
        //#endif
                new BlockHitResult(Vec3.atCenterOf(target), Direction.UP, target, false));
        // Ordered behind the open: rejection leaves menu 0 active and returns a full snapshot;
        // a successful open changes the server menu ID and ignores this menu-0 no-op.
        QuickShulkerClicks.requestFullSync(client.player.inventoryMenu);
    }

    private void scan() {
        for (int budget = 0; budget < 2048; budget++) {
            if (!positions.hasNext()) {
                if (!boxes.hasNext()) {
                    highlights.addAll(remaining);
                    phase = Phase.READY;
                    if (remaining.isEmpty()) { discard(); message("no_targets"); }
                    return;
                }
                positions = boxes.next().iterator();
            }
            BlockPos pos = positions.next();
            if (!client.level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                scanPaused = true; pause("unloaded"); return;
            }
            if (client.level.getBlockState(pos).is(block) && containerSize(pos) == size) {
                BlockPos key = canonical(pos);
                if (!key.equals(reference)) remaining.add(key.immutable());
            }
        }
    }

    private BlockPos canonical(BlockPos pos) {
        var state = client.level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) == ChestType.RIGHT)
            return pos.relative(ChestBlock.getConnectedDirection(state));
        return pos.immutable();
    }

    private int containerSize(BlockPos pos) {
        var state = client.level.getBlockState(pos);
        return state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE ? 54 : 27;
    }

    private boolean reachable(BlockPos pos) {
        return client.level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4) && PlayerUtils.isWithinBlockInteractionRange(client.player, pos, 0)
                && !client.player.isShiftKeyDown();
    }

    public void opened(int id, MenuType<?> type) {
        if (phase != Phase.OPENING || !sameWorld()) return;
        boolean accepted = block instanceof ShulkerBoxBlock ? type == MenuType.SHULKER_BOX
                : type == (size == 54 ? MenuType.GENERIC_9x6 : MenuType.GENERIC_9x3);
        if (!accepted) { pause("changed"); return; }
        candidate = id; phase = Phase.CONTENT;
        me.aleksilassila.litematica.printer.integration.quickshulker.QuickShulkerDiagnostics.log("container-sync opened menu=" + id);
    }

    public void content(int id, int revision, List<ItemStack> items, ItemStack carried) {
        if (!sameWorld() || client.player == null) return;
        if (phase == Phase.RESYNC) {
            if (!resyncRequested || resyncMenu != client.player.containerMenu || resyncMenu.containerId != id
                    || items.size() != resyncMenu.slots.size()
                    || !ContainerSyncTransfer.newerRevision(revision, resyncRevision)) return;
            if (!carried.isEmpty()) {
                // A plugin may retain a cursor after closing. Only the user can decide where it belongs.
                resyncNeedsAttention = true;
                return;
            }
            // Never close a replacement/manual menu, nor continue the old material plan.
            pause("changed");
            return;
        }
        if (phase == Phase.OPENING && id == 0 && ContainerSyncTransfer.newerRevision(revision, openingInventoryRevision)) {
            if (cancelling) { discard(); message("cancelled"); }
            else pause("open_timeout");
            return;
        }
        if (phase == Phase.TRANSFER && client.player.containerMenu == menu && menu.containerId == id) {
            if (ContainerSyncTransfer.newerRevision(revision, sentRevision)) transfer.observe(menu, items, carried);
            return;
        }
        if (phase != Phase.CONTENT || candidate != id || client.player.containerMenu.containerId != id) return;
        menu = client.player.containerMenu;
        if (items.size() != size + 36 || menu.slots.size() != items.size() || !carried.isEmpty()) {
            pause("changed"); return;
        }
        // Explicit slot ownership prevents modded menus from masquerading as ordinary storage.
        for (int i = 0; i < items.size(); i++) {
            boolean playerSlot = menu.getSlot(i).container == client.player.getInventory();
            if (playerSlot != (i >= size)) { pause("unsupported"); return; }
        }
        if (cancelling) { closeOwned(); discard(); message("cancelled"); return; }
        if (template == null) {
            template = ContainerSyncPlan.copy(items.subList(0, size));
            closeOwned(); phase = Phase.READY; return;
        }
        var rules = new ContainerSyncPlan.Rules() {
            public boolean movable(int slot) {
                if (!menu.getSlot(slot).mayPickup(client.player)) return false;
                if (slot < size) return true;
                int inventorySlot = menu.getSlot(slot).getContainerSlot();
                if (inventorySlot == me.aleksilassila.litematica.printer.utils.InventoryUtils.getSelectedSlot(client.player.getInventory())) return false;
                // Honour Litematica's protected hotbar; main inventory is the material pool.
                return inventorySlot >= 9 && inventorySlot < 36
                        || inventorySlot >= 0 && inventorySlot < 9
                        && InventoryUtilsAccessor.getPICK_BLOCKABLE_SLOTS().contains(inventorySlot)
                        && InventoryUtilsAccessor.canPickToSlot(client.player.getInventory(), inventorySlot);
            }
            public int capacity(int slot, ItemStack stack) {
                var targetSlot = menu.getSlot(slot);
                return targetSlot.mayPlace(stack) ? targetSlot.getMaxStackSize(stack) : 0;
            }
        };
        var plan = ContainerSyncPlan.create(template, items, rules);
        if (plan.problem() != null) {
            pause(plan.problem() == ContainerSyncPlan.Problem.MISSING_ITEMS ? "missing"
                    : plan.problem() == ContainerSyncPlan.Problem.NO_SPACE ? "space" : "unsupported");
            return;
        }
        final AbstractContainerMenu owned = menu;
        transfer = new ContainerSyncTransfer(new ContainerSyncTransfer.Port() {
            public Object context() { return sameWorld() && client.player != null && client.player.containerMenu == owned ? owned : null; }
            public boolean acquire() { return ContainerSyncController.this.acquire(); }
            // The batch owns the lease across menu changes; pause/discard releases it.
            public void release() { }
            public void click(int slot, int button) {
                sentRevision = owned.getStateId();
                QuickShulkerClicks.sendWithFullSync(owned, slot, button, ContainerInput.PICKUP);
            }
            public void refresh() { sentRevision = owned.getStateId(); QuickShulkerClicks.requestFullSync(owned); }
            public boolean unchanged(List<ItemStack> expected, ItemStack cursor) {
                return ContainerSyncPlan.matches(expected, owned.slots.stream().map(slot -> slot.getItem()).toList())
                        && ItemStack.matches(cursor, owned.getCarried());
            }
        }, plan);
        phase = Phase.TRANSFER;
        message("progress", remaining.size());
    }

    /** Manual clicks/close request cancellation; an in-flight cursor must be settled first. */
    public boolean guardManualInput() {
        if (closing || !sameWorld() || client.player == null) return false;
        if (phase == Phase.RESYNC) {
            cancel();
            if (!resyncNeedsAttention || resyncMenu != client.player.containerMenu || !resyncRequested) return true;
            // Let one manual recovery click through, then request its full confirmation on the next tick.
            resyncRequested = false; resyncNeedsAttention = false;
            return false;
        }
        if (phase == Phase.TRANSFER && client.player.containerMenu == menu && !transfer.finished()) {
            cancel(); return !transfer.needsAttention();
        }
        if (phase == Phase.CONTENT) {
            // Keep the opened menu alive until its first snapshot can finish cancellation.
            // Letting a click through here also queues a mutation ahead of our close.
            cancel(); return true;
        }
        if (phase == Phase.OPENING) cancel();
        return false;
    }

    public boolean guardClose() {
        if (!closing && sameWorld() && phase == Phase.RESYNC) { cancel(); return true; }
        if (!closing && sameWorld() && transfer != null && !transfer.finished()
                && client.player != null && client.player.containerMenu == menu) {
            cancel(); return true;
        }
        return guardManualInput();
    }

    private boolean acquire() { return runtime.actionBroker().tryAcquire(OWNER, RESOURCES, 0); }
    private boolean sameWorld() { return client.level == level && client.getConnection() == connection; }
    private static long now() { return System.nanoTime() / 1_000_000L; }

    private void cancel() {
        cancelling = true;
        if (transfer != null) transfer.cancel();
        if (phase == Phase.READY || phase == Phase.SCANNING || phase == Phase.PAUSED) { discard(); message("cancelled"); }
    }

    private void closeOwned() {
        if (sameWorld() && menu != null && client.player != null && client.player.containerMenu == menu && menu.getCarried().isEmpty()) {
            closing = true;
            try { client.player.closeContainer(); } finally { closing = false; }
        }
        menu = null; candidate = -1;
    }

    private void pause(String reason) {
        me.aleksilassila.litematica.printer.integration.quickshulker.QuickShulkerDiagnostics.log(
                "container-sync paused reason=" + reason + " target=" + target);
        closeOwned();
        resyncMenu = null; resyncRequested = false; resyncNeedsAttention = false;
        runtime.actionBroker().releaseOwner(OWNER);
        phase = Phase.PAUSED;
        message(reason);
    }

    private void discard() {
        transfer = null; menu = null; candidate = -1; template = null;
        resyncMenu = null; resyncRequested = false; resyncNeedsAttention = false;
        remaining.clear(); if (highlights != null) highlights.clear();
        phase = Phase.IDLE; cancelling = false; scanPaused = false;
        runtime.actionBroker().releaseOwner(OWNER);
    }

    @Override public void onEpochChanged(RuntimeEvent.EpochChanged event) {
        // Connection.disconnect may run on Netty. Menu state and highlight sets belong to the client thread.
        if (!client.isSameThread()) {
            Object previousConnection = connection;
            Object previousLevel = level;
            client.execute(() -> {
                if (connection == previousConnection && level == previousLevel) onEpochChanged(event);
            });
            return;
        }
        if (!sameWorld() || event.reason().equals("disconnect") || event.reason().equals("client_scope_changed")
                || event.reason().equals("local_player_init")) discard();
        else if (phase != Phase.IDLE) {
            cancel();
            // ActionBroker clears leases earlier in the epoch reset. Retain ownership for draining.
            if (phase == Phase.TRANSFER || phase == Phase.OPENING || phase == Phase.CONTENT || phase == Phase.RESYNC) acquire();
        }
    }

    private void message(String key, Object... args) {
        MessageUtils.setOverlayMessage(net.minecraft.client.resources.language.I18n.get("litematica-printer.message.container_sync." + key, args));
    }
}
