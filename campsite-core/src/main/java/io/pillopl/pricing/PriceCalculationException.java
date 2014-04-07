package io.pillopl.pricing;

/**
 * A price could not be calculated (missing or negative values, wrong component). Ends as PRICING_FAILED, never as
 * a sentinel cost.
 */
public class PriceCalculationException extends Exception {

    public PriceCalculationException(String message) {
        super(message);
    }

    public PriceCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}
