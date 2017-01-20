package io.pillopl.pedestallink.v2;

import io.pillopl.card.Authorization;
import io.pillopl.card.ValidationStatus;
import io.pillopl.config.SystemPropertyService;
import io.pillopl.dto.pedestallink.v1.BootRequest;
import io.pillopl.dto.pedestallink.v2.SocketStatusRequest;
import io.pillopl.dto.pedestallink.v2.VendorDataRequest;
import io.pillopl.event.Event;
import io.pillopl.event.EventAttribute;
import io.pillopl.event.EventRetryService;
import io.pillopl.event.EventService;
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

class PedestalLinkServiceV2Test {

    static final String MANUFACTURER = "ADRIATEC";

    final PedestalService pedestalService = mock(PedestalService.class);
    final DateService dateService = mock(DateService.class);
    final EventRetryService eventRetryService = mock(EventRetryService.class);
    final SocketStateTaskWorker socketStateTaskWorker = mock(SocketStateTaskWorker.class);
    final SocketStatusLogDao socketStatusLogDao = mock(SocketStatusLogDao.class);
    final PedestalLinkServiceV2 service = new PedestalLinkServiceV2(mock(Authorization.class), dateService,
            mock(SystemPropertyService.class), mock(SupplyControlUnitImpl.class), mock(SupplySessionManager.class),
            pedestalService, mock(EventService.class), eventRetryService, mock(PedestalLinkClientV2.class),
            socketStateTaskWorker, socketStatusLogDao, mock(PedestalMailService.class));

    final LocalDateTime now = LocalDateTime.of(2026, 7, 14, 10, 30);
    final Pedestal pedestal = new Pedestal();
    final Socket socket = new Socket("HR-BAY-P101_1", null, 1);

    @BeforeEach
    void setUp() {
        pedestal.setId(9L);
        pedestal.setCode("HR-BAY-P101");
        pedestal.setPedestalType(new PedestalType(MANUFACTURER, "AT-2", PedestalLinkVersion.V2));
        pedestal.addSocket(socket);
        when(pedestalService.findByCode("HR-BAY-P101")).thenReturn(pedestal);
        when(dateService.getNow()).thenReturn(now);
    }

    @Test
    void linkV2TellsTheGuestWhenThePowerBudgetRefusesTheSupply() {
        assertThat(PedestalLinkServiceV2.translateFrom(ValidationStatus.UNAVAILABLE)).isEqualTo("UNAVAILABLE");
        assertThat(PedestalLinkServiceV2.translateFrom(ValidationStatus.ACCEPTED)).isEqualTo("ACCEPTED");
        assertThat(PedestalLinkServiceV2.translateFrom(ValidationStatus.SOCKET_OCCUPIED)).isEqualTo("OCCUPIED");
        assertThat(PedestalLinkServiceV2.translateFrom(ValidationStatus.EXPIRED)).isEqualTo("EXPIRED");
        assertThat(PedestalLinkServiceV2.translateFrom(ValidationStatus.BLOCKED)).isEqualTo("BLOCKED");
        assertThat(PedestalLinkServiceV2.translateFrom(ValidationStatus.CARD_NOT_ACCEPTED_AT_SITE))
                .isEqualTo("INVALID");
        assertThat(PedestalLinkServiceV2.translateFrom(null)).isEqualTo("INVALID");
    }

    @Test
    void linkV2KnowsPluggedInSupplyingTrippedAndFirmwareUpdate() {
        assertThat(PedestalLinkServiceV2.translateStatus("OCCUPIED")).isEqualTo(SocketStatus.OCCUPIED);
        assertThat(PedestalLinkServiceV2.translateStatus("SUPPLYING")).isEqualTo(SocketStatus.SUPPLYING);
        assertThat(PedestalLinkServiceV2.translateStatus("BREAKER_TRIPPED")).isEqualTo(SocketStatus.BREAKER_TRIPPED);
        assertThat(PedestalLinkServiceV2.translateStatus("FIRMWARE_UPDATE")).isEqualTo(SocketStatus.FIRMWARE_UPDATE);
        assertThat(PedestalLinkServiceV2.translateStatus("AVAILABLE")).isEqualTo(SocketStatus.AVAILABLE);
        assertThat(PedestalLinkServiceV2.translateStatus("UNAVAILABLE")).isEqualTo(SocketStatus.UNAVAILABLE);
        assertThat(PedestalLinkServiceV2.translateStatus("FAULTED", "BREAKER_TRIPPED"))
                .isEqualTo(SocketStatus.BREAKER_TRIPPED);
        assertThat(PedestalLinkServiceV2.translateStatus("FAULTED", "GROUND_FAILURE")).isEqualTo(SocketStatus.FAULTED);
    }

    @Test
    void statusUnknownToLinkV2IsRefused() {
        assertThatThrownBy(() -> PedestalLinkServiceV2.translateStatus("SMOKING"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PedestalLinkServiceV2.translateStatus(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownPedestalIsRejectedAtBootAndOnlyAcknowledgedAtHeartbeat() {
        assertThat(service.boot("HR-NOWHERE", new BootRequest(MANUFACTURER, "AT-2", null, null, null, null, null))
                .status()).isEqualTo("REJECTED");
        assertThat(service.heartbeat("HR-NOWHERE").currentTime()).isEqualTo(now);

        verify(pedestalService, never()).registerIncoming(any(), any());
    }

    @Test
    void vendorErrorCodeIsTranslatedWithTheMaskOfTheManufacturerFirst() {
        when(socketStatusLogDao.findErrorMask(MANUFACTURER, "E02")).thenReturn(2L);

        assertThat(service.translateErrorCode(pedestal, status("FAULTED", "OTHER_ERROR", "E02"))).isEqualTo(2L);
        verify(socketStateTaskWorker, never()).translateErrorCode(any(), any());
    }

    @Test
    void vendorCodeWithoutErrorFallsBackToTheLinkErrorCode() {
        when(socketStatusLogDao.findErrorMask(MANUFACTURER, "E00")).thenReturn(0L);
        when(socketStateTaskWorker.translateErrorCode(pedestal, "GROUND_FAILURE")).thenReturn(2L);

        assertThat(service.translateErrorCode(pedestal, status("FAULTED", "GROUND_FAILURE", "E00"))).isEqualTo(2L);
    }

    @Test
    void unknownVendorCodeFallsBackToTheLinkErrorCode() {
        when(socketStateTaskWorker.translateErrorCode(pedestal, "NO_ERROR")).thenReturn(0L);

        assertThat(service.translateErrorCode(pedestal, status("AVAILABLE", "NO_ERROR", "X42"))).isEqualTo(0L);
    }

    @Test
    void statusWithoutVendorCodeUsesTheLinkErrorCode() {
        when(socketStateTaskWorker.translateErrorCode(pedestal, "BREAKER_TRIPPED")).thenReturn(1L);

        assertThat(service.translateErrorCode(pedestal, status("BREAKER_TRIPPED", "BREAKER_TRIPPED", " ")))
                .isEqualTo(1L);
        verify(socketStatusLogDao, never()).findErrorMask(any(), any());
    }

    @Test
    void socketStatusIsMonitoredAndItsEventGoesThroughTheRetry() {
        when(socketStatusLogDao.findErrorMask(MANUFACTURER, "E01")).thenReturn(1L);

        service.socketStatus("HR-BAY-P101", status("BREAKER_TRIPPED", "BREAKER_TRIPPED", "E01"));

        verify(socketStateTaskWorker).monitorSocket(socket, SocketStateParams.status(now,
                SocketStatus.BREAKER_TRIPPED, 1L));
        Event event = sentEvent();
        assertThat(event.getType()).isEqualTo(EventType.SOCKET_STATE_NOTIFICATION);
        assertThat(event.getAttribute(EventAttribute.PEDESTAL_VENDOR_ERROR_CODE)).isEqualTo("E01");
        assertThat(event.getAttribute(EventAttribute.PEDESTAL_VENDOR)).isEqualTo(MANUFACTURER);
        assertThat(event.getAttribute(EventAttribute.SOCKET_STATUS)).isEqualTo(SocketStatus.BREAKER_TRIPPED);
        assertThat(event.getAttribute(EventAttribute.TIME)).isEqualTo(now.minusSeconds(5));
    }

    @Test
    void failingStatusEventDoesNotFailTheMessage() {
        when(eventRetryService.sendOrRetryLater(any())).thenThrow(new IllegalStateException("broken"));

        assertThat(service.socketStatus("HR-BAY-P101", status("AVAILABLE", "NO_ERROR", null)).currentTime())
                .isEqualTo(now);
    }

    @Test
    void statusWithoutPedestalCodeIsRefused() {
        assertThatThrownBy(() -> service.socketStatus(null, status("AVAILABLE", "NO_ERROR", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("without pedestal code");
    }

    @Test
    void statusOfAnUnknownSocketIsRefused() {
        SocketStatusRequest request = new SocketStatusRequest(0, "AVAILABLE", "NO_ERROR", null, null, null, now);

        assertThatThrownBy(() -> service.socketStatus("HR-BAY-P101", request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(socketStateTaskWorker, never()).monitorSocket(any(), any());
    }

    @Test
    void vendorDataOfTheManufacturerIsPassedOnAsEvent() {
        assertThat(service.vendorData("HR-BAY-P101", new VendorDataRequest("adriatec", "DOOR", "open")).status())
                .isEqualTo("ACCEPTED");

        Event event = sentEvent();
        assertThat(event.getType()).isEqualTo(EventType.VENDOR_DATA);
        assertThat(event.getAttribute(EventAttribute.VENDOR_MESSAGE_ID)).isEqualTo("DOOR");
        assertThat(event.getAttribute(EventAttribute.VENDOR_DATA)).isEqualTo("open");
    }

    @Test
    void vendorDataOfAnotherVendorIsRefused() {
        assertThat(service.vendorData("HR-BAY-P101", new VendorDataRequest("OTHER", "DOOR", "open")).status())
                .isEqualTo("UNKNOWN_VENDOR");
        assertThat(service.vendorData("HR-BAY-P101", new VendorDataRequest(null, "DOOR", "open")).status())
                .isEqualTo("UNKNOWN_VENDOR");
        verify(eventRetryService, never()).sendOrRetryLater(any());
    }

    @Test
    void vendorDataWithoutMessageIdIsRefused() {
        assertThat(service.vendorData("HR-BAY-P101", new VendorDataRequest(MANUFACTURER, " ", "open")).status())
                .isEqualTo("UNKNOWN_MESSAGE_ID");
        verify(eventRetryService, never()).sendOrRetryLater(any());
    }

    @Test
    void vendorDataOfAnUnknownPedestalIsRefused() {
        assertThatThrownBy(() -> service.vendorData("HR-NOWHERE", new VendorDataRequest(MANUFACTURER, "DOOR", "x")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private SocketStatusRequest status(String status, String errorCode, String vendorErrorCode) {
        return new SocketStatusRequest(1, status, errorCode, MANUFACTURER, vendorErrorCode, null, now.minusSeconds(5));
    }

    private Event sentEvent() {
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRetryService).sendOrRetryLater(captor.capture());
        return captor.getValue();
    }
}
