package io.pillopl.pricing;

/**
 * Outcome of pricing a supply. A failed pricing has no cost (instead of a sentinel value) and waits for review.
 */
public enum PricingStatus {
    PRICED,
    PRICING_FAILED,
    NOT_BILLABLE
}
