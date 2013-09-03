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
     * Seconds of the supply inside the tariff window of this component; null when the component prices the whole
     * supply.
     */
    protected final Long windowSeconds;

    protected BigDecimal billedQuantity;

    protected PriceCalculator(T pricingContext, PriceComponent price, Long windowSeconds) {
        this.pricingContext = pricingContext;
        this.price = price;
        this.windowSeconds = windowSeconds;
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

    /**
     * The part of a quantity of the whole supply that falls into the window of this component, proportionally to
     * time: quantity * window seconds / duration. The whole quantity without a window or for a supply without
     * duration.
     */
    protected BigDecimal portionOf(BigDecimal quantity) {
        Long totalSeconds = pricingContext.getDurationSeconds();
        if (windowSeconds == null || totalSeconds == null || totalSeconds == 0
                || windowSeconds.longValue() == totalSeconds.longValue()) {
            return quantity;
        }
        return quantity.multiply(BigDecimal.valueOf(windowSeconds))
                .divide(BigDecimal.valueOf(totalSeconds), INTERMEDIATE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * @return the share of the supply priced by this component (1 for the whole supply)
     */
    public BigDecimal getShare() {
        return portionOf(BigDecimal.ONE);
    }

    public Long getWindowSeconds() {
        return windowSeconds;
    }

    /**
     * @return the quantity charged after rounding up to the step size (kWh, hours, nights, sessions); null before
     * {@link #calculate()}
     */
    public BigDecimal getBilledQuantity() {
        return billedQuantity;
    }
}
