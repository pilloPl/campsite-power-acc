package io.pillopl.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Price per kWh. The energy (its share inside the window of the component) is rounded up to the step size in Wh.
 */
public class PriceByEnergyCalculator extends PriceCalculator<PricingInput> {

    public static final BigDecimal WH_PER_KWH = BigDecimal.valueOf(1000);

    public PriceByEnergyCalculator(PricingInput pricingInput, PriceComponent energyPrice, BigDecimal share) {
        super(pricingInput, energyPrice, share);
    }

    @Override
    protected BigDecimal calculateCost() throws PriceCalculationException {
        BigDecimal consumptionInKwh = checkPositiveValue(pricingContext.getKwh());
        BigDecimal consumptionInWh = consumptionInKwh.multiply(share).multiply(WH_PER_KWH);

        BigDecimal steps = stepsRoundedUp(consumptionInWh, price.getStepSize());
        billedQuantity = steps.multiply(BigDecimal.valueOf(price.getStepSize()))
                .divide(WH_PER_KWH, INTERMEDIATE_SCALE, RoundingMode.HALF_UP).stripTrailingZeros();

        return price.getPrice().multiply(billedQuantity);
    }

    @Override
    protected TariffDimensionType getDimensionType() {
        return TariffDimensionType.ENERGY;
    }

    @Override
    public void validateContext() throws PriceCalculationException {
        super.validateContext();
        if (pricingContext.getKwh() == null) {
            throw new PriceCalculationException("consumption is not set");
        }
    }
}
