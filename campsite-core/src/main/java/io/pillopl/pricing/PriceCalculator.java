package io.pillopl.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Prices one component of a tariff for (a share of) a supply. The amount is returned unrounded: the sum of all
 * calculators is rounded once, to scale 2 HALF_UP, by {@link PricingService}.
 *
 * @param <T> what is priced
 */
public abstract class PriceCalculator<T extends PricingInput> {

    /**
     * Scale of intermediate divisions (shares, Wh to kWh, seconds to hours).
     */
    static final int INTERMEDIATE_SCALE = 10;

    protected final T pricingContext;

    protected final PriceComponent price;

    /**
     * Part of the supply priced by this component (time inside its tariff window / whole duration), 1 for all.
     */
    protected final BigDecimal share;

    protected BigDecimal billedQuantity;

    protected PriceCalculator(T pricingContext, PriceComponent price, BigDecimal share) {
        this.pricingContext = pricingContext;
        this.price = price;
        this.share = share != null ? share : BigDecimal.ONE;
    }

    public BigDecimal calculate() throws PriceCalculationException {
        validateContext();
        return calculateCost();
    }

    protected abstract BigDecimal calculateCost() throws PriceCalculationException;

    protected abstract TariffDimensionType getDimensionType();

    public void validateContext() throws PriceCalculationException {
        if (price == null) {
            throw new PriceCalculationException("Price is missing");
        }
        if (pricingContext == null) {
            throw new PriceCalculationException("Pricing context is missing");
        }
        if (price.getDimensionType() != getDimensionType()) {
            throw new PriceCalculationException("price is of the wrong type: " + price.getDimensionType());
        }
        if (price.getPrice() == null) {
            throw new PriceCalculationException("price is not set");
        }
        checkPositiveValue(price.getPrice());
        if (price.getStepSize() == null || price.getStepSize() < 1) {
            throw new PriceCalculationException("stepSize is not set");
        }
    }

    protected BigDecimal checkPositiveValue(BigDecimal value) throws PriceCalculationException {
        if (null == value) {
            return BigDecimal.ZERO;
        } else if (value.signum() == -1) {
            throw new PriceCalculationException("Values for price calculation must not be negative! Value was: "
                    + value);
        }
        return value;
    }

    /**
     * Rounds a quantity up to a whole number of steps.
     *
     * @return the number of steps
     */
    protected static BigDecimal stepsRoundedUp(BigDecimal quantity, int stepSize) {
        return quantity.divide(BigDecimal.valueOf(stepSize), 0, RoundingMode.CEILING);
    }

    public PriceComponent getPrice() {
        return price;
    }

    public BigDecimal getShare() {
        return share;
    }

    /**
     * @return the quantity charged after rounding up to the step size (kWh, hours, nights, sessions); null before
     * {@link #calculate()}
     */
    public BigDecimal getBilledQuantity() {
        return billedQuantity;
    }
}
