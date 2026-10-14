package com.stayease.entity.enums;

/**
 * RESERVED -> GUESTS_ADDED -> CONFIRMED (paid).
 * RESERVED / GUESTS_ADDED become EXPIRED when the hold runs out, and any
 * non-final booking can be CANCELLED by its owner before check-in.
 */
public enum BookingStatus {
    RESERVED,
    GUESTS_ADDED,
    CONFIRMED,
    CANCELLED,
    EXPIRED;

    /** Rooms are held (counted in Inventory.reservedCount) but not yet paid for. */
    public boolean isHold() {
        return this == RESERVED || this == GUESTS_ADDED;
    }
}
