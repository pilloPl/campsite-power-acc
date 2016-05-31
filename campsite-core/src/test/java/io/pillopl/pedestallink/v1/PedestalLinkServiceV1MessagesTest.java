package io.pillopl.pedestallink.v1;

import io.pillopl.card.Authorization;
import io.pillopl.config.SystemPropertyService;
import io.pillopl.dto.pedestallink.v1.MeterReading;
import io.pillopl.dto.pedestallink.v1.MeterReadingsRequest;
import io.pillopl.dto.pedestallink.v1.SocketStatusRequest;
import io.pillopl.event.EventService;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.Socket;
import io.pillopl.monitoring.PedestalMailService;
import io.pillopl.monitoring.SocketStateParams;
import io.pillopl.monitoring.SocketStateTaskWorker;
import io.pillopl.supply.SupplyControlUnitImpl;
import io.pillopl.supply.SupplySessionManager;
import io.pillopl.util.DateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PedestalLinkServiceV1MessagesTest {

    static final String CODE = "HR-BAY-P001";

    final PedestalService pedestalService = mock(PedestalService.class);
    final DateService dateService = mock(DateService.class);
    final SocketStateTaskWorker socketStateTaskWorker = mock(SocketStateTaskWorker.class);
    final PedestalLinkClientV1 pedestalLinkClient = mock(PedestalLinkClientV1.class);
    final PedestalLinkServiceV1 service = new PedestalLinkServiceV1(mock(Authorization.class), dateService,
            mock(SystemPropertyService.class), mock(SupplyControlUnitImpl.class), mock(SupplySessionManager.class),
            pedestalService, mock(EventService.class), pedestalLinkClient, socketStateTaskWorker,
            mock(PedestalMailService.class));

    final LocalDateTime now = LocalDateTime.of(2026, 7, 14, 10, 30);
    final Pedestal pedestal = new Pedestal();
    final Socket socket = new Socket(CODE + "_1", null, 1);

    @BeforeEach
    void setUp() {
        pedestal.setId(5L);
        pedestal.setCode(CODE);
        pedestal.addSocket(socket);
        when(pedestalService.findByCode(CODE)).thenReturn(pedestal);
        when(pedestalService.findSocket(CODE, 1)).thenReturn(socket);
        when(dateService.getNow()).thenReturn(now);
    }

    @Test
    void heartbeatOfAnUnknownPedestalIsOnlyAcknowledged() {
        assertThat(service.heartbeat("HR-NOWHERE").currentTime()).isEqualTo(now);

        verify(pedestalService, never()).registerIncoming(any(), any());
        verifyNoInteractions(pedestalLinkClient);
    }

    @Test
    void statusOfAnUnknownSocketIsRefusedBeforeAnythingIsMonitored() {
        assertThatThrownBy(() -> service.socketStatus(CODE, new SocketStatusRequest(4, "AVAILABLE", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown socket 4");

        verify(pedestalService, never()).registerIncoming(any(), any());
        verifyNoInteractions(socketStateTaskWorker);
    }

    @Test
    void meterReadingWithoutValueIsSkippedAndOneWithoutTimeTakesTheCurrentTime() {
        service.meterReadings(CODE, new MeterReadingsRequest(1, 1007, Arrays.asList(
                new MeterReading(now.minusMinutes(5), null),
                new MeterReading(null, 12345))));

        verify(socketStateTaskWorker).monitorSocket(socket,
                SocketStateParams.meterReading(now, new BigDecimal("12.345")));
        verify(socketStateTaskWorker, never()).monitorSocket(socket,
                SocketStateParams.meterReading(now.minusMinutes(5), null));
    }
}
