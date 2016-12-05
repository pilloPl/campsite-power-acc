package io.pillopl.infrastructure;

import io.pillopl.group.GroupService;
import io.pillopl.monitoring.PedestalMailService;
import io.pillopl.monitoring.SocketStatusLog;
import io.pillopl.persistence.dao.EventRetryDao;
import io.pillopl.persistence.dao.PedestalCommunicationLogDao;
import io.pillopl.persistence.dao.PedestalDao;
import io.pillopl.persistence.dao.PedestalStateDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.persistence.dao.SocketStatusLogDao;
import io.pillopl.persistence.dao.SupplySessionDao;
import io.pillopl.supply.RemoteSupplyControl;
import io.pillopl.supply.SupplySession;
import io.pillopl.user.UserinfoService;
import io.pillopl.util.DateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PedestalServiceServiceCommandsTest {

    final SocketDao socketDao = mock(SocketDao.class);
    final SocketStatusLogDao socketStatusLogDao = mock(SocketStatusLogDao.class);
    final SupplySessionDao supplySessionDao = mock(SupplySessionDao.class);
    final RemoteSupplyControl remoteSupplyControl = mock(RemoteSupplyControl.class);
    final PedestalMailService pedestalMailService = mock(PedestalMailService.class);

    final PedestalService pedestalService = new PedestalService(mock(PedestalDao.class), socketDao,
            mock(PedestalStateDao.class), mock(GroupService.class), mock(UserinfoService.class),
            mock(LocationService.class), socketStatusLogDao, mock(PedestalCommunicationLogDao.class),
            mock(EventRetryDao.class), supplySessionDao, remoteSupplyControl, pedestalMailService,
            new DateService());

    final PedestalType type = new PedestalType("ACME", "P6", PedestalLinkVersion.V2);
    final Pedestal pedestal = new Pedestal();
    final Socket socket = new Socket();

    @BeforeEach
    void setUp() {
        pedestal.setCode("HR-BAY-P009");
        pedestal.setPedestalType(type);
        socket.setId(5L);
        socket.setName("P009_2");
        socket.setSocketNo(2);
        socket.setPedestal(pedestal);
        socket.setMeterKwh(new BigDecimal("12.5"));
    }

    @Test
    void trippedBreakerIsSwitchedOnRemotelyWhenTheTypeCanDoIt() {
        type.setRemoteBreakerReset(true);
        socket.setStatus(SocketStatus.BREAKER_TRIPPED);
        when(remoteSupplyControl.resetBreaker(pedestal, 2)).thenReturn(true);

        assertThat(pedestalService.resetFault(socket)).isTrue();

        assertThat(socket.getStatus()).isEqualTo(SocketStatus.AVAILABLE);
        ArgumentCaptor<SocketStatusLog> log = ArgumentCaptor.forClass(SocketStatusLog.class);
        verify(socketStatusLogDao).save(log.capture());
        assertThat(log.getValue().getStatus()).isEqualTo(SocketStatus.AVAILABLE);
        assertThat(log.getValue().getErrorFlags()).isZero();
        assertThat(log.getValue().getMeterKwh()).isEqualByComparingTo("12.5");
    }

    @Test
    void refusedRemoteResetKeepsTheFault() {
        type.setRemoteBreakerReset(true);
        socket.setStatus(SocketStatus.BREAKER_TRIPPED);

        assertThatThrownBy(() -> pedestalService.resetFault(socket)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("did not accept");
        assertThat(socket.getStatus()).isEqualTo(SocketStatus.BREAKER_TRIPPED);
        verify(socketStatusLogDao, never()).save(any());
    }

    @Test
    void faultWithoutRemoteResetIsEndedByTheServiceTeamAndAnActiveSessionLeavesTheSocketOccupied() {
        socket.setStatus(SocketStatus.BREAKER_TRIPPED);
        when(supplySessionDao.findActiveBySocket(socket)).thenReturn(List.of(new SupplySession()));

        assertThat(pedestalService.resetFault(socket)).isFalse();

        assertThat(socket.getStatus()).isEqualTo(SocketStatus.OCCUPIED);
        verify(remoteSupplyControl, never()).resetBreaker(any(), anyInt());
    }

    @Test
    void socketWithoutFaultCannotBeReset() {
        socket.setStatus(SocketStatus.SUPPLYING);

        assertThatThrownBy(() -> pedestalService.resetFault(socket)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not in fault");
    }

    @Test
    void lastLoggedFaultIsMailedAgain() throws Exception {
        SocketStatusLog fault = new SocketStatusLog();
        fault.setErrorFlags(12L);
        when(socketStatusLogDao.findLastFaultBySocketId(any(), any())).thenReturn(List.of(fault));

        assertThat(pedestalService.resendFaultMail(socket)).isEqualTo(12L);

        verify(pedestalMailService).sendErrorMail(socket, 12L);
    }

    @Test
    void statusRequestThePedestalRefusesIsAConflict() {
        assertThatThrownBy(() -> pedestalService.requestStatus(pedestal, null))
                .isInstanceOf(IllegalStateException.class);
        when(remoteSupplyControl.requestStatus(pedestal, null)).thenReturn(true);
        pedestalService.requestStatus(pedestal, null);
    }
}
