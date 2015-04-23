package io.pillopl.webservice.console.report;

import io.pillopl.consumption.ConsumptionRecord;
import io.pillopl.consumption.PaymentChannel;
import io.pillopl.consumption.SettlementFlags;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupType;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.REPORT + "/getReport";
    static final LocalDateTime DAY = LocalDateTime.of(2026, 7, 10, 0, 0);
    static final AtomicInteger TRANSACTIONS = new AtomicInteger(690_000);

    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired SupplySessionDao supplySessionDao;
    @Autowired ConsumptionRecordDao consumptionRecordDao;

    Group bay;
    Group lake;
    Socket baySocket;
    Socket lakeSocket;

    @BeforeEach
    void setUp() {
        bay = groupManager.createChildGroup(console.site("HR SITES"), "BAY REPORT CONSOLE", "Bay", GroupType.SITE);
        lake = groupManager.createChildGroup(console.site("HR SITES"), "LAKE REPORT CONSOLE", "Lake", GroupType.SITE);
        Pedestal bayPedestal = infrastructure.pedestal("HR-BAYRPT-P001", bay, 1);
        Pedestal lakePedestal = infrastructure.pedestal("HR-LAKERPT-P001", lake, 1);
        baySocket = bayPedestal.getSockets().get(0);
        lakeSocket = lakePedestal.getSockets().get(0);
        console.user("bayowner", UserroleName.AFFILIATE_OWNER, console.staff("HR SELF SERVICE"), bay);
        console.user("technician", UserroleName.SERVICE_TECHNICIAN, console.staff("HR RECEPTION"), bay);
        console.hrAdmin("admin");
    }

    @Test
    void operatorSeesTheHourAverageOfHisCampsiteOnly() throws Exception {
        record(baySocket, "4", DAY.plusHours(12));
        record(lakeSocket, "40", DAY.plusHours(12));

        call(PATH, request("bayowner", "average per hour of day", null, null, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines", hasSize(24)))
                .andExpect(jsonPath("$.lines[10].period").value("10"))
                .andExpect(jsonPath("$.lines[10].kwh").value(2.0))
                .andExpect(jsonPath("$.lines[10].count").value(1));
    }

    @Test
    void marketAdminNarrowsTheMonthlyConsumptionToOneCampsite() throws Exception {
        record(baySocket, "4", DAY.plusHours(12));
        record(lakeSocket, "40", DAY.plusHours(12));

        call(PATH, new GetReportRequest("admin", "MONTHLY_CONSUMPTION", null, null, lake.getId(), null, 2026, 7,
                null, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportType").value("MONTHLY_CONSUMPTION"))
                .andExpect(jsonPath("$.lines", hasSize(1)))
                .andExpect(jsonPath("$.lines[0].period").value("2026_07"))
                .andExpect(jsonPath("$.lines[0].kwh").value(40.0))
                .andExpect(jsonPath("$.lines[0].hours").value(2.0));
    }

    @Test
    void accumulatedKwhHasALinePerPedestalAndTheTotal() throws Exception {
        record(baySocket, "4", DAY.plusHours(12));
        record(lakeSocket, "40", DAY.plusHours(12));

        call(PATH, new GetReportRequest("admin", "ACCUMULATED_KWH_PER_PEDESTAL", "2026-07-01", "2026-07-31", null,
                null, null, null, "MONTH", List.of("HR-BAYRPT-P001", "HR-LAKERPT-P001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines", hasSize(3)))
                .andExpect(jsonPath("$.lines[2].series").value("ALL"))
                .andExpect(jsonPath("$.lines[2].kwh").value(44.0));
    }

    @Test
    void campsiteOutsideTheCallersScopeIsRefused() throws Exception {
        call(PATH, new GetReportRequest("bayowner", "MONTHLY_CONSUMPTION", null, null, lake.getId(), null, null,
                null, null, null)).andExpect(status().isForbidden());
    }

    @Test
    void reportNeedsTheReportRightAKnownTypeAndAPeriod() throws Exception {
        call(PATH, request("technician", "AVERAGE_PER_DAY_OF_WEEK", null, null, null))
                .andExpect(status().isForbidden());
        call(PATH, request("admin", "TOP_SECRET", null, null, null)).andExpect(status().isBadRequest());
        call(PATH, new GetReportRequest("admin", "AVERAGE_PER_DAY_OF_WEEK", null, "2026-07-10", null, null, null,
                null, null, null)).andExpect(status().isBadRequest());
        call(PATH, new GetReportRequest("admin", "ACCUMULATED_KWH_PER_PEDESTAL", "2026-07-01", "2026-07-31", null,
                null, null, null, "WEEK", List.of("HR-BAYRPT-P001"))).andExpect(status().isBadRequest());
        call(PATH, new GetReportRequest("admin", "MONTHLY_CONSUMPTION", null, null, null, null, 2026, 13, null,
                null)).andExpect(status().isBadRequest());
        call(PATH, new GetReportRequest("admin", "MONTHLY_CONSUMPTION", null, null, null, 987654321L, null, null,
                null, null)).andExpect(status().isNotFound());
    }

    private static GetReportRequest request(String user, String type, Long campsiteGroupId, Integer year,
                                            Integer month) {
        return new GetReportRequest(user, type, "2026-07-10", "2026-07-10", campsiteGroupId, null, year, month,
                null, null);
    }

    private ConsumptionRecord record(Socket socket, String kwh, LocalDateTime plugOut) {
        SupplySession session = new SupplySession();
        session.setSocket(socket);
        session.setTransactionId(TRANSACTIONS.incrementAndGet());
        session.setCreateDate(plugOut.minusHours(2));
        session.setMaxCurrentA(16);
        supplySessionDao.save(session);

        ConsumptionRecord record = new ConsumptionRecord();
        record.setRecordNo("RPT-" + session.getTransactionId());
        record.setSupplySessionId(session.getId());
        record.setSocketId(socket.getId());
        record.setSocketCode(socket.getName());
        record.setPedestalCode(socket.getPedestal().getCode());
        record.setSiteGroupId(socket.getPedestal().getGroup().getId());
        record.setCampsiteCode("BAY");
        record.setPaymentChannel(PaymentChannel.CARD);
        record.setSettlementFlags(new SettlementFlags(true, false, false));
        record.setKwh(new BigDecimal(kwh));
        record.setPlugIn(plugOut.minusHours(2));
        record.setPlugOut(plugOut);
        record.setDurationSeconds(7200L);
        record.setNights(1);
        record.setCreateDate(plugOut);
        return consumptionRecordDao.saveAndFlush(record);
    }
}
