package io.pillopl.guest;

/**
 * A seasonal contract runs (ACTIVE), the resident asked to leave at a date (TERMINATION_REQUESTED, still running
 * until then) or it is over (ENDED).
 */
public enum ContractStatus {
    ACTIVE,
    ENDED,
    TERMINATION_REQUESTED
}
