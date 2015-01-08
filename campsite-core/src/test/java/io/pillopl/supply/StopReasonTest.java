package io.pillopl.supply;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StopReasonTest {

    @Test
    void missingReasonOfThePedestalIsALocalStop() {
        assertThat(StopReason.fromPedestal(null)).isEqualTo(StopReason.LOCAL);
        assertThat(StopReason.fromPedestal("  ")).isEqualTo(StopReason.LOCAL);
    }

    @Test
    void reasonIsReadRegardlessOfCaseSpacesAndDashes() {
        assertThat(StopReason.fromPedestal("unplugged")).isEqualTo(StopReason.UNPLUGGED);
        assertThat(StopReason.fromPedestal(" Breaker trip ")).isEqualTo(StopReason.BREAKER_TRIP);
        assertThat(StopReason.fromPedestal("emergency-stop")).isEqualTo(StopReason.EMERGENCY_STOP);
    }

    @Test
    void unknownReasonIsOtherAndNeverAnError() {
        assertThat(StopReason.fromPedestal("FUSE_BLOWN")).isEqualTo(StopReason.OTHER);
    }

    @Test
    void onlyCheckoutAndTimeoutAreClosedBySystem() {
        assertThat(StopReason.CHECKOUT.isClosedBySystem()).isTrue();
        assertThat(StopReason.TIMEOUT.isClosedBySystem()).isTrue();
        assertThat(StopReason.REMOTE.isClosedBySystem()).isFalse();
        assertThat(StopReason.LOCAL.isClosedBySystem()).isFalse();
    }
}
