package io.pillopl.pricing;

import java.math.BigDecimal;

/**
 * Connection fee, once per session.
 */
public class PriceByFlatRateCalculator extends PriceCalculator<PricingInput> {

    public PriceByFlatRateCalculator(PricingInput pricingInput, PriceComponent flatPrice) {
        super(pricingInput, flatPrice, BigDecimal.ONE);
    }

    @Override
    protected BigDecimal calculateCost() {
        billedQuantity = BigDecimal.ONE;
        return price.getPrice();
    }

    @Override
    protected TariffDimensionType getDimensionType() {
        return TariffDimensionType.FLAT;
    }
}
