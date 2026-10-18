package me.aleksilassila.litematica.printer.integration.inventory;

/** Capability boundary implemented by inventory integrations. */
public interface InventoryProvider {
    String id();

    MaterialReservation request(MaterialRequest request);

    /** Polls a request previously accepted by this provider without issuing it again. */
    default MaterialReservation status(MaterialRequest request) {
        return new MaterialReservation(request.token(), MaterialReservation.State.UNAVAILABLE);
    }

    /** Maximum number of client ticks a pending request may stay with this provider. */
    default long pendingTimeoutTicks() {
        return 80L;
    }

    /** External work must finish before this provider can send the current request. */
    default boolean isWaitingToStart() {
        return false;
    }

    /** Whether a pending request must pause the whole printer while it owns resources. */
    default boolean blocksPrinterWhilePending() {
        return true;
    }

    default void tick() {
    }

    default void reset() {
    }
}
