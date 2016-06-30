package io.pillopl.pedestallink.v3;

import io.pillopl.card.Authorization;
import io.pillopl.card.ValidationStatus;
import io.pillopl.config.SystemPropertyService;
import io.pillopl.dto.pedestallink.v1.BootRequest;
import io.pillopl.dto.pedestallink.v2.SocketStatusRequest;
import io.pillopl.dto.pedestallink.v2.VendorDataRequest;
import io.pillopl.event.Event;
import io.pillopl.event.EventAttribute;
import io.pillopl.event.EventRetryService;
import io.pillopl.event.EventType;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalLinkVersion;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.PedestalType;
import io.pillopl.infrastructure.Socket;
import io.pillopl.infrastructure.SocketStatus;
import io.pillopl.monitoring.PedestalMailService;
import io.pillopl.monitoring.SocketStateParams;
import io.pillopl.monitoring.SocketStateTaskWorker;
import io.pillopl.pedestallink.v2.PedestalLinkClientV2;
import io.pillopl.pedestallink.v3.PedestalLinkServiceV3.TranslationHelper;
import io.pillopl.persistence.dao.SocketStatusLogDao;
import io.pillopl.supply.SupplyControlUnitImpl;
import io.pillopl.supply.SupplySessionManager;
import io.pillopl.util.DateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PedestalLinkServiceV3Test {

    static final String MANUFACTURER = "ADRIATEC";
    static final String CODE = "HR-BAY-P401";

    final PedestalService pedestalService = mock(PedestalService.class);
    final DateService dateService = mock(DateService.class);
    final EventRetryService eventRetryService = mock(EventRetryService.class);
    final SocketStateTaskWorker socketStateTaskWorker = mock(SocketStateTaskWorker.class);
    final SocketStatusLogDao socketStatusLogDao = mock(SocketStatusLogDao.class);
    final PedestalLinkClientV2 pedestalLinkClient = mock(PedestalLinkClientV2.class);
    final SystemPropertyService systemPropertyService = mock(SystemPropertyService.class);
    final PedestalLinkServiceV3 service = new PedestalLinkServiceV3(mock(Authorization.class), dateService,
            systemPropertyService, mock(SupplyControlUnitImpl.class), mock(SupplySessionManager.class),
            pedestalService, eventRetryService, pedestalLinkClient, socketStateTaskWorker, socketStatusLogDao,
            mock(PedestalMailService.class));

    final LocalDateTime now = LocalDateTime.of(2026, 7, 14, 10, 30);
    final Pedestal pedestal = new Pedestal();
    final Socket socket = new Socket(CODE + "_1", null, 1);

    @BeforeEach
    void setUp() {
        service.registerHandlers();
        pedestal.setId(11L);
        pedestal.setCode(CODE);
        pedestal.setPedestalType(new PedestalType(MANUFACTURER, "AT-3", PedestalLinkVersion.V3));
        pedestal.addSocket(socket);
        when(pedestalService.findByCode(CODE)).thenReturn(pedestal);
        when(pedestalService.findSocket(CODE, 1)).thenReturn(socket);
        when(dateService.getNow()).thenReturn(now);
    }

    @Test
    void linkV3TellsTheGuestWhyTheCardIsRefused() {
        assertThat(TranslationHelper.toLinkStatus(ValidationStatus.CARD_NOT_ACCEPTED_AT_SITE))
                .isEqualTo("NOT_ACCEPTED_HERE");
        assertThat(TranslationHelper.toLinkStatus(ValidationStatus.UNAVAILABLE))
                .isEqualTo("UNAVAILABLE");
        assertThat(TranslationHelper.toLinkStatus(ValidationStatus.SOCKET_OCCUPIED))
                .isEqualTo("OCCUPIED");
        assertThat(TranslationHelper.toLinkStatus(ValidationStatus.ACCEPTED)).isEqualTo("ACCEPTED");
        assertThat(TranslationHelper.toLinkStatus(ValidationStatus.EXPIRED)).isEqualTo("EXPIRED");
        assertThat(TranslationHelper.toLinkStatus(ValidationStatus.BLOCKED)).isEqualTo("BLOCKED");
        assertThat(TranslationHelper.toLinkStatus(ValidationStatus.CARD_INVALID))
                .isEqualTo("INVALID");
        assertThat(TranslationHelper.toLinkStatus(null)).isEqualTo("INVALID");
    }

    @Test
    void linkV3SocketStatusesAreTheV2Statuses() {
        assertThat(TranslationHelper.translateSocketStatus("OCCUPIED", null)).isEqualTo(SocketStatus.OCCUPIED);
        assertThat(TranslationHelper.translateSocketStatus("SUPPLYING", null)).isEqualTo(SocketStatus.SUPPLYING);
        assertThat(TranslationHelper.translateSocketStatus("FAULTED", "BREAKER_TRIPPED"))
                .isEqualTo(SocketStatus.BREAKER_TRIPPED);
        assertThat(TranslationHelper.translateSocketStatus("FAULTED", "GROUND_FAILURE")).isEqualTo(SocketStatus.FAULTED);
        assertThat(TranslationHelper.translateSocketStatus("FIRMWARE_UPDATE", null))
                .isEqualTo(SocketStatus.FIRMWARE_UPDATE);
        assertThatThrownBy(() -> TranslationHelper.translateSocketStatus("SMOKING", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TranslationHelper.translateSocketStatus(null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void socketInErrorDoesNotSupplyBeforeItIsAvailableAgain() {
        assertThat(TranslationHelper.isExpectedTransition(SocketStatus.BREAKER_TRIPPED, SocketStatus.SUPPLYING))
                .isFalse();
        assertThat(TranslationHelper.isExpectedTransition(SocketStatus.FAULTED, SocketStatus.OCCUPIED)).isFalse();
        assertThat(TranslationHelper.isExpectedTransition(SocketStatus.UNAVAILABLE, SocketStatus.SUPPLYING))
                .isFalse();
        assertThat(TranslationHelper.isExpectedTransition(SocketStatus.FAULTED, SocketStatus.AVAILABLE)).isTrue();
        assertThat(TranslationHelper.isExpectedTransition(SocketStatus.OCCUPIED, SocketStatus.SUPPLYING)).isTrue();
        assertThat(TranslationHelper.isExpectedTransition(null, SocketStatus.SUPPLYING)).isTrue();
        assertThat(TranslationHelper.isExpectedTransition(SocketStatus.SUPPLYING, SocketStatus.SUPPLYING)).isTrue();
    }

    @Test
    void messageWithoutRegisteredHandlerIsATechnicalError() {
        PedestalLinkServiceV3 unregistered = new PedestalLinkServiceV3(null, null, null, null, null, null, null, null,
                null, null, null);

        assertThatThrownBy(() -> unregistered.handleRequest(PedestalLinkV3Handler.Boot.class, CODE, null))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.handleRequest(null, CODE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void statusOfThePedestalItselfIsOnlyPassedOnAsEvent() {
        service.handleRequest(PedestalLinkV3Handler.SocketStatusHandler.class, CODE,
                new SocketStatusRequest(0, "UNAVAILABLE", "NO_ERROR", MANUFACTURER, null, "maintenance", now));

        verify(socketStateTaskWorker, never()).monitorSocket(any(), any());
        Event event = sentEvent();
        assertThat(event.getType()).isEqualTo(EventType.SOCKET_STATE_NOTIFICATION);
        assertThat(event.getAttribute(EventAttribute.SOCKET_NO)).isEqualTo(0);
        assertThat(event.getAttribute(EventAttribute.SOCKET_ID)).isNull();
        assertThat(event.getAttribute(EventAttribute.SOCKET_STATUS)).isEqualTo(SocketStatus.UNAVAILABLE);
    }

    @Test
    void unexpectedStatusChangeIsStillMonitoredWithTheVendorMaskFirst() {
        socket.setStatus(SocketStatus.BREAKER_TRIPPED);
        when(socketStatusLogDao.findErrorMask(MANUFACTURER, "E07")).thenReturn(4L);

        service.handleRequest(PedestalLinkV3Handler.SocketStatusHandler.class, CODE,
                new SocketStatusRequest(1, "SUPPLYING", "OTHER_ERROR", MANUFACTURER, "E07", null, now));

        verify(socketStateTaskWorker).monitorSocket(socket, SocketStateParams.status(now, SocketStatus.SUPPLYING, 4L));
        verify(socketStateTaskWorker, never()).translateErrorCode(any(), any());
        assertThat(sentEvent().getAttribute(EventAttribute.PEDESTAL_VENDOR_ERROR_CODE)).isEqualTo("E07");
    }

    @Test
    void vendorCodeWithoutErrorFallsBackToTheLinkErrorCode() {
        when(socketStatusLogDao.findErrorMask(MANUFACTURER, "E00")).thenReturn(0L);
        when(socketStateTaskWorker.translateErrorCode(pedestal, "GROUND_FAILURE")).thenReturn(2L);

        service.handleRequest(PedestalLinkV3Handler.SocketStatusHandler.class, CODE,
                new SocketStatusRequest(1, "FAULTED", "GROUND_FAILURE", MANUFACTURER, "E00", null, now));

        verify(socketStateTaskWorker).monitorSocket(socket, SocketStateParams.status(now, SocketStatus.FAULTED, 2L));
    }

    @Test
    void statusOfAnUnknownPedestalIsRefused() {
        assertThatThrownBy(() -> service.handleRequest(PedestalLinkV3Handler.SocketStatusHandler.class, "HR-NOWHERE",
                new SocketStatusRequest(1, "AVAILABLE", null, null, null, null, now)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bootEventOfLinkV3GoesThroughTheRetry() {
        when(pedestalService.updateBootData(CODE, "3.1.0", "SN-401", null)).thenReturn(pedestal);
        when(systemPropertyService.getPedestalHeartbeatIntervalInSeconds()).thenReturn(600);
        when(pedestalLinkClient.hasQueuedLocalList(pedestal)).thenReturn(true);

        assertThat(service.handleRequest(PedestalLinkV3Handler.Boot.class, CODE,
                new BootRequest(MANUFACTURER, "AT-3", "SN-401", "3.1.0", null, null, null)).status())
                .isEqualTo("ACCEPTED");

        assertThat(sentEvent().getType()).isEqualTo(EventType.BOOT_NOTIFICATION);
        verify(pedestalLinkClient).sendQueuedLocalList(pedestal);
    }

    @Test
    void bootOfAnUnknownPedestalIsRejectedWithoutEvent() {
        assertThat(service.handleRequest(PedestalLinkV3Handler.Boot.class, "HR-NOWHERE",
                new BootRequest(MANUFACTURER, "AT-3", null, null, null, null, null)).status()).isEqualTo("REJECTED");

        verify(eventRetryService, never()).sendOrRetryLater(any());
    }

    @Test
    void vendorDataIsAcceptedOnlyFromTheManufacturerWithAMessageId() {
        assertThat(service.handleRequest(PedestalLinkV3Handler.VendorData.class, CODE,
                new VendorDataRequest("OTHER", "DOOR", "open")).status()).isEqualTo("UNKNOWN_VENDOR");
        assertThat(service.handleRequest(PedestalLinkV3Handler.VendorData.class, CODE,
                new VendorDataRequest(MANUFACTURER, null, "open")).status()).isEqualTo("UNKNOWN_MESSAGE_ID");
        assertThat(service.handleRequest(PedestalLinkV3Handler.VendorData.class, CODE,
                new VendorDataRequest(MANUFACTURER, "DOOR", "open")).status()).isEqualTo("ACCEPTED");

        assertThat(sentEvent().getAttribute(EventAttribute.VENDOR_DATA)).isEqualTo("open");
    }

    private Event sentEvent() {
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRetryService).sendOrRetryLater(captor.capture());
        return captor.getValue();
    }
}
