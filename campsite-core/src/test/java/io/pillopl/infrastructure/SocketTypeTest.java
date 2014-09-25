package io.pillopl.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SocketTypeTest {

    @Test
    void typeWithTheSamePlugPhasesAndBreakerIsReused() {
        SocketType type = new SocketType("CEE 16A", PlugStandard.CEE_BLUE_16A, 1, breaker(BreakerRating.A16));

        assertThat(type.sameAs(PlugStandard.CEE_BLUE_16A, 1, BreakerRating.A16)).isTrue();
        assertThat(type.sameAs(PlugStandard.CEE_RED_32A, 3, BreakerRating.A16)).isFalse();
        assertThat(type.sameAs(PlugStandard.CEE_BLUE_16A, 1, BreakerRating.A10)).isFalse();
    }

    @Test
    void typeWithoutBreakerHasNoRatingAndIsNeverTheSame() {
        SocketType type = new SocketType("CEE 16A", PlugStandard.CEE_BLUE_16A, 1, null);

        assertThat(type.getRating()).isNull();
        assertThat(type.sameAs(PlugStandard.CEE_BLUE_16A, 1, null)).isFalse();
    }

    private static BreakerRatingSpecification breaker(BreakerRating rating) {
        BreakerRatingSpecification specification = new BreakerRatingSpecification();
        specification.setRating(rating);
        return specification;
    }
}
