package io.pillopl.webservice.console.report;

import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupType;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Socket;
import io.pillopl.infrastructure.SocketStatus;
import io.pillopl.monitoring.SocketStatusLog;
import io.pillopl.persistence.dao.SocketStatusLogDao;
import io.pillopl.user.UserroleName;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SocketUtilizationReportIntegrationTest extends WebServiceIntegrationTest {

    static final LocalDateTime MONDAY = LocalDateTime.of(2026, 7, 13, 0, 0);

    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired SocketStatusLogDao socketStatusLogDao;

    Socket baySocket;
    Socket lakeSocket;

    @BeforeEach
    void setUp() {
        Group bay = groupManager.createChildGroup(console.site("HR SITES"), "BAY UTILIZATION CONSOLE", "Bay",
                GroupType.SITE);
        Group lake = groupManager.createChildGroup(console.site("HR SITES"), "LAKE UTILIZATION CONSOLE", "Lake",
                GroupType.SITE);
        baySocket = infrastructure.pedestal("HR-BAYUTL-P001", bay, 1).getSockets().get(0);
        lakeSocket = infrastructure.pedestal("HR-LAKEUTL-P001", lake, 1).getSockets().get(0);
        console.user("bayowner", UserroleName.AFFILIATE_OWNER, console.staff("HR SELF SERVICE"), bay);
        console.user("technician", UserroleName.SERVICE_TECHNICIAN, console.staff("HR RECEPTION"), bay);
    }

    @Test
    void heatmapShowsSupplyingHoursAndMonitoredMinutesPerDayAndHourOfTheCampsite() throws Exception {
        log(baySocket, SocketStatus.SUPPLYING, "10", MONDAY.plusHours(8));
        log(baySocket, SocketStatus.SUPPLYING, "11", MONDAY.plusHours(8).plusMinutes(30));
        log(baySocket, SocketStatus.AVAILABLE, "11", MONDAY.plusHours(9));
        log(lakeSocket, SocketStatus.SUPPLYING, "1", MONDAY.plusHours(8));
        log(lakeSocket, SocketStatus.SUPPLYING, "5", MONDAY.plusHours(10));

        call("bayowner", WebServicePaths.REPORT + "/refreshUtilization", new RefreshUtilizationRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshed").value(true));
        call("bayowner", WebServicePaths.REPORT + "/getReport", new GetReportRequest("SOCKET_UTILIZATION",
                "2026-07-13", "2026-07-13", null, null, null, null, null, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines", hasSize(2)))
                .andExpect(jsonPath("$.lines[0].period").value("MONDAY"))
                .andExpect(jsonPath("$.lines[0].series").value("08"))
                .andExpect(jsonPath("$.lines[0].count").value(60))
                .andExpect(jsonPath("$.lines[0].hours").value(0.5))
                .andExpect(jsonPath("$.lines[1].series").value("09"))
                .andExpect(jsonPath("$.lines[1].count").value(0));
        call("bayowner", WebServicePaths.REPORT + "/getReport", new GetReportRequest("SOCKET_UTILIZATION", null,
                null, null, null, null, null, null, null)).andExpect(status().isBadRequest());
        call("technician", WebServicePaths.REPORT + "/refreshUtilization", new RefreshUtilizationRequest())
                .andExpect(status().isForbidden());
    }

    private void log(Socket socket, SocketStatus status, String meter, LocalDateTime at) {
        SocketStatusLog log = new SocketStatusLog(socket, at, at);
        log.setStatus(status);
        log.setMeterKwh(new BigDecimal(meter));
        socketStatusLogDao.saveAndFlush(log);
    }
}
