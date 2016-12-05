package io.pillopl.webservice.console.monitoring;

import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupType;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalState;
import io.pillopl.infrastructure.Socket;
import io.pillopl.infrastructure.SocketStatus;
import io.pillopl.monitoring.PedestalCommunicationLog;
import io.pillopl.monitoring.SocketStatusLog;
import io.pillopl.pedestallink.v1.PedestalLinkClientV1;
import io.pillopl.persistence.dao.PedestalCommunicationLogDao;
import io.pillopl.persistence.dao.PedestalStateDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.persistence.dao.SocketStatusLogDao;
import io.pillopl.user.UserroleName;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MonitoringWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.MONITORING;

    @MockitoBean PedestalLinkClientV1 pedestalLink;

    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired SocketDao socketDao;
    @Autowired SocketStatusLogDao socketStatusLogDao;
    @Autowired PedestalCommunicationLogDao communicationLogDao;
    @Autowired PedestalStateDao pedestalStateDao;

    Group bay;
    Pedestal bayPedestal;
    Socket baySocket;
    Pedestal lakePedestal;

    @BeforeEach
    void setUp() {
        bay = groupManager.createChildGroup(console.site("HR SITES"), "BAY MONITORING CONSOLE", "Bay", GroupType.SITE);
        Group lake = groupManager.createChildGroup(console.site("HR SITES"), "LAKE MONITORING CONSOLE", "Lake",
                GroupType.SITE);
        bayPedestal = infrastructure.pedestal("HR-BAYMON-P001", bay, 2);
        lakePedestal = infrastructure.pedestal("HR-LAKEMON-P001", lake, 1);
        baySocket = bayPedestal.getSockets().get(0);
        console.user("bayservice", UserroleName.SERVICE_TECHNICIAN, console.staff("HR RECEPTION"), bay);
        console.user("baydesk", UserroleName.RECEPTION, console.staff("HR RECEPTION"), bay);
        console.hrAdmin("admin");
    }

    @Test
    void statusLogsOfAPedestalShowTheErrorHistoryNewestFirst() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        log(baySocket, SocketStatus.AVAILABLE, 0L, now.minusHours(3));
        log(baySocket, SocketStatus.FAULTED, 4L, now.minusHours(1));
        log(lakePedestal.getSockets().get(0), SocketStatus.FAULTED, 4L, now.minusHours(1));

        call("baydesk", PATH + "/findSocketStatusLogs", new FindMonitoringLogsRequest("HR-BAYMON-P001", null, null,
                null, null, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs", hasSize(2)))
                .andExpect(jsonPath("$.logs[0].status").value("FAULTED"))
                .andExpect(jsonPath("$.logs[0].errorFlags").value(4))
                .andExpect(jsonPath("$.logs[0].pedestalCode").value("HR-BAYMON-P001"))
                .andExpect(jsonPath("$.logs[1].status").value("AVAILABLE"));
        call("baydesk", PATH + "/findSocketStatusLogs", new FindMonitoringLogsRequest(null, baySocket.getId(), null,
                null, null, 1))
                .andExpect(jsonPath("$.logs", hasSize(1)));
        call("baydesk", PATH + "/findSocketStatusLogs", new FindMonitoringLogsRequest("HR-LAKEMON-P001", null, null,
                null, null, null))
                .andExpect(status().isNotFound());
        call("baydesk", PATH + "/findSocketStatusLogs", new FindMonitoringLogsRequest(null, null, null, null, null,
                null))
                .andExpect(status().isBadRequest());
        call("baydesk", PATH + "/findSocketStatusLogs", new FindMonitoringLogsRequest("HR-BAYMON-P001", null, null,
                null, null, 5000))
                .andExpect(status().isBadRequest());
    }

    @Test
    void statusReportOfTheMonthCountsTheTimePerStatusInTheScope() throws Exception {
        LocalDateTime start = LocalDateTime.now().withDayOfMonth(1).withHour(1).withMinute(0).withSecond(0)
                .withNano(0);
        log(baySocket, SocketStatus.SUPPLYING, 0L, start);
        log(baySocket, SocketStatus.AVAILABLE, 0L, start.plusHours(2));
        log(baySocket, SocketStatus.AVAILABLE, 0L, start.plusHours(3));

        call("bayservice", PATH + "/getStatusReport", new GetStatusReportRequest(null, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines", hasSize(2)))
                .andExpect(jsonPath("$.lines[?(@.status == 'SUPPLYING_WITHOUT_CONSUMPTION')].timeInSeconds")
                        .value(7200))
                .andExpect(jsonPath("$.lines[0].pedestalCode").value("HR-BAYMON-P001"));
        call("bayservice", PATH + "/getStatusReport", new GetStatusReportRequest("2020-01", null))
                .andExpect(jsonPath("$.lines", hasSize(0)));
        call("bayservice", PATH + "/getStatusReport", new GetStatusReportRequest("July", null))
                .andExpect(status().isBadRequest());
    }

    @Test
    void communicationLogsHeartbeatTimeoutsAndFailedEventsAreListed() throws Exception {
        communicationLogDao.saveAndFlush(new PedestalCommunicationLog(bayPedestal, LocalDateTime.now().minusHours(1),
                PedestalCommunicationLog.LogType.BOOT, "Boot with firmware 2.4"));
        PedestalState silent = new PedestalState(bayPedestal, LocalDateTime.now().minusHours(5));
        silent.setTimeout(true);
        silent.setLastTimeout(LocalDateTime.now().minusHours(4));
        pedestalStateDao.saveAndFlush(silent);
        PedestalState lakeSilent = new PedestalState(lakePedestal, LocalDateTime.now().minusHours(5));
        lakeSilent.setTimeout(true);
        pedestalStateDao.saveAndFlush(lakeSilent);
        jdbcTemplate.update("INSERT INTO EVENT_RETRY (EVENT_ID, EVENT_TYPE, ATTRIBUTES, FAILURE, STATUS, RETRY_COUNT,"
                + " CREATE_DATE) VALUES ('ev-monitoring-1', 'SOCKET_STATE_NOTIFICATION', '{}', 'queue full', 'FAILED',"
                + " 3, now())");

        call("bayservice", PATH + "/findCommunicationLogs", new FindMonitoringLogsRequest("HR-BAYMON-P001", null,
                null, null, null, null))
                .andExpect(jsonPath("$.logs", hasSize(1)))
                .andExpect(jsonPath("$.logs[0].logType").value("BOOT"))
                .andExpect(jsonPath("$.logs[0].message").value("Boot with firmware 2.4"));
        call("bayservice", PATH + "/findHeartbeatTimeouts", new FindMonitoringLogsRequest(null, null, null, null,
                null, null))
                .andExpect(jsonPath("$.timeouts", hasSize(1)))
                .andExpect(jsonPath("$.timeouts[0].pedestalCode").value("HR-BAYMON-P001"))
                .andExpect(jsonPath("$.timeouts[0].campsiteName").value("Bay"));
        call("bayservice", PATH + "/findFailedEvents", new FindMonitoringLogsRequest(null, null, null, null, null,
                null))
                .andExpect(status().isForbidden());
        call("admin", PATH + "/findFailedEvents", new FindMonitoringLogsRequest(null, null, null, null, null, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].eventId").value("ev-monitoring-1"))
                .andExpect(jsonPath("$.events[0].failure").value("queue full"));
    }

    @Test
    void statusRequestIsSentToThePedestalAndARefusalIsAConflict() throws Exception {
        when(pedestalLink.requestStatus(any(), eq(2))).thenReturn(true);

        call("bayservice", PATH + "/requestStatus", new SocketCommandRequest("HR-BAYMON-P001", 2, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.command.command").value("STATUS_REQUEST"))
                .andExpect(jsonPath("$.command.socketNo").value(2));
        call("bayservice", PATH + "/requestStatus", new SocketCommandRequest("HR-BAYMON-P001", null, null))
                .andExpect(status().isConflict());
        call("bayservice", PATH + "/requestStatus", new SocketCommandRequest("HR-BAYMON-P001", 6, null))
                .andExpect(status().isNotFound());
        call("baydesk", PATH + "/requestStatus", new SocketCommandRequest("HR-BAYMON-P001", 2, null))
                .andExpect(status().isForbidden());
    }

    @Test
    void faultOfASocketIsResetOnceAndLogged() throws Exception {
        baySocket.updateStatus(SocketStatus.FAULTED, LocalDateTime.now().minusMinutes(10));
        socketDao.saveAndFlush(baySocket);

        call("bayservice", PATH + "/resetFault", new SocketCommandRequest(null, null, baySocket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.command.socketStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.command.message").value(containsString("repaired on site")));
        call("bayservice", PATH + "/resetFault", new SocketCommandRequest(null, null, baySocket.getId()))
                .andExpect(status().isConflict());
        assertThat(socketStatusLogDao.findLastBySocketId(baySocket.getId()).getStatus())
                .isEqualTo(SocketStatus.AVAILABLE);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM AUDIT WHERE ACTION_TYPE = 'CONSOLE_ACTION' "
                + "AND USERID = 'bayservice' AND ACTION_NAME = 'monitoring/resetFault'", Integer.class)).isEqualTo(2);
    }

    @Test
    void faultMailNeedsALoggedFaultAndARecipient() throws Exception {
        call("bayservice", PATH + "/resendFaultMail", new SocketCommandRequest(null, null, baySocket.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("no logged fault")));
        log(baySocket, SocketStatus.FAULTED, 4L, LocalDateTime.now().minusMinutes(5));
        call("bayservice", PATH + "/resendFaultMail", new SocketCommandRequest(null, null, baySocket.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("Fault mail not sent")));
        call("bayservice", PATH + "/resendFaultMail", new SocketCommandRequest(null, null,
                lakePedestal.getSockets().get(0).getId())).andExpect(status().isNotFound());
    }

    private void log(Socket socket, SocketStatus status, long errorFlags, LocalDateTime at) {
        SocketStatusLog log = new SocketStatusLog(socket, at, at);
        log.setStatus(status);
        log.setErrorFlags(errorFlags);
        log.setMeterKwh(new BigDecimal("10"));
        socketStatusLogDao.saveAndFlush(log);
    }
}
