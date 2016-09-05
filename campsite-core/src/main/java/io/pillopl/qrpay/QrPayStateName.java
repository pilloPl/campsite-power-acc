package io.pillopl.qrpay;

import java.util.List;

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
     * The pedestal may (still) report the start of the supply for the process: the remote supply start is on its way or the
     * pedestal repeats a start it got no answer for.
     */
    public boolean isSupplyMayStart() {
        return supplyMayStart;
    }

    /**
     * The states the state classes may move a process to from this one (ERROR is reachable from every state but
     * CLOSED; a supply running in spite of an error is still stopped and priced).
     */
    public List<QrPayStateName> getNextStates() {
        return switch (this) {
            case START_REQUESTED -> List.of(SOCKET_START_WAITING, ERROR);
            case SOCKET_START_WAITING -> List.of(SOCKET_START_RECEIVED, STOP_REQUESTED, SOCKET_STOP_RECEIVED, ERROR);
            case SOCKET_START_RECEIVED -> List.of(START_CONFIRMED, STOP_REQUESTED, SOCKET_STOP_RECEIVED, ERROR);
            case START_CONFIRMED -> List.of(STOP_REQUESTED, SOCKET_STOP_RECEIVED, ERROR);
            case STOP_REQUESTED -> List.of(SOCKET_STOP_WAITING, ERROR);
            case SOCKET_STOP_WAITING -> List.of(SOCKET_STOP_RECEIVED, ERROR);
            case SOCKET_STOP_RECEIVED -> List.of(CALCULATION_REQUESTED, ERROR);
            case CALCULATION_REQUESTED -> List.of(CALCULATION_DONE, ERROR);
            case CALCULATION_DONE -> List.of(CLOSED, ERROR);
            case CLOSED -> List.of();
            case ERROR -> List.of(STOP_REQUESTED, SOCKET_STOP_RECEIVED);
        };
    }

    public boolean isFinished() {
        return this == CLOSED || this == ERROR;
    }
}
