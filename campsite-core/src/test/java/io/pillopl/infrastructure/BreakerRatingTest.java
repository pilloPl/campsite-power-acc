package io.pillopl.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BreakerRatingTest {

    @Test
    void ratingIsFoundByItsAmperes() {
        assertThat(BreakerRating.ofAmperes(16)).isEqualTo(BreakerRating.A16);
        assertThat(BreakerRating.ofAmperes(32)).isEqualTo(BreakerRating.A32);
    }

    @Test
    void amperesWithoutARatingGiveNoRating() {
        assertThat(BreakerRating.ofAmperes(25)).isNull();
    }
}
