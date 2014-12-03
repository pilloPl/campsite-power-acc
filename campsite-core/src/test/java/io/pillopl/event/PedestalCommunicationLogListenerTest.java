package io.pillopl.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.pillopl.group.attribute.CommunicationLogAttributeService;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.monitoring.PedestalCommunicationLog;
import io.pillopl.monitoring.PedestalCommunicationLog.LogType;
import io.pillopl.persistence.dao.PedestalCommunicationLogDao;
import io.pillopl.persistence.dao.PedestalDao;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PedestalCommunicationLogListenerTest {

    static final LocalDateTime AT = LocalDateTime.of(2026, 7, 10, 8, 0);

    final CommunicationLogAttributeService attributeService = mock(CommunicationLogAttributeService.class);
    final PedestalDao pedestalDao = mock(PedestalDao.class);
    final PedestalCommunicationLogDao logDao = mock(PedestalCommunicationLogDao.class);
    final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    final PedestalCommunicationLogListener boot = new PedestalCommunicationLogListener.Boot(new EventService(),
            new EventTaskExecutorFactory(), attributeService, pedestalDao, logDao, objectMapper);

    final PedestalCommunicationLogListener record = new PedestalCommunicationLogListener.ConsumptionRecord(
            new EventService(), new EventTaskExecutorFactory(), attributeService, pedestalDao, logDao, objectMapper);

    final Pedestal pedestal = new Pedestal();

    @Test
    void bootOfALoggedPedestalIsLoggedWithItsTimeAndAttributes() {
        loggable(true);

        boot.log(new Event(EventType.BOOT_NOTIFICATION)
                .setAttribute(EventAttribute.PEDESTAL_CODE, "HR-BAY-P001")
                .setAttribute(EventAttribute.FIRMWARE_VERSION, "4.2.1")
                .setAttribute(EventAttribute.TIME, AT));

        PedestalCommunicationLog log = saved();
        assertThat(log.getLogType()).isEqualTo(LogType.BOOT);
        assertThat(log.getDate()).isEqualTo(AT);
        assertThat(log.getPedestal()).isSameAs(pedestal);
        assertThat(log.getMessage()).contains("\"FIRMWARE_VERSION\":\"4.2.1\"");
    }

    @Test
    void finishedSupplyIsLoggedAsConsumptionRecordAtItsStop() {
        loggable(true);

        record.log(new Event(EventType.SUPPLY_STOPPED)
                .setAttribute(EventAttribute.PEDESTAL_CODE, "HR-BAY-P001")
                .setAttribute(EventAttribute.STOP_METER_KWH, new BigDecimal("12.5"))
                .setAttribute(EventAttribute.STOP_TIME, AT.plusHours(2)));

        PedestalCommunicationLog log = saved();
        assertThat(log.getLogType()).isEqualTo(LogType.CONSUMPTION_RECORD);
        assertThat(log.getDate()).isEqualTo(AT.plusHours(2));
    }

    @Test
    void pedestalWithoutCommunicationLogIsNotLogged() {
        loggable(false);

        boot.log(new Event(EventType.BOOT_NOTIFICATION).setAttribute(EventAttribute.PEDESTAL_CODE, "HR-BAY-P001"));

        verify(logDao, never()).save(any());
    }

    @Test
    void eventWithoutPedestalOrOfAnUnknownPedestalIsNotLogged() {
        boot.log(new Event(EventType.BOOT_NOTIFICATION));
        when(attributeService.isPedestalLoggable("HR-GONE-P1")).thenReturn(true);
        boot.log(new Event(EventType.BOOT_NOTIFICATION).setAttribute(EventAttribute.PEDESTAL_CODE, "HR-GONE-P1"));

        verify(logDao, never()).save(any());
    }

    private void loggable(boolean loggable) {
        when(attributeService.isPedestalLoggable("HR-BAY-P001")).thenReturn(loggable);
        when(pedestalDao.findByCode("HR-BAY-P001")).thenReturn(pedestal);
    }

    private PedestalCommunicationLog saved() {
        ArgumentCaptor<PedestalCommunicationLog> log = ArgumentCaptor.forClass(PedestalCommunicationLog.class);
        verify(logDao).save(log.capture());
        return log.getValue();
    }
}
