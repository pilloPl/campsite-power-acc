package io.pillopl.monitoring;

import io.pillopl.event.Event;
import io.pillopl.event.EventAttribute;
import io.pillopl.event.EventService;
import io.pillopl.event.EventType;
import io.pillopl.infrastructure.Location;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalLinkVersion;
import io.pillopl.infrastructure.PedestalType;
import io.pillopl.infrastructure.Socket;
import io.pillopl.infrastructure.SocketStatus;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.persistence.dao.SocketStatusLogDao;
import io.pillopl.util.DateService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SocketStateTaskWorkerTest {

    static final LocalDateTime SERVER_NOW = LocalDateTime.of(2026, 7, 10, 8, 0);
    static final LocalDateTime REMOTE = SERVER_NOW.minusSeconds(20);
    static final long BREAKER = 1L;
    static final long TEMPERATURE = 4L;

    final SocketDao socketDao = mock(SocketDao.class);
    final SocketStatusLogDao socketStatusLogDao = mock(SocketStatusLogDao.class);
    final EventService eventService = mock(EventService.class);
    final PedestalMailService pedestalMailService = mock(PedestalMailService.class);
    final DateService dateService = mock(DateService.class);

    final SocketStateTaskWorker worker = new SocketStateTaskWorker(socketDao, socketStatusLogDao, eventService,
            pedestalMailService, dateService);

    Pedestal pedestal;
    Socket socket;

    @BeforeEach
    void setUp() {
        pedestal = new Pedestal("HR-BAY-P001", new PedestalType("ACME PEDESTALS", "P4", PedestalLinkVersion.V1),
                new Location("Rovinj", "52210", "Obala", "12"));
        pedestal.setId(1001L);
        socket = new Socket("HR-BAY-P001_1", null, 1);
        socket.setId(2001L);
        socket.setStatus(SocketStatus.AVAILABLE);
        socket.setMeterKwh(new BigDecimal("100.000"));
        socket.setMonitoring(true);
        pedestal.addSocket(socket);
        when(dateService.getNow()).thenReturn(SERVER_NOW);
    }

    @Test
    void messageWithoutTimestampIsAProgrammingError() {
        assertThatThrownBy(() -> worker.monitorSocket(socket, SocketStateParams.status(null, SocketStatus.FAULTED, 1L)))
                .isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> worker.monitorSocket(socket, null)).isInstanceOf(AssertionError.class);
    }

    @Test
    void messageForAMissingSocketChangesNothing() {
        worker.monitorSocket(null, SocketStateParams.status(REMOTE, SocketStatus.FAULTED, BREAKER));

        verifyNoInteractions(socketDao, socketStatusLogDao, eventService, pedestalMailService);
    }

    @Test
    void newFirmwareVersionIsStored() {
        socket.setFirmwareVersion("1.0");

        worker.monitorSocket(socket, new SocketStateParams(REMOTE, null, null, null, null, "1.1"));

        assertThat(socket.getFirmwareVersion()).isEqualTo("1.1");
    }

    @Test
    void repairedSocketIsMonitoredAgain() {
        socket.setStatus(SocketStatus.BREAKER_TRIPPED);
        socket.setMonitoring(false);

        worker.monitorSocket(socket, SocketStateParams.status(REMOTE, SocketStatus.AVAILABLE, null));

        assertThat(socket.isMonitoring()).isTrue();
        assertThat(socket.getStatus()).isEqualTo(SocketStatus.AVAILABLE);
    }

    @Test
    void availableAfterSupplyingDoesNotSwitchMonitoringOn() {
        socket.setStatus(SocketStatus.SUPPLYING);
        socket.setMonitoring(false);

        worker.monitorSocket(socket, SocketStateParams.status(REMOTE, SocketStatus.AVAILABLE, null));

        assertThat(socket.isMonitoring()).isFalse();
    }

    @Test
    void statusChangeIsDatedWithTheServerTimeAndTheSocketWithTheMessageTime() {
        worker.monitorSocket(socket, SocketStateParams.status(REMOTE, SocketStatus.SUPPLYING, null));

        assertThat(socket.getStatus()).isEqualTo(SocketStatus.SUPPLYING);
        assertThat(socket.getLastStatusUpdate()).isEqualTo(SERVER_NOW);
        assertThat(socket.getModDate()).isEqualTo(REMOTE);
        verify(socketDao).save(socket);
    }

    @Test
    void repeatedStatusKeepsTheTimeOfTheLastChange() {
        socket.setLastStatusUpdate(SERVER_NOW.minusHours(3));

        worker.monitorSocket(socket, SocketStateParams.status(REMOTE, SocketStatus.AVAILABLE, null));

        assertThat(socket.getLastStatusUpdate()).isEqualTo(SERVER_NOW.minusHours(3));
    }

    @Test
    void negativeMeterReadingIsSkippedAndAGoodOneStored() {
        worker.monitorSocket(socket, SocketStateParams.meterReading(REMOTE, new BigDecimal("-1")));
        assertThat(socket.getMeterKwh()).isEqualByComparingTo("100");

        worker.monitorSocket(socket, SocketStateParams.meterReading(REMOTE, new BigDecimal("101.5")));
        assertThat(socket.getMeterKwh()).isEqualByComparingTo("101.5");
    }

    @Test
    void meterReadingWithoutStatusPutsAMonitoredSocketBackToItsLastLoggedStatus() {
        socket.setStatus(SocketStatus.UNKNOWN);
        when(socketStatusLogDao.findLastBySocketId(2001L)).thenReturn(log(SocketStatus.SUPPLYING, 0L, "100"));

        worker.monitorSocket(socket, SocketStateParams.meterReading(REMOTE, new BigDecimal("101")));

        assertThat(socket.getStatus()).isEqualTo(SocketStatus.SUPPLYING);
    }

    @Test
    void lastLoggedLinkStatusesCountAsAvailable() {
        assertThat(SocketStatusLog.lastStatusOrAvailable(null)).isEqualTo(SocketStatus.AVAILABLE);
        assertThat(SocketStatusLog.lastStatusOrAvailable(log(SocketStatus.UNKNOWN, 0L, "1")))
                .isEqualTo(SocketStatus.AVAILABLE);
        assertThat(SocketStatusLog.lastStatusOrAvailable(log(SocketStatus.OCCUPIED, 0L, "1")))
                .isEqualTo(SocketStatus.AVAILABLE);
        assertThat(SocketStatusLog.lastStatusOrAvailable(log(SocketStatus.FAULTED, 0L, "1")))
                .isEqualTo(SocketStatus.FAULTED);
    }

    @Test
    void meterReadingWithoutStatusLeavesAnUnmonitoredSocketAlone() {
        socket.setStatus(SocketStatus.UNKNOWN);
        socket.setMonitoring(false);

        worker.monitorSocket(socket, SocketStateParams.meterReading(REMOTE, new BigDecimal("101")));

        assertThat(socket.getStatus()).isEqualTo(SocketStatus.UNKNOWN);
        verify(socketStatusLogDao, never()).save(any());
    }

    @Test
    void statusChangeStartsANewStatusLog() {
        when(socketStatusLogDao.findLastBySocketId(2001L)).thenReturn(log(SocketStatus.AVAILABLE, 0L, "100"));

        worker.monitorSocket(socket, new SocketStateParams(REMOTE, SocketStatus.SUPPLYING, null, null,
                new BigDecimal("12.5"), null));

        SocketStatusLog saved = savedLog();
        assertThat(saved.getId()).isNull();
        assertThat(saved.getStatus()).isEqualTo(SocketStatus.SUPPLYING);
        assertThat(saved.getCreateDate()).isEqualTo(SERVER_NOW);
        assertThat(saved.getRemoteDate()).isEqualTo(REMOTE);
        assertThat(saved.getMeterKwh()).isEqualByComparingTo("100");
        assertThat(saved.getCurrentA()).isEqualByComparingTo("12.5");
    }

    @Test
    void messageWithoutErrorFlagsKeepsTheFormerFlagsInTheNewLog() {
        when(socketStatusLogDao.findLastBySocketId(2001L)).thenReturn(log(SocketStatus.FAULTED, TEMPERATURE, "100"));

        worker.monitorSocket(socket, SocketStateParams.status(REMOTE, SocketStatus.AVAILABLE, null));

        assertThat(savedLog().getErrorFlags()).isEqualTo(TEMPERATURE);
    }

    @Test
    void sameStatusMovesTheLastLogForwardWithTheNewMeterReading() {
        SocketStatusLog last = log(SocketStatus.SUPPLYING, 0L, "100");
        socket.setStatus(SocketStatus.SUPPLYING);
        when(socketStatusLogDao.findLastBySocketId(2001L)).thenReturn(last);

        worker.monitorSocket(socket, SocketStateParams.meterReading(REMOTE, new BigDecimal("103")));

        assertThat(savedLog()).isSameAs(last);
        assertThat(last.getUpdateDate()).isEqualTo(SERVER_NOW);
        assertThat(last.getMeterKwh()).isEqualByComparingTo("103");
    }

    @Test
    void otherErrorFlagsOrCurrentStartANewLogEvenWithTheSameStatus() {
        socket.setStatus(SocketStatus.SUPPLYING);
        SocketStatusLog last = log(SocketStatus.SUPPLYING, 0L, "100");
        last.setCurrentA(new BigDecimal("10"));
        when(socketStatusLogDao.findLastBySocketId(2001L)).thenReturn(last);

        worker.monitorSocket(socket, new SocketStateParams(REMOTE, SocketStatus.SUPPLYING, null, null,
                new BigDecimal("16"), null));
        assertThat(savedLog()).isNotSameAs(last);
    }

    @Test
    void newErrorClassRaisesAnEventAndAServiceMail() throws MessagingException {
        when(socketStatusLogDao.findLastBySocketId(2001L)).thenReturn(log(SocketStatus.FAULTED, TEMPERATURE, "100"));

        worker.monitorSocket(socket, SocketStateParams.status(REMOTE, SocketStatus.BREAKER_TRIPPED, BREAKER));

        ArgumentCaptor<Event> event = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(event.capture());
        assertThat(event.getValue().getType()).isEqualTo(EventType.SOCKET_STATUS_CHANGED);
        assertThat(event.getValue().getAttribute(EventAttribute.SOCKET_ID)).isEqualTo(2001L);
        assertThat(event.getValue().getAttribute(EventAttribute.PEDESTAL_ID)).isEqualTo(1001L);
        assertThat(event.getValue().getAttribute(EventAttribute.SOCKET_STATUS)).isEqualTo(SocketStatus.BREAKER_TRIPPED);
        verify(pedestalMailService).sendErrorMail(socket, BREAKER);
    }

    @Test
    void sameErrorAgainIsNotMailedTwice() throws MessagingException {
        when(socketStatusLogDao.findLastBySocketId(2001L)).thenReturn(log(SocketStatus.FAULTED, BREAKER, "100"));

        worker.monitorSocket(socket, SocketStateParams.status(REMOTE, SocketStatus.FAULTED, BREAKER));

        verifyNoInteractions(eventService);
        verify(pedestalMailService, never()).sendErrorMail(any(), anyLong());
    }

    @Test
    void faultWithoutErrorFlagsOrWithoutErrorIsNotMailed() throws MessagingException {
        worker.monitorSocket(socket, SocketStateParams.status(REMOTE, SocketStatus.FAULTED, null));
        worker.monitorSocket(socket, SocketStateParams.status(REMOTE, SocketStatus.FAULTED, SocketStatusLog.NO_ERROR));

        verify(pedestalMailService, never()).sendErrorMail(any(), anyLong());
    }

    @Test
    void errorFlagsOfANonErrorStatusAreNotMailed() throws MessagingException {
        worker.monitorSocket(socket, SocketStateParams.status(REMOTE, SocketStatus.UNAVAILABLE, TEMPERATURE));

        verify(pedestalMailService, never()).sendErrorMail(any(), anyLong());
    }

    @Test
    void failingServiceMailDoesNotFailTheMessage() throws MessagingException {
        doThrow(new MessagingException("no recipient")).when(pedestalMailService).sendErrorMail(socket, BREAKER);

        worker.monitorSocket(socket, SocketStateParams.status(REMOTE, SocketStatus.FAULTED, BREAKER));

        assertThat(socket.getStatus()).isEqualTo(SocketStatus.FAULTED);
        verify(socketStatusLogDao).save(any());
    }

    @Test
    void errorCodeIsReadWithTheMaskOfTheManufacturerAndUnknownCodesAsUnknownError() {
        when(socketStatusLogDao.findErrorMask("ACME PEDESTALS", "BREAKER_TRIPPED")).thenReturn(BREAKER);
        when(socketStatusLogDao.findErrorMask("ACME PEDESTALS", SocketStateTaskWorker.UNKNOWN_ERROR_CODE))
                .thenReturn(4611686018427387904L);
        when(socketStatusLogDao.findErrorMask("ACME PEDESTALS", "SMOKE")).thenReturn(null);

        assertThat(worker.translateErrorCode(pedestal, "BREAKER_TRIPPED")).isEqualTo(BREAKER);
        assertThat(worker.translateErrorCode(pedestal, "SMOKE")).isEqualTo(4611686018427387904L);
        assertThat(worker.translateErrorCode(pedestal, null)).isNull();
        assertThat(worker.translateErrorCode(pedestal, " ")).isNull();
    }

    private SocketStatusLog savedLog() {
        ArgumentCaptor<SocketStatusLog> saved = ArgumentCaptor.forClass(SocketStatusLog.class);
        verify(socketStatusLogDao).save(saved.capture());
        return saved.getValue();
    }

    private SocketStatusLog log(SocketStatus status, long errorFlags, String meterKwh) {
        SocketStatusLog log = new SocketStatusLog(socket, SERVER_NOW.minusHours(1), SERVER_NOW.minusHours(1));
        log.setId(3001L);
        log.setStatus(status);
        log.setErrorFlags(errorFlags);
        log.setMeterKwh(new BigDecimal(meterKwh));
        return log;
    }
}
