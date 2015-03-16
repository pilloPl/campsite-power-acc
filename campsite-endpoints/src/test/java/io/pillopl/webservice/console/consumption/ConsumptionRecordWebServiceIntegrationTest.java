package io.pillopl.webservice.console.consumption;

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
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConsumptionRecordWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.CONSUMPTION_RECORD;
    static final LocalDateTime DAY = LocalDateTime.of(2026, 7, 10, 0, 0);
    static final AtomicInteger TRANSACTIONS = new AtomicInteger(890_000);

    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired SupplySessionDao supplySessionDao;
    @Autowired ConsumptionRecordDao consumptionRecordDao;

    Socket baySocket;
    Socket lakeSocket;
    Group club;

    @BeforeEach
    void setUp() {
        Group bay = groupManager.createChildGroup(console.site("HR SITES"), "BAY RECORD CONSOLE", "Bay",
                GroupType.SITE);
        Group lake = groupManager.createChildGroup(console.site("HR SITES"), "LAKE RECORD CONSOLE", "Lake",
                GroupType.SITE);
        club = groupManager.createChildGroup(console.issuer("HR ISSUERS"), "RECORD CONSOLE CLUB", "Club",
                GroupType.ISSUER);
        Pedestal bayPedestal = infrastructure.pedestal("HR-BAYREC-P001", bay, 1);
        Pedestal lakePedestal = infrastructure.pedestal("HR-LAKEREC-P001", lake, 1);
        baySocket = bayPedestal.getSockets().get(0);
        lakeSocket = lakePedestal.getSockets().get(0);
        console.user("bayowner", UserroleName.AFFILIATE_OWNER, console.staff("HR SELF SERVICE"), bay);
        console.user("technician", UserroleName.SERVICE_TECHNICIAN, console.staff("HR RECEPTION"), bay);
        console.hrAdmin("admin");
    }

    @Test
    void operatorSeesOnlyTheRecordsOfHisCampsite() throws Exception {
        record(baySocket, "12.5", DAY.plusHours(10));
        record(lakeSocket, "8", DAY.plusHours(11));

        call(PATH + "/findConsumptionRecords", new FindConsumptionRecordsRequest("bayowner", "2026-07-10",
                "2026-07-10", null, null, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumptionRecords", hasSize(1)))
                .andExpect(jsonPath("$.consumptionRecords[0].pedestalCode").value("HR-BAYREC-P001"))
                .andExpect(jsonPath("$.consumptionRecords[0].issuerTenant").value("Club"))
                .andExpect(jsonPath("$.consumptionRecords[0].paymentChannel").value("CARD"));
    }

    @Test
    void marketAdminFiltersTheReviewQueueAndPages() throws Exception {
        record(baySocket, "12.5", DAY.plusHours(10));
        record(lakeSocket, "8", DAY.plusHours(11));
        ConsumptionRecord suspect = record(lakeSocket, "400", DAY.plusHours(12));
        suspect.markSuspect("400 kWh in one night");
        consumptionRecordDao.flush();

        call(PATH + "/findConsumptionRecords", new FindConsumptionRecordsRequest("admin", "2026-07-10",
                "2026-07-10", null, null, null))
                .andExpect(jsonPath("$.consumptionRecords", hasSize(3)));
        call(PATH + "/findConsumptionRecords", new FindConsumptionRecordsRequest("admin", "2026-07-10",
                "2026-07-10", "suspect", null, null))
                .andExpect(jsonPath("$.consumptionRecords", hasSize(1)))
                .andExpect(jsonPath("$.consumptionRecords[0].exportStatus").value("REVIEW"));
        call(PATH + "/findConsumptionRecords", new FindConsumptionRecordsRequest("admin", "2026-07-10",
                "2026-07-10", null, 1, 2))
                .andExpect(jsonPath("$.consumptionRecords", hasSize(1)))
                .andExpect(jsonPath("$.consumptionRecords[0].kwh").value(12.5));
    }

    @Test
    void confirmedSuspectRecordIsPricedAndExported() throws Exception {
        ConsumptionRecord suspect = record(baySocket, "400", DAY.plusHours(12));
        suspect.markSuspect("400 kWh in one night");
        consumptionRecordDao.flush();

        call(PATH + "/reviewConsumptionRecord", new ReviewConsumptionRecordRequest("admin", suspect.getId(), true,
                "heater of the sanitary block"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumptionRecord.validationState").value("CONFIRMED"))
                .andExpect(jsonPath("$.consumptionRecord.pricingStatus").value("PRICED"))
                .andExpect(jsonPath("$.consumptionRecord.cost").value(180.0))
                .andExpect(jsonPath("$.consumptionRecord.exportStatus").value("NEW"))
                .andExpect(jsonPath("$.consumptionRecord.reviewedBy").value("admin"));
    }

    @Test
    void rejectedRecordIsIgnored() throws Exception {
        ConsumptionRecord suspect = record(baySocket, "-3", DAY.plusHours(12));
        suspect.markSuspect("Negative consumption");
        consumptionRecordDao.flush();

        call(PATH + "/reviewConsumptionRecord", new ReviewConsumptionRecordRequest("admin", suspect.getId(), false,
                "meter replaced"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumptionRecord.validationState").value("REJECTED"))
                .andExpect(jsonPath("$.consumptionRecord.exportStatus").value("IGNORED"));
    }

    @Test
    void reviewNeedsTheRightAndARecordWaitingForReview() throws Exception {
        ConsumptionRecord valid = record(baySocket, "12.5", DAY.plusHours(10));

        call(PATH + "/reviewConsumptionRecord", new ReviewConsumptionRecordRequest("bayowner", valid.getId(), true,
                null)).andExpect(status().isForbidden());
        call(PATH + "/reviewConsumptionRecord", new ReviewConsumptionRecordRequest("admin", valid.getId(), true,
                null)).andExpect(status().isConflict());
        call(PATH + "/reviewConsumptionRecord", new ReviewConsumptionRecordRequest("admin", 987654321L, true,
                null)).andExpect(status().isNotFound());
    }

    @Test
    void listNeedsTheReportRightAndAPeriod() throws Exception {
        call(PATH + "/findConsumptionRecords", new FindConsumptionRecordsRequest("technician", "2026-07-10",
                "2026-07-10", null, null, null)).andExpect(status().isForbidden());
        call(PATH + "/findConsumptionRecords", new FindConsumptionRecordsRequest("admin", null, "2026-07-10",
                null, null, null)).andExpect(status().isBadRequest());
        call(PATH + "/findConsumptionRecords", new FindConsumptionRecordsRequest("admin", "2026-07-10",
                "2026-07-10", "LOST", null, null)).andExpect(status().isBadRequest());
    }

    private ConsumptionRecord record(Socket socket, String kwh, LocalDateTime plugOut) {
        SupplySession session = new SupplySession();
        session.setSocket(socket);
        session.setTransactionId(TRANSACTIONS.incrementAndGet());
        session.setCreateDate(plugOut.minusHours(2));
        session.setMaxCurrentA(16);
        supplySessionDao.save(session);

        ConsumptionRecord record = new ConsumptionRecord();
        record.setRecordNo("WS-" + session.getTransactionId());
        record.setSupplySessionId(session.getId());
        record.setSocketId(socket.getId());
        record.setPedestalCode(socket.getPedestal().getCode());
        record.setSiteGroupId(socket.getPedestal().getGroup().getId());
        record.setIssuerGroupId(club.getId());
        record.setIssuerTenant("Club");
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
