package io.pillopl.supply;

/**
 * Why a supply session ended: reported by the pedestal in its stop message, or set by the pedestal backend when it
 * closes a session the pedestal never stopped (check-out of the stay, timeout).
 */
public enum StopReason {
    LOCAL,
    REMOTE,
    UNPLUGGED,
    BREAKER_TRIP,
    POWER_LOSS,
    REBOOT,
    EMERGENCY_STOP,
    CHECKOUT,
    TIMEOUT,
    OTHER;

    /**
     * Reason sent by a pedestal; unknown or missing reasons are OTHER, never an error (the stop must be accepted).
     */
    public static StopReason fromPedestal(String reason) {
        if (reason == null || reason.isBlank()) {
            return LOCAL;
        }
        try {
            return StopReason.valueOf(reason.trim().toUpperCase().replace(' ', '_').replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }

    /**
     * Closed by the pedestal backend, not by the pedestal.
     */
    public boolean isClosedBySystem() {
        return this == CHECKOUT || this == TIMEOUT;
    }
}
