package io.pillopl.webservice.console.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import io.pillopl.consumption.ConsumptionRecord;
import io.pillopl.consumption.PaymentChannel;
import io.pillopl.consumption.SettlementFlags;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupType;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalState;
import io.pillopl.infrastructure.Socket;
import io.pillopl.infrastructure.SocketStatus;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
import io.pillopl.persistence.dao.PedestalStateDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.persistence.dao.SupplySessionDao;
import io.pillopl.supply.SupplySession;
import io.pillopl.user.UserroleName;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.DASHBOARD;
    static final AtomicInteger TRANSACTIONS = new AtomicInteger(860_000);

    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired SocketDao socketDao;
    @Autowired SupplySessionDao supplySessionDao;
    @Autowired ConsumptionRecordDao consumptionRecordDao;
    @Autowired PedestalStateDao pedestalStateDao;

    Group bay;
    Pedestal bayPedestal;
    Socket supplying;
    Socket faulted;
    Socket tripped;

    @BeforeEach
    void setUp() {
        bay = groupManager.createChildGroup(console.site("HR SITES"), "BAY DASHBOARD CONSOLE", "Bay", GroupType.SITE);
        Group lake = groupManager.createChildGroup(console.site("HR SITES"), "LAKE DASHBOARD CONSOLE", "Lake",
                GroupType.SITE);
        bayPedestal = infrastructure.pedestal("HR-BAYDSH-P001", bay, 4);
        Pedestal lakePedestal = infrastructure.pedestal("HR-LAKEDSH-P001", lake, 1);
        LocalDateTime now = LocalDateTime.now();
        supplying = bayPedestal.getSockets().get(0);
        supplying.setMeterKwh(new BigDecimal("112.5"));
        faulted = withStatus(bayPedestal.getSockets().get(1), SocketStatus.FAULTED, now.minusMinutes(20));
        tripped = withStatus(bayPedestal.getSockets().get(2), SocketStatus.BREAKER_TRIPPED, now.minusMinutes(10));
        withStatus(lakePedestal.getSockets().get(0), SocketStatus.FAULTED, now.minusMinutes(5));
        socketDao.saveAndFlush(supplying);

        session(supplying, now.minusHours(2), "100");
        Socket silent = bayPedestal.getSockets().get(3);
        silent.setModDate(now.minusHours(40));
        socketDao.saveAndFlush(silent);
        session(silent, now.minusHours(40), "1");
        record(supplying, "5", now.toLocalDate().atStartOfDay().plusMinutes(1), false);
        record(supplying, "400", now.minusDays(1), true);
        console.user("baydesk", UserroleName.RECEPTION, console.staff("HR RECEPTION"), bay);
        console.hrAdmin("admin");
    }

    @Test
    void keyFiguresCountTheCampsitesOfTheCaller() throws Exception {
        call("baydesk", PATH + "/getDashboard", new GetDashboardRequest(null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dashboard.activeSessions").value(2))
                .andExpect(jsonPath("$.dashboard.kwhToday").value(5 + 12.5 + 0))
                .andExpect(jsonPath("$.dashboard.faultedSockets").value(1))
                .andExpect(jsonPath("$.dashboard.breakerTrippedSockets").value(1))
                .andExpect(jsonPath("$.dashboard.reviewQueue").value(1))
                .andExpect(jsonPath("$.dashboard.staleSessions").value(1))
                .andExpect(jsonPath("$.dashboard.failedEvents").doesNotExist())
                .andExpect(jsonPath("$.dashboard.openAlerts").value(4));
        call("admin", PATH + "/getDashboard", new GetDashboardRequest(bay.getId()))
                .andExpect(jsonPath("$.dashboard.faultedSockets").value(1))
                .andExpect(jsonPath("$.dashboard.failedEvents").isNumber())
                .andExpect(jsonPath("$.dashboard.erpLinesInError").isNumber());
        call("baydesk", PATH + "/getDashboard", new GetDashboardRequest(console.site("HR SITES").getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void alertsAreDerivedFromTheDataAndAcknowledgedOnce() throws Exception {
        PedestalState state = new PedestalState(bayPedestal, LocalDateTime.now().minusHours(30));
        state.setTimeout(true);
        state.setLastTimeout(LocalDateTime.now().minusHours(2));
        pedestalStateDao.saveAndFlush(state);

        String body = call("baydesk", PATH + "/findAlerts", new FindAlertsRequest(null, null, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerts", hasSize(5)))
                .andReturn().getResponse().getContentAsString();
        List<String> types = new ArrayList<>();
        String faultKey = null;
        for (JsonNode alert : objectMapper.readTree(body).path("alerts")) {
            types.add(alert.path("alertType").asText());
            if (alert.path("socketId").asLong() == faulted.getId()) {
                faultKey = alert.path("alertKey").asText();
                assertThat(alert.path("severity").asText()).isEqualTo("ERROR");
                assertThat(alert.path("pedestalCode").asText()).isEqualTo("HR-BAYDSH-P001");
            }
        }
        assertThat(types).containsExactlyInAnyOrder("SOCKET_FAULT", "SOCKET_FAULT", "PEDESTAL_TIMEOUT",
                "STALE_SESSION", "REVIEW_RECORD");
        assertThat(faultKey).startsWith("SOCKET_FAULT:" + faulted.getId() + ":");

        call("baydesk", PATH + "/acknowledgeAlert", new AcknowledgeAlertRequest(faultKey, "electrician called"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alert.acknowledged").value(true))
                .andExpect(jsonPath("$.alert.acknowledgedBy").value("baydesk"))
                .andExpect(jsonPath("$.alert.acknowledgeComment").value("electrician called"));
        call("baydesk", PATH + "/acknowledgeAlert", new AcknowledgeAlertRequest(faultKey, null))
                .andExpect(status().isConflict());
        call("baydesk", PATH + "/acknowledgeAlert", new AcknowledgeAlertRequest("SOCKET_FAULT:1:1", null))
                .andExpect(status().isNotFound());
        call("baydesk", PATH + "/findAlerts", new FindAlertsRequest(null, false, null))
                .andExpect(jsonPath("$.alerts", hasSize(4)));
        call("baydesk", PATH + "/findAlerts", new FindAlertsRequest(null, true, null))
                .andExpect(jsonPath("$.alerts", hasSize(5)));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM AUDIT WHERE ACTION_TYPE = 'CONSOLE_ACTION' "
                + "AND ACTION_NAME = 'dashboard/acknowledgeAlert' AND USERID = 'baydesk'", Integer.class))
                .isEqualTo(3);
    }

    @Test
    void networkWideAlertsNeedTheirRights() throws Exception {
        jdbcTemplate.update("INSERT INTO EVENT_RETRY (EVENT_ID, EVENT_TYPE, ATTRIBUTES, FAILURE, STATUS, RETRY_COUNT,"
                + " CREATE_DATE) VALUES ('ev-dashboard-1', 'SOCKET_STATE_NOTIFICATION', '{}', 'queue full', 'FAILED',"
                + " 3, now())");

        call("baydesk", PATH + "/findAlerts", new FindAlertsRequest(null, null, null))
                .andExpect(jsonPath("$.alerts[?(@.alertType == 'FAILED_EVENT')]", hasSize(0)));
        call("admin", PATH + "/findAlerts", new FindAlertsRequest(bay.getId(), null, null))
                .andExpect(jsonPath("$.alerts[?(@.alertType == 'FAILED_EVENT')]", hasSize(1)));
        call("admin", PATH + "/findAlerts", new FindAlertsRequest(null, null, 0))
                .andExpect(status().isBadRequest());
    }

    private Socket withStatus(Socket socket, SocketStatus status, LocalDateTime at) {
        socket.updateStatus(status, at);
        return socketDao.saveAndFlush(socket);
    }

    private void session(Socket socket, LocalDateTime start, String startMeter) {
        SupplySession session = new SupplySession();
        session.setSocket(socket);
        session.setTransactionId(TRANSACTIONS.incrementAndGet());
        session.setCreateDate(start);
        session.setRemoteDate(start);
        session.setStartMeterKwh(new BigDecimal(startMeter));
        session.setMaxCurrentA(16);
        session.setActive(true);
        supplySessionDao.saveAndFlush(session);
    }

    private void record(Socket socket, String kwh, LocalDateTime plugOut, boolean suspect) {
        SupplySession session = new SupplySession();
        session.setSocket(socket);
        session.setTransactionId(TRANSACTIONS.incrementAndGet());
        session.setCreateDate(plugOut.minusHours(2));
        session.setMaxCurrentA(16);
        supplySessionDao.save(session);
        ConsumptionRecord record = new ConsumptionRecord();
        record.setRecordNo("DSH-" + session.getTransactionId());
        record.setSupplySessionId(session.getId());
        record.setSocketId(socket.getId());
        record.setPedestalCode(socket.getPedestal().getCode());
        record.setSiteGroupId(socket.getPedestal().getGroup().getId());
        record.setPaymentChannel(PaymentChannel.CARD);
        record.setSettlementFlags(new SettlementFlags(true, false, false));
        record.setKwh(new BigDecimal(kwh));
        record.setPlugIn(plugOut.minusHours(2));
        record.setPlugOut(plugOut);
        record.setDurationSeconds(7200L);
        record.setNights(1);
        record.setCreateDate(plugOut);
        if (suspect) {
            record.markSuspect("400 kWh in one night");
        }
        consumptionRecordDao.saveAndFlush(record);
    }
}
