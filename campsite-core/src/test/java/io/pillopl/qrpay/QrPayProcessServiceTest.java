package io.pillopl.qrpay;

import io.pillopl.config.SystemPropertyService;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.QrPayProcessDao;
import io.pillopl.qrpay.state.QrPayStateFactory;
import io.pillopl.util.DateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QrPayProcessServiceTest {

    final QrPayProcessDao dao = mock(QrPayProcessDao.class);
    final QrPayProcessService service = new QrPayProcessService(dao, mock(QrPayStateFactory.class),
            new DateService(), mock(SystemPropertyService.class));

    final Socket socket = new Socket();
    final QrPayProcess current = new QrPayProcess("ORD-1", 5L, "payment-app", "guest-7", LocalDateTime.now());

    @BeforeEach
    void setUp() {
        socket.setId(5L);
        current.setId(1003L);
        current.setProcessId(1000L);
        when(dao.findCurrentByProcessId(1000L)).thenReturn(current);
    }

    @Test
    void pedestalMayStartTheSupplyWhileThePaymentWaitsForIt() {
        current.setState(QrPayStateName.SOCKET_START_WAITING.name());

        assertThat(service.isSupplyAuthorized(1000L, socket, false)).isTrue();
    }

    @Test
    void pedestalMayNotStartTheSupplyOfAStoppedOrFailedPayment() {
        current.setState(QrPayStateName.STOP_REQUESTED.name());
        assertThat(service.isSupplyAuthorized(1000L, socket, false)).isFalse();

        current.setState(QrPayStateName.ERROR.name());
        assertThat(service.isSupplyAuthorized(1000L, socket, false)).isFalse();
    }

    @Test
    void stopOfARunningSupplyOnlyNeedsThePaymentAtTheSocket() {
        current.setState(QrPayStateName.ERROR.name());

        assertThat(service.isSupplyAuthorized(1000L, socket, true)).isTrue();
    }

    @Test
    void paymentOfAnotherSocketOrUnknownPaymentIsNotAuthorized() {
        Socket other = new Socket();
        other.setId(6L);

        assertThat(service.isSupplyAuthorized(1000L, other, false)).isFalse();
        assertThat(service.isSupplyAuthorized(1000L, null, true)).isFalse();
        assertThat(service.isSupplyAuthorized(999L, socket, true)).isFalse();
    }

    @Test
    void openPaymentOfAnotherProcessMakesTheSocketBusy() {
        when(dao.findOpenBySocketId(5L)).thenReturn(List.of(current));

        assertThat(service.hasOtherOpenProcess(5L, 1000L)).isFalse();
        assertThat(service.hasOtherOpenProcess(5L, 2000L)).isTrue();
    }

    @Test
    void archiveWorksInBatches() {
        List<Long> ids = LongStream.rangeClosed(1, 1001).boxed().toList();

        assertThat(QrPayProcessService.batches(ids)).extracting(List::size).containsExactly(500, 500, 1);
        assertThat(QrPayProcessService.batches(List.of())).isEmpty();
    }
}
