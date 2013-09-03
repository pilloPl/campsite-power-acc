package io.pillopl.pricing;

import java.math.BigDecimal;

/**
 * Price per night of the stay with power (daily lump sum). The nights are rounded up to the step size in nights.
 */
public class PriceByNightCalculator extends PriceCalculator<PricingInput> {

    public PriceByNightCalculator(PricingInput pricingInput, PriceComponent nightPrice) {
        super(pricingInput, nightPrice, null);
    }

    @Override
    protected BigDecimal calculateCost() throws PriceCalculationException {
        BigDecimal nights = checkPositiveValue(BigDecimal.valueOf(pricingContext.getNights()));

        BigDecimal steps = stepsRoundedUp(nights, price.getStepSize());
        billedQuantity = steps.multiply(BigDecimal.valueOf(price.getStepSize()));

        return price.getPrice().multiply(billedQuantity);
    }

    @Override
    protected TariffDimensionType getDimensionType() {
        return TariffDimensionType.PER_NIGHT;
    }
}
