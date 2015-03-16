package io.pillopl.consumption;

/**
 * Validation of the meter values and times of a consumption record. SUSPECT records wait for a review that confirms
 * them (they are priced and exported then) or rejects them (they are never exported).
 */
public enum RecordValidationState {
    VALID,
    SUSPECT,
    CONFIRMED,
    REJECTED;

    public boolean isWaitingForReview() {
        return this == SUSPECT;
    }
}
