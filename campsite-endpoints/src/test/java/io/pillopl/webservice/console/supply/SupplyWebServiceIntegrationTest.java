package io.pillopl.webservice.console.supply;

import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupType;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.pedestallink.v1.PedestalLinkClientV1;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.persistence.dao.SupplyIncidentDao;
import io.pillopl.persistence.dao.SupplySessionDao;
import io.pillopl.supply.SupplyIncident;
import io.pillopl.supply.SupplySession;
import io.pillopl.user.UserroleName;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SupplyWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.SUPPLY;
    static final AtomicInteger TRANSACTIONS = new AtomicInteger(870_000);

    @MockitoBean PedestalLinkClientV1 pedestalLink;

    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired SupplySessionDao supplySessionDao;
    @Autowired SupplyIncidentDao supplyIncidentDao;
    @Autowired SocketDao socketDao;

    Group bay;
    Socket baySocket;
    Socket lakeSocket;

    @BeforeEach
    void setUp() {
        bay = groupManager.createChildGroup(console.site("HR SITES"), "BAY SUPPLY CONSOLE", "Bay", GroupType.SITE);
        Group lake = groupManager.createChildGroup(console.site("HR SITES"), "LAKE SUPPLY CONSOLE", "Lake",
                GroupType.SITE);
        Pedestal bayPedestal = infrastructure.pedestal("HR-BAYSUP-P001", bay, 2);
        Pedestal lakePedestal = infrastructure.pedestal("HR-LAKESUP-P001", lake, 1);
        baySocket = bayPedestal.getSockets().get(0);
        baySocket.setSocketCode("HRBAY*SUP*1");
        baySocket.setPitchNo("A7");
        baySocket.setMeterKwh(new BigDecimal("112.5"));
        socketDao.saveAndFlush(baySocket);
        lakeSocket = lakePedestal.getSockets().get(0);
        console.user("bayservice", UserroleName.SERVICE_TECHNICIAN, console.staff("HR RECEPTION"), bay);
        console.user("baydesk", UserroleName.RECEPTION, console.staff("HR RECEPTION"), bay);
        console.hrAdmin("admin");
    }

    @Test
    void activeSessionsComeFirstWithTheirConsumptionSoFarAndTheListStaysInTheScope() throws Exception {
        SupplySession active = session(baySocket, LocalDateTime.now().minusHours(3), "100");
        SupplySession closed = session(baySocket, LocalDateTime.now().minusDays(2), "90");
        closed.close(io.pillopl.supply.StopReason.LOCAL, LocalDateTime.now().minusDays(2).plusHours(4));
        session(lakeSocket, LocalDateTime.now().minusHours(1), "5");
        supplySessionDao.flush();

        call("bayservice", PATH + "/findSupplySessions", new FindSupplySessionsRequest(null, null, null, null, null,
                null, null, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.sessions", hasSize(2)))
                .andExpect(jsonPath("$.sessions[0].id").value(active.getId()))
                .andExpect(jsonPath("$.sessions[0].active").value(true))
                .andExpect(jsonPath("$.sessions[0].stale").value(false))
                .andExpect(jsonPath("$.sessions[0].kwh").value(12.5))
                .andExpect(jsonPath("$.sessions[0].pitchNo").value("A7"))
                .andExpect(jsonPath("$.sessions[0].campsiteName").value("Bay"))
                .andExpect(jsonPath("$.sessions[1].stopReason").value("LOCAL"));
        call("bayservice", PATH + "/findSupplySessions", new FindSupplySessionsRequest(null, null, null, true, null,
                null, null, null))
                .andExpect(jsonPath("$.sessions", hasSize(1)));
        call("admin", PATH + "/findSupplySessions", new FindSupplySessionsRequest(null, "HR-LAKESUP-P001", null,
                null, null, null, null, null))
                .andExpect(jsonPath("$.sessions", hasSize(1)));
        call("bayservice", PATH + "/findSupplySessions", new FindSupplySessionsRequest(
                console.site("HR SITES").getId(), null, null, null, null, null, null, null))
                .andExpect(status().isForbidden());
    }

    @Test
    void supplyStopFromTheConsoleGoesToThePedestalAndIsAudited() throws Exception {
        SupplySession active = session(baySocket, LocalDateTime.now().minusHours(3), "100");
        when(pedestalLink.remoteSupplyStop(any(), eq(active.getTransactionId()))).thenReturn(true);

        call("bayservice", PATH + "/stopSupply", new SupplySessionRequest(active.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.command.command").value("SUPPLY_STOP"))
                .andExpect(jsonPath("$.command.accepted").value(true))
                .andExpect(jsonPath("$.command.pedestalCode").value("HR-BAYSUP-P001"));

        List<Map<String, Object>> audit = jdbcTemplate.queryForList("SELECT ACTION_NAME, STATUS FROM AUDIT "
                + "WHERE ACTION_TYPE = 'CONSOLE_ACTION' AND USERID = 'bayservice'");
        assertThat(audit).hasSize(1);
        assertThat(audit.get(0)).containsEntry("ACTION_NAME", "supply/stopSupply").containsEntry("STATUS", "SUCCESS");
    }

    @Test
    void stopAndCloseWithoutASessionAreBadRequests() throws Exception {
        call("bayservice", PATH + "/stopSupply", new SupplySessionRequest(null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("id is missing")));
        call("bayservice", PATH + "/closeStaleSession", new SupplySessionRequest(null))
                .andExpect(status().isBadRequest());
    }

    @Test
    void supplyStopFromTheConsoleIsRefusedWithoutRightOutsideTheScopeAndWhenThePedestalSaysNo() throws Exception {
        SupplySession active = session(baySocket, LocalDateTime.now().minusHours(3), "100");
        SupplySession lake = session(lakeSocket, LocalDateTime.now().minusHours(3), "1");

        call("baydesk", PATH + "/stopSupply", new SupplySessionRequest(active.getId()))
                .andExpect(status().isForbidden());
        call("bayservice", PATH + "/stopSupply", new SupplySessionRequest(lake.getId()))
                .andExpect(status().isNotFound());
        call("bayservice", PATH + "/stopSupply", new SupplySessionRequest(active.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("did not accept")));
    }

    @Test
    void supplyStartFromTheConsoleNeedsACardValidAtTheSocket() throws Exception {
        when(pedestalLink.remoteSupplyStart(any(), anyInt(), any())).thenReturn(true);

        call("bayservice", PATH + "/startSupply", new StartSupplyRequest(baySocket.getId(), "UNKNOWN-CARD"))
                .andExpect(status().isConflict());
        call("bayservice", PATH + "/startSupply", new StartSupplyRequest(baySocket.getId(), null))
                .andExpect(status().isBadRequest());
        call("bayservice", PATH + "/startSupply", new StartSupplyRequest(lakeSocket.getId(), "UNKNOWN-CARD"))
                .andExpect(status().isNotFound());
    }

    @Test
    void silentSessionIsClosedWithTimeoutAndAReportingOneIsNot() throws Exception {
        LocalDateTime longAgo = LocalDateTime.now().minusHours(30);
        SupplySession silent = session(baySocket, longAgo, "100");
        baySocket.setModDate(longAgo);
        socketDao.saveAndFlush(baySocket);
        Socket second = baySocket.getPedestal().getSockets().get(1);
        SupplySession reporting = session(second, longAgo, "3");

        call("bayservice", PATH + "/findSupplySessions", new FindSupplySessionsRequest(null, null, "HRBAY*SUP*1",
                true, null, null, null, null))
                .andExpect(jsonPath("$.sessions[0].stale").value(true));
        call("bayservice", PATH + "/closeStaleSession", new SupplySessionRequest(silent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.session.active").value(false))
                .andExpect(jsonPath("$.session.stopReason").value("TIMEOUT"))
                .andExpect(jsonPath("$.session.kwh").value(12.5))
                .andExpect(jsonPath("$.session.consumptionRecordId").isNumber());
        call("bayservice", PATH + "/closeStaleSession", new SupplySessionRequest(silent.getId()))
                .andExpect(status().isConflict());
        call("bayservice", PATH + "/closeStaleSession", new SupplySessionRequest(reporting.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void incidentsOfTheScopeAreReviewedOnce() throws Exception {
        SupplyIncident bayIncident = incident("HR-BAYSUP-P001", LocalDateTime.now().minusHours(2));
        incident("HR-LAKESUP-P001", LocalDateTime.now().minusHours(1));

        call("bayservice", PATH + "/findSupplyIncidents", new FindSupplyIncidentsRequest(null, null, true, null,
                null, null, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidents", hasSize(1)))
                .andExpect(jsonPath("$.incidents[0].incidentType").value("NO_SESSION"))
                .andExpect(jsonPath("$.incidents[0].campsiteGroupId").value(bay.getId()));
        call("bayservice", PATH + "/reviewSupplyIncident", new ReviewSupplyIncidentRequest(bayIncident.getId(),
                "card was blocked during the stay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incident.reviewedBy").value("bayservice"))
                .andExpect(jsonPath("$.incident.reviewComment").value("card was blocked during the stay"));
        call("bayservice", PATH + "/reviewSupplyIncident", new ReviewSupplyIncidentRequest(bayIncident.getId(),
                null)).andExpect(status().isConflict());
        call("baydesk", PATH + "/reviewSupplyIncident", new ReviewSupplyIncidentRequest(bayIncident.getId(), null))
                .andExpect(status().isForbidden());
        call("bayservice", PATH + "/findSupplyIncidents", new FindSupplyIncidentsRequest(null, null, true, null,
                null, null, null))
                .andExpect(jsonPath("$.incidents", hasSize(0)));
    }

    private SupplySession session(Socket socket, LocalDateTime start, String startMeter) {
        SupplySession session = new SupplySession();
        session.setSocket(socket);
        session.setTransactionId(TRANSACTIONS.incrementAndGet());
        session.setCreateDate(start);
        session.setRemoteDate(start);
        session.setStartMeterKwh(new BigDecimal(startMeter));
        session.setMaxCurrentA(16);
        session.setActive(true);
        return supplySessionDao.saveAndFlush(session);
    }

    private SupplyIncident incident(String pedestalCode, LocalDateTime at) {
        SupplyIncident incident = new SupplyIncident(SupplyIncident.IncidentType.NO_SESSION, pedestalCode,
                "Stop without session", at);
        incident.setTransactionId(4711);
        return supplyIncidentDao.saveAndFlush(incident);
    }
}
