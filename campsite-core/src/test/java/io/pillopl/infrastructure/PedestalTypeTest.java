package io.pillopl.infrastructure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PedestalTypeTest {

    @Test
    void controllerWithoutLocalCardListHasNoCapacity() {
        PedestalType type = new PedestalType("ACME", "P4", PedestalLinkVersion.V1);
        type.setLocalCardListCapacity(250);

        assertThat(type.getEffectiveLocalCardListCapacity(100)).isZero();
    }

    @Test
    void localCardListCapacityComesFromThePedestalType() {
        PedestalType type = new PedestalType("ACME", "P4", PedestalLinkVersion.V2);
        type.setLocalCardList(true);
        type.setLocalCardListCapacity(250);

        assertThat(type.getEffectiveLocalCardListCapacity(100)).isEqualTo(250);
    }

    @Test
    void typeWithLocalCardListButNoCapacityFallsBackToTheConfiguredDefault() {
        PedestalType type = new PedestalType("ACME", "P4", PedestalLinkVersion.V2);
        type.setLocalCardList(true);

        assertThat(type.getEffectiveLocalCardListCapacity(100)).isEqualTo(100);
    }
}
