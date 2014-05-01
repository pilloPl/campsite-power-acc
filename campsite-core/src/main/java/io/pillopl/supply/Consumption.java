package io.pillopl.supply;

import java.math.BigDecimal;

/**
 * Meter values of a supply as used for the consumption record: start and stop reading (kWh, possibly replaced by the
 * socket's last reading) and the consumption, which is null when one of the pedestal's readings was unusable.
 */
public record Consumption(BigDecimal startMeterKwh, BigDecimal stopMeterKwh, BigDecimal consumptionKwh) {

    public boolean isKnown() {
        return consumptionKwh != null;
    }

    /**
     * A negative consumption means the record must not be billed as it is (meter replaced, wrong reading).
     */
    public boolean isNegative() {
        return consumptionKwh != null && consumptionKwh.signum() < 0;
    }
}
