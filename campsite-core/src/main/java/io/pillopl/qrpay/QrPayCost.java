package io.pillopl.qrpay;

import java.math.BigDecimal;

/**
 * Cost of a QR payment supply: the estimate while the socket supplies, the price of the consumption record when it
 * stopped. A cost that could not be calculated has no amount and a reason.
 *
 * @param finished true for the price of the consumption record (the supply stopped)
 */
public record QrPayCost(BigDecimal kwh, Long durationSeconds, BigDecimal cost, String currency, boolean finished,
                        String failureReason) {

    public static QrPayCost nothingYet() {
        return new QrPayCost(BigDecimal.ZERO, 0L, null, null, false, null);
    }

    public static QrPayCost failed(BigDecimal kwh, Long durationSeconds, boolean finished, String reason) {
        return new QrPayCost(kwh, durationSeconds, null, null, finished, reason);
    }

    public boolean isPriced() {
        return cost != null;
    }
}
