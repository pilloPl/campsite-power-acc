package io.pillopl.infrastructure;

/**
 * Rating of the breaker protecting a socket. The technical data per rating is kept in table BREAKER_RATING
 * ({@link BreakerRatingSpecification}).
 */
public enum BreakerRating {

    A6(6),
    A10(10),
    A16(16),
    A32(32);

    private final int amperes;

    BreakerRating(int amperes) {
        this.amperes = amperes;
    }

    public int getAmperes() {
        return amperes;
    }

    /**
     * @return the rating with exactly these amperes or null
     */
    public static BreakerRating ofAmperes(int amperes) {
        for (BreakerRating rating : values()) {
            if (rating.amperes == amperes) {
                return rating;
            }
        }
        return null;
    }
}
