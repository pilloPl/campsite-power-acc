package io.pillopl.card;

/**
 * Outcome of one distribution run: pedestals found, lists sent, lists failed, and whether the run finished within
 * its maximum duration.
 */
public record LocalCardListDistributionResult(int pedestals, int sent, int failed, boolean completed) {
}
