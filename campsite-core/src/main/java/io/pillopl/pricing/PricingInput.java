package io.pillopl.pricing;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * What is priced: a supply on a socket (with the card, when known) from plug-in to plug-out with the energy taken
 * and the nights of the stay with power. Built by the consumption record, the QR payment, the start answer and the
 * price simulation.
 */
public class PricingInput {

    private final Long socketId;

    private final Long cardId;

    private final LocalDateTime plugIn;

    private final LocalDateTime plugOut;

    private final BigDecimal kwh;

    private final Integer nights;

    private final boolean billable;

    private PricingInput(Long socketId, Long cardId, LocalDateTime plugIn, LocalDateTime plugOut, BigDecimal kwh,
                         Integer nights, boolean billable) {
        this.socketId = socketId;
        this.cardId = cardId;
        this.plugIn = plugIn;
        this.plugOut = plugOut;
        this.kwh = kwh;
        this.nights = nights;
        this.billable = billable;
    }

    /**
     * @param cardId null for supplies without a card (QR payment, free socket)
     * @param nights null to count them from plug-in and plug-out
     */
    public static PricingInput of(Long socketId, Long cardId, LocalDateTime plugIn, LocalDateTime plugOut,
                                  BigDecimal kwh, Integer nights) {
        return new PricingInput(socketId, cardId, plugIn, plugOut, kwh, nights, true);
    }

    /**
     * A supply that is not billed (house card, free socket, pedestal not billable): priced as NOT_BILLABLE.
     */
    public static PricingInput notBillable(Long socketId, Long cardId, LocalDateTime plugIn, LocalDateTime plugOut,
                                           BigDecimal kwh) {
        return new PricingInput(socketId, cardId, plugIn, plugOut, kwh, null, false);
    }

    /**
     * Nights of power use: the midnights between plug-in and plug-out, at least one for a supply that lasted at
     * all (a day visit counts as one).
     */
    public static int nightsBetween(LocalDateTime plugIn, LocalDateTime plugOut) {
        if (plugIn == null || plugOut == null || !plugOut.isAfter(plugIn)) {
            return 0;
        }
        long midnights = ChronoUnit.DAYS.between(plugIn.toLocalDate(), plugOut.toLocalDate());
        return (int) Math.max(1, midnights);
    }

    public Long getSocketId() {
        return socketId;
    }

    public Long getCardId() {
        return cardId;
    }

    public LocalDateTime getPlugIn() {
        return plugIn;
    }

    public LocalDateTime getPlugOut() {
        return plugOut;
    }

    public BigDecimal getKwh() {
        return kwh;
    }

    public boolean isBillable() {
        return billable;
    }

    /**
     * @return the duration in seconds, negative when plug-out lies before plug-in, null when a date is missing
     */
    public Long getDurationSeconds() {
        if (plugIn == null || plugOut == null) {
            return null;
        }
        return Duration.between(plugIn, plugOut).getSeconds();
    }

    public Integer getNights() {
        return nights != null ? nights : Integer.valueOf(nightsBetween(plugIn, plugOut));
    }

    @Override
    public String toString() {
        return "PricingInput{socketId=" + socketId + ", cardId=" + cardId + ", plugIn=" + plugIn + ", plugOut="
                + plugOut + ", kwh=" + kwh + ", nights=" + nights + ", billable=" + billable + "}";
    }
}
