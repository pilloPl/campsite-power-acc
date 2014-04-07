package io.pillopl.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Price per hour hooked up. The duration (its share inside the window of the component) is rounded up to the step
 * size in seconds, e.g. started quarters of an hour.
 */
public class PriceByTimeCalculator extends PriceCalculator<PricingInput> {

    public static final BigDecimal HOUR_IN_SECONDS = BigDecimal.valueOf(3600);

    public PriceByTimeCalculator(PricingInput pricingInput, PriceComponent timePrice, Long windowSeconds) {
        super(pricingInput, timePrice, windowSeconds);
    }

    @Override
    protected BigDecimal calculateCost() throws PriceCalculationException {
        BigDecimal durationInSeconds = portionOf(
                checkPositiveValue(BigDecimal.valueOf(pricingContext.getDurationSeconds())));

        BigDecimal steps = stepsRoundedUp(durationInSeconds, price.getStepSize());
        billedQuantity = steps.multiply(BigDecimal.valueOf(price.getStepSize()))
                .divide(HOUR_IN_SECONDS, INTERMEDIATE_SCALE, RoundingMode.HALF_UP).stripTrailingZeros();

        return price.getPrice().multiply(billedQuantity);
    }

    @Override
    protected TariffDimensionType getDimensionType() {
        return TariffDimensionType.TIME;
    }

    @Override
    public void validateContext() throws PriceCalculationException {
        super.validateContext();
        if (pricingContext.getDurationSeconds() == null) {
            throw new PriceCalculationException("duration is not set");
        }
    }
}
