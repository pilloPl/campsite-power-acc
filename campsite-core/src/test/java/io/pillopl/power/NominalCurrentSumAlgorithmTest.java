package io.pillopl.power;

import io.pillopl.infrastructure.Socket;
import io.pillopl.supply.SupplySession;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NominalCurrentSumAlgorithmTest {

    final NominalCurrentSumAlgorithm algorithm = new NominalCurrentSumAlgorithm();

    final PowerSection section = new PowerSection("Meadow", new BigDecimal("48.00"),
            NominalCurrentSumAlgorithm.BEAN_NAME);

    final Socket asked = socket(1L, 16);

    @Test
    void anEmptySectionTakesASocketUpToItsMaximumCurrent() {
        assertThat(algorithm.isWithinBudget(section, asked, List.of())).isTrue();
        assertThat(algorithm.isWithinBudget(section, socket(9L, 32), List.of())).isTrue();
    }

    @Test
    void theRatedCurrentsOfActiveSessionsPlusTheNewSocketMayReachTheLimitExactly() {
        List<SupplySession> active = List.of(session(socket(2L, 16)), session(socket(3L, 16)));

        assertThat(algorithm.isWithinBudget(section, asked, active)).isTrue();
    }

    @Test
    void aSupplyThatWouldExceedTheSectionLimitIsNotWithinBudget() {
        List<SupplySession> active = List.of(session(socket(2L, 16)), session(socket(3L, 32)));

        assertThat(algorithm.isWithinBudget(section, asked, active)).isFalse();
    }

    @Test
    void aSocketRatedAboveTheSectionLimitNeverFits() {
        assertThat(algorithm.isWithinBudget(new PowerSection("Small", new BigDecimal("10"), null), asked, List.of()))
                .isFalse();
    }

    @Test
    void theSessionAlreadyRunningOnTheAskedSocketIsNotCountedTwice() {
        List<SupplySession> active = List.of(session(asked), session(socket(2L, 16)), session(socket(3L, 16)));

        assertThat(algorithm.isWithinBudget(section, asked, active)).isTrue();
    }

    @Test
    void theSessionCountsWithTheCurrentItStartedWithNotTheSocketsCurrentRating() {
        Socket rebuilt = socket(2L, 6);
        SupplySession startedWith32 = session(rebuilt);
        startedWith32.setMaxCurrentA(32);

        assertThat(algorithm.isWithinBudget(section, asked, List.of(startedWith32))).isTrue();
        assertThat(algorithm.isWithinBudget(section, socket(4L, 32), List.of(startedWith32))).isFalse();
    }

    static Socket socket(long id, int amperes) {
        Socket socket = mock(Socket.class);
        when(socket.getId()).thenReturn(id);
        when(socket.getMaxCurrentInAmperes()).thenReturn(amperes);
        return socket;
    }

    static SupplySession session(Socket socket) {
        SupplySession session = new SupplySession();
        session.setSocket(socket);
        session.setActive(true);
        session.setMaxCurrentA(socket.getMaxCurrentInAmperes());
        return session;
    }
}
