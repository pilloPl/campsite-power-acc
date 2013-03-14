package io.pillopl.infrastructure;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PedestalTest {

    private final Pedestal pedestal = new Pedestal("HR-BAY-P001", new PedestalType("ACME", "P4", PedestalLinkVersion.V1),
            new Location("Rovinj", "52210", "Obala", "12"));

    @Test
    void pedestalIsInErrorWhenAMonitoredSocketReportsAnError() {
        Socket socket = new Socket("1", null, 1);
        socket.setMonitoring(true);
        socket.updateStatus(SocketStatus.BREAKER_TRIPPED, LocalDateTime.now());
        pedestal.addSocket(socket);

        assertThat(pedestal.hasSocketInError()).isTrue();
    }

    @Test
    void errorOfASocketWithoutMonitoringIsIgnored() {
        Socket socket = new Socket("1", null, 1);
        socket.updateStatus(SocketStatus.FAULTED, LocalDateTime.now());
        pedestal.addSocket(socket);

        assertThat(pedestal.hasSocketInError()).isFalse();
    }

    @Test
    void socketIsFoundByItsPitch() {
        Socket socket = new Socket("1", null, 1);
        socket.setPitchNo("A-17");
        pedestal.addSocket(socket);

        assertThat(pedestal.findSocketByPitchNo("A-17")).isSameAs(socket);
        assertThat(pedestal.findSocketByPitchNo("A-18")).isNull();
        assertThat(socket.isAssignedToPitch()).isTrue();
    }

    @Test
    void socketWithoutBreakerRatingMayDrawTheStandardCurrent() {
        assertThat(new Socket("1", null, 1).getMaxCurrentInAmperes()).isEqualTo(Socket.STD_MAX_CURRENT_IN_AMPERES);
    }
}
