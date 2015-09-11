package io.pillopl.qrpay;

/**
 * States of a QR payment process, in the order of the happy flow. A stop request of the app in a state where the
 * supply is already stopping or stopped is silently ignored (the app repeats its stop).
 */
public enum QrPayStateName {
    START_REQUESTED(false, true),
    SOCKET_START_WAITING(false, true),
    SOCKET_START_RECEIVED(false, true),
    START_CONFIRMED(false, true),
    STOP_REQUESTED(true, false),
    SOCKET_STOP_WAITING(true, false),
    SOCKET_STOP_RECEIVED(true, false),
    CALCULATION_REQUESTED(true, false),
    CALCULATION_DONE(true, false),
    CLOSED(true, false),
    ERROR(false, false);

    private final boolean ignoreStopRequest;

    private final boolean supplyMayStart;

    QrPayStateName(boolean ignoreStopRequest, boolean supplyMayStart) {
        this.ignoreStopRequest = ignoreStopRequest;
        this.supplyMayStart = supplyMayStart;
    }

    /**
     * @return the state, null for null
     * @throws IllegalArgumentException for an unknown name
     */
    public static QrPayStateName fromString(String value) {
        if (value != null) {
            return valueOf(value);
        }
        return null;
    }

    public boolean isIgnoreStopRequest() {
        return ignoreStopRequest;
    }

    /**
     * The pedestal may (still) report the start of the supply for the process: the remote start is on its way or the
     * pedestal repeats a start it got no answer for.
     */
    public boolean isSupplyMayStart() {
        return supplyMayStart;
    }

    public boolean isFinished() {
        return this == CLOSED || this == ERROR;
    }
}
