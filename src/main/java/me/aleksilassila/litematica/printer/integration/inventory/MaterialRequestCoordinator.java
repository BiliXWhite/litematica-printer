package me.aleksilassila.litematica.printer.integration.inventory;

import me.aleksilassila.litematica.printer.core.runtime.RuntimeComponent;
import me.aleksilassila.litematica.printer.core.runtime.RuntimeEvent;
import net.minecraft.world.item.Item;

import java.util.Arrays;
import java.util.List;

/**
 * Serializes external material acquisition through one tokenized provider chain.
 *
 * <p>Repeated requests for the same item poll the active token. Requests for another item wait
 * behind it, preventing pick-block, printing and third-party integrations from retrieving the
 * same stack twice.</p>
 */
public final class MaterialRequestCoordinator implements RuntimeComponent {
    private final List<InventoryProvider> providers;
    private long nextToken = 1L;
    private long tick;
    private ActiveRequest active;

    public MaterialRequestCoordinator(List<InventoryProvider> providers) {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalArgumentException("providers must not be empty");
        }
        this.providers = List.copyOf(providers);
    }

    public MaterialReservation request(Item item, MaterialRequest.Source source) {
        if (item == null) {
            throw new IllegalArgumentException("item must not be null");
        }
        return this.request(List.of(item), item, source);
    }

    public MaterialReservation request(Item[] acceptedItems, MaterialRequest.Source source) {
        if (acceptedItems == null || acceptedItems.length == 0) {
            throw new IllegalArgumentException("acceptedItems must not be empty");
        }
        return this.request(Arrays.asList(acceptedItems), acceptedItems[0], source);
    }

    public MaterialReservation request(
            List<Item> acceptedItems,
            Item preferredItem,
            MaterialRequest.Source source
    ) {
        return request(acceptedItems, preferredItem, source, false);
    }

    public MaterialReservation requestEmptyShulker(Item item) {
        return request(List.of(item), item, MaterialRequest.Source.PRINT, true);
    }

    private MaterialReservation request(List<Item> acceptedItems, Item preferredItem,
                                        MaterialRequest.Source source, boolean emptyShulkerOnly) {
        MaterialRequest requested = new MaterialRequest(
                this.nextToken,
                acceptedItems,
                preferredItem,
                1,
                source,
                emptyShulkerOnly
        );
        if (this.active != null && !sameRequirement(this.active.request, requested)) {
            MaterialReservation current = this.advance();
            if (current.state() == MaterialReservation.State.PENDING) {
                return current;
            }
        }
        if (this.active == null) {
            MaterialRequest request = new MaterialRequest(
                    this.nextToken++,
                    acceptedItems,
                    preferredItem,
                    1,
                    source,
                    emptyShulkerOnly
            );
            this.active = new ActiveRequest(request, 0, false, this.tick);
        }
        return this.advance();
    }

    public void tick() {
        this.tick++;
        if (this.active != null) {
            this.advance();
        }
    }

    public boolean isBusy() {
        return this.active != null;
    }

    public long activeToken() {
        return this.active == null ? 0L : this.active.request.token();
    }

    public Item activeItem() {
        return this.active == null ? null : this.active.request.preferredItem();
    }

    public List<Item> activeItems() {
        return this.active == null ? List.of() : this.active.request.acceptedItems();
    }

    public boolean blocksPrinterWhilePending() {
        if (this.active == null || this.active.providerIndex >= this.providers.size()) {
            return false;
        }
        return this.providers.get(this.active.providerIndex).blocksPrinterWhilePending();
    }

    @Override
    public void onEpochChanged(RuntimeEvent.EpochChanged event) {
        this.reset();
    }

    public void reset() {
        for (InventoryProvider provider : this.providers) {
            provider.reset();
        }
        this.active = null;
        this.tick = 0L;
    }

    private MaterialReservation advance() {
        while (this.active != null && this.active.providerIndex < this.providers.size()) {
            InventoryProvider provider = this.providers.get(this.active.providerIndex);
            MaterialReservation result;
            if (this.active.started) {
                result = provider.status(this.active.request);
            } else {
                result = provider.request(this.active.request);
                this.active = this.active.startedAt(this.tick);
            }

            if (result.state() == MaterialReservation.State.AVAILABLE) {
                this.active = null;
                return result;
            }
            if (result.state() == MaterialReservation.State.PENDING && provider.isWaitingToStart()) {
                this.active = this.active.waitingToStart();
                return result;
            }
            if (this.active.providerStartedTick < 0L) {
                this.active = this.active.startedAt(this.tick);
            }
            long timeoutTicks = Math.max(1L, provider.pendingTimeoutTicks());
            if (result.state() == MaterialReservation.State.PENDING
                    && this.tick - this.active.providerStartedTick <= timeoutTicks) {
                return result;
            }
            if (result.state() == MaterialReservation.State.PENDING) {
                // A timed-out provider may still own inventory/main-hand leases. Release its
                // operation before advancing, otherwise an unavailable material can permanently
                // block printing even though the coordinator itself is no longer busy.
                provider.reset();
            }
            this.active = this.active.nextProvider(this.tick);
        }

        long token = this.active == null ? 0L : this.active.request.token();
        this.active = null;
        return new MaterialReservation(token, MaterialReservation.State.UNAVAILABLE);
    }

    private static boolean sameRequirement(MaterialRequest left, MaterialRequest right) {
        return left.source() == right.source()
                && left.emptyShulkerOnly() == right.emptyShulkerOnly()
                && left.minimumCount() == right.minimumCount()
                && left.preferredItem() == right.preferredItem()
                && left.acceptedItems().equals(right.acceptedItems());
    }

    private record ActiveRequest(
            MaterialRequest request,
            int providerIndex,
            boolean started,
            long providerStartedTick
    ) {
        ActiveRequest startedAt(long tick) {
            return new ActiveRequest(this.request, this.providerIndex, true, tick);
        }

        ActiveRequest waitingToStart() {
            return new ActiveRequest(this.request, this.providerIndex, true, -1L);
        }

        ActiveRequest nextProvider(long tick) {
            return new ActiveRequest(this.request, this.providerIndex + 1, false, tick);
        }
    }
}
