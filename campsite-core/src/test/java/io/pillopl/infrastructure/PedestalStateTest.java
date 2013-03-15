package io.pillopl.infrastructure;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PedestalStateTest {

    private final LocalDateTime noon = LocalDateTime.of(2026, 7, 1, 12, 0);

    @Test
    void pedestalTimesOutWhenNothingArrivedForLongerThanTheTimeout() {
        PedestalState state = new PedestalState(null, noon);

        assertThat(state.checkTimeout(noon.plusMinutes(15), 15)).isFalse();
        assertThat(state.checkTimeout(noon.plusMinutes(16), 15)).isTrue();
        assertThat(state.isTimeout()).isTrue();
        assertThat(state.getLastTimeout()).isEqualTo(noon.plusMinutes(16));
    }

    @Test
    void timeoutIsReportedOnlyOnce() {
        PedestalState state = new PedestalState(null, noon);
        state.checkTimeout(noon.plusHours(1), 15);

        assertThat(state.checkTimeout(noon.plusHours(2), 15)).isFalse();
    }

    @Test
    void incomingMessageClearsTheTimeout() {
        PedestalState state = new PedestalState(null, noon);
        state.checkTimeout(noon.plusHours(1), 15);

        assertThat(state.incoming(noon.plusHours(2))).isTrue();
        assertThat(state.isTimeout()).isFalse();
        assertThat(state.incoming(noon.plusHours(3))).isFalse();
    }
}
