package io.pillopl.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * One priced component of a supply: what was charged, how much of it and for what amount. {@code amount} is
 * unrounded; the total of the calculation is rounded once.
 *
 * @param elementPosition position of the tariff element the component belongs to
 * @param share           part of the supply inside the window of the component (1 for the whole supply)
 */
public record PriceCalculationLine(int elementPosition, TariffDimensionType dimensionType, BigDecimal price,
                                   int stepSize, BigDecimal share, BigDecimal billedQuantity, BigDecimal amount) {

    public PriceUnit priceUnit() {
        return dimensionType.getPriceUnit();
    }

    /**
     * @return the amount at scale 2 HALF_UP, for display only
     */
    public BigDecimal displayAmount() {
        return amount.setScale(PricingService.MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
