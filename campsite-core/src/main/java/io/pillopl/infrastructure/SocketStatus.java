package io.pillopl.infrastructure;

import java.util.EnumSet;
import java.util.Set;

/**
 * Status of a socket as reported by the pedestal (translated from the link messages) or corrected by monitoring.
 */
public enum SocketStatus {
    AVAILABLE,
    OCCUPIED,
    SUPPLYING,
    SUPPLYING_WITHOUT_CONSUMPTION,
    BREAKER_TRIPPED,
    FAULTED,
    UNAVAILABLE,
    FIRMWARE_UPDATE,
    UNKNOWN;

    private static final Set<SocketStatus> ERRORS = EnumSet.of(BREAKER_TRIPPED, FAULTED, UNKNOWN);

    /**
     * Statuses that count as an error of the pedestal (pedestal list "in error", monitoring mails).
     */
    public static Set<SocketStatus> errors() {
        return EnumSet.copyOf(ERRORS);
    }

    public boolean isError() {
        return ERRORS.contains(this);
    }
}
