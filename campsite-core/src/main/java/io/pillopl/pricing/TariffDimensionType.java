package io.pillopl.pricing;

/**
 * What a price component charges for, with the unit of its price and of its step size.
 */
public enum TariffDimensionType {

    /**
     * Energy taken from the socket meter. Price per kWh, step size in Wh.
     */
    ENERGY(PriceUnit.PER_KWH),

    /**
     * Time the unit is hooked up to the socket. Price per hour, step size in seconds.
     */
    TIME(PriceUnit.PER_HOUR),

    /**
     * Night of the stay with power (daily lump sum). Price per night, step size in nights.
     */
    PER_NIGHT(PriceUnit.PER_NIGHT),

    /**
     * Connection fee, once per session. Step size is always 1.
     */
    FLAT(PriceUnit.PER_SESSION);

    private final PriceUnit priceUnit;

    TariffDimensionType(PriceUnit priceUnit) {
        this.priceUnit = priceUnit;
    }

    public PriceUnit getPriceUnit() {
        return priceUnit;
    }

    /**
     * ENERGY and TIME are spread over the tariff windows a session crosses; FLAT and PER_NIGHT are priced once
     * with the element that applies at plug-in.
     */
    public boolean isSplitOverWindows() {
        return this == ENERGY || this == TIME;
    }
}
