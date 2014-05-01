package io.pillopl.supply;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * What the pedestal link passes to the supply control unit for a start or a stop: the pedestal's meter values (kWh)
 * and times, and for a stop the transaction id and the reason. The values are the pedestal's and may be wrong.
 */
public final class SupplyRequest {

    private final Integer transactionId;

    private final BigDecimal startMeterKwh;

    private final LocalDateTime startTimestamp;

    private final BigDecimal stopMeterKwh;

    private final LocalDateTime stopTimestamp;

    private final StopReason stopReason;

    private SupplyRequest(Integer transactionId, BigDecimal startMeterKwh, LocalDateTime startTimestamp,
                          BigDecimal stopMeterKwh, LocalDateTime stopTimestamp, StopReason stopReason) {
        this.transactionId = transactionId;
        this.startMeterKwh = startMeterKwh;
        this.startTimestamp = startTimestamp;
        this.stopMeterKwh = stopMeterKwh;
        this.stopTimestamp = stopTimestamp;
        this.stopReason = stopReason;
    }

    public static SupplyRequest start(BigDecimal startMeterKwh, LocalDateTime startTimestamp) {
        return new SupplyRequest(null, startMeterKwh, startTimestamp, null, null, null);
    }

    public static SupplyRequest stop(Integer transactionId, BigDecimal stopMeterKwh, LocalDateTime stopTimestamp,
                                     StopReason stopReason) {
        return new SupplyRequest(transactionId, null, null, stopMeterKwh, stopTimestamp, stopReason);
    }

    /**
     * Stop of a session the pedestal never stopped: the session's start and the socket's last reading.
     */
    public static SupplyRequest close(SupplySession session, BigDecimal stopMeterKwh, LocalDateTime stopTimestamp,
                                      StopReason stopReason) {
        return new SupplyRequest(session.getTransactionId(), session.getStartMeterKwh(), session.getRemoteDate(),
                stopMeterKwh, stopTimestamp, stopReason);
    }

    /**
     * The stop with the start values of its session (the stop message carries no start values).
     */
    public SupplyRequest withStartOf(SupplySession session) {
        return new SupplyRequest(transactionId, session.getStartMeterKwh(), session.getRemoteDate(), stopMeterKwh,
                stopTimestamp, stopReason);
    }

    public Integer getTransactionId() {
        return transactionId;
    }

    public BigDecimal getStartMeterKwh() {
        return startMeterKwh;
    }

    public LocalDateTime getStartTimestamp() {
        return startTimestamp;
    }

    public BigDecimal getStopMeterKwh() {
        return stopMeterKwh;
    }

    public LocalDateTime getStopTimestamp() {
        return stopTimestamp;
    }

    public StopReason getStopReason() {
        return stopReason;
    }

    @Override
    public String toString() {
        return "SupplyRequest{tx=" + transactionId + ", start=" + startMeterKwh + "@" + startTimestamp + ", stop="
                + stopMeterKwh + "@" + stopTimestamp + ", reason=" + stopReason + "}";
    }
}
