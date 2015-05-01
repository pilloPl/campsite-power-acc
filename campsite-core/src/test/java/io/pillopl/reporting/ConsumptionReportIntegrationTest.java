package io.pillopl.reporting;

import io.pillopl.consumption.ConsumptionRecord;
import io.pillopl.consumption.PaymentChannel;
import io.pillopl.consumption.SettlementFlags;
import io.pillopl.dto.report.ReportLineDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
import io.pillopl.persistence.dao.ConsumptionReportDao;
import io.pillopl.persistence.dao.ConsumptionReportDao.ReportRecord;
import io.pillopl.persistence.dao.PowerSectionDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.persistence.dao.SupplyIncidentDao;
import io.pillopl.persistence.dao.SupplySessionDao;
import io.pillopl.power.PowerSection;
import io.pillopl.supply.SupplyIncident;
import io.pillopl.supply.SupplySession;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The report SQL on the database: scope of the reader (SITE subtree, ISSUER subtree), power section, rejected records
 * left out, date functions in GROUP BY with bound year and month, kWh per stay night and per pedestal.
 */
@Transactional
class ConsumptionReportIntegrationTest extends IntegrationTest {

    static final LocalDateTime JULY = LocalDateTime.of(2026, 7, 10, 0, 0);
    static final LocalDateTime AUGUST = LocalDateTime.of(2026, 8, 10, 0, 0);
    static final LocalDateTime OCTOBER = LocalDateTime.of(2026, 10, 10, 0, 0);
    static final AtomicInteger TRANSACTIONS = new AtomicInteger(790_000);

    @Autowired ConsumptionReportDao reportDao;
    @Autowired CampsiteReportService campsiteReportService;
    @Autowired ReportingService reportingService;
    @Autowired GroupManager groupManager;
    @Autowired GroupService groupService;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired SupplySessionDao supplySessionDao;
    @Autowired ConsumptionRecordDao consumptionRecordDao;
    @Autowired SocketDao socketDao;
    @Autowired PowerSectionDao powerSectionDao;
    @Autowired SupplyIncidentDao supplyIncidentDao;

    Group operator;
    Group bay;
    Group lake;
    Group club;
    Socket baySocket;
    Socket baySectionSocket;
    Socket lakeSocket;
    PowerSection northSection;

    @BeforeEach
    void setUp() {
        operator = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "REPORT OPERATOR",
                "Operator");
        bay = groupManager.createChildGroup(operator, "BAY REPORTS", "Bay");
        lake = groupManager.createChildGroup(operator, "LAKE REPORTS", "Lake");
        club = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"), "REPORT CLUB", "Club");
        Pedestal bayPedestal = infrastructure.pedestal("HR-BAYREP-P001", bay, 2);
        Pedestal lakePedestal = infrastructure.pedestal("HR-LAKEREP-P001", lake, 1);
        baySocket = bayPedestal.getSockets().get(0);
        baySectionSocket = bayPedestal.getSockets().get(1);
        lakeSocket = lakePedestal.getSockets().get(0);
        northSection = powerSectionDao.saveAndFlush(new PowerSection("REPORT NORTH", new BigDecimal("63"),
                "nominalCurrentSum"));
        baySectionSocket.setPowerSection(northSection);
        socketDao.saveAndFlush(baySectionSocket);
    }

    @Test
    void reportRecordsAreTheRecordsOfTheReadersSiteSubtreeWithoutRejectedOnes() {
        record(baySocket, "10", JULY.plusHours(10), "BAY", 1, null);
        record(lakeSocket, "20", JULY.plusHours(11), "LAKE", 1, null);
        ConsumptionRecord rejected = record(baySocket, "999", JULY.plusHours(12), "BAY", 1, null);
        rejected.markSuspect("too much");
        rejected.reject("checker", JULY.plusDays(1), "meter fault");
        consumptionRecordDao.flush();

        assertThat(reportDao.findReportRecords(JULY, JULY.plusDays(1), operator.getId(), null, null))
                .extracting(ReportRecord::socketCode).containsExactly(baySocket.getName(), lakeSocket.getName());
        assertThat(reportDao.findReportRecords(JULY, JULY.plusDays(1), bay.getId(), null, null))
                .extracting(ReportRecord::kwh).containsExactly(new BigDecimal("10.0000"));
    }

    @Test
    void issuerSideSeesTheRecordsOfItsGuestsAndAMissingGroupMakesItsSideEmpty() {
        record(baySocket, "10", JULY.plusHours(10), "BAY", 1, club);
        record(lakeSocket, "20", JULY.plusHours(11), "LAKE", 1, null);

        assertThat(reportDao.findReportRecords(JULY, JULY.plusDays(1), null, club.getId(), null))
                .extracting(ReportRecord::socketCode).containsExactly(baySocket.getName());
        assertThat(reportDao.findReportRecords(JULY, JULY.plusDays(1), null, null, null)).isEmpty();
        // a record seen from both sides comes once
        assertThat(reportDao.findReportRecords(JULY, JULY.plusDays(1), bay.getId(), club.getId(), null)).hasSize(1);
    }

    @Test
    void powerSectionNarrowsTheReportToTheSocketsOfItsSwitchboard() {
        record(baySocket, "10", JULY.plusHours(10), "BAY", 1, null);
        record(baySectionSocket, "4", JULY.plusHours(11), "BAY", 1, null);

        assertThat(reportDao.findReportRecords(JULY, JULY.plusDays(1), operator.getId(), null,
                northSection.getId())).extracting(ReportRecord::socketCode)
                .containsExactly(baySectionSocket.getName());
    }

    @Test
    void monthlyConsumptionIsGroupedByYearAndMonthOfPlugOut() {
        record(baySocket, "10", JULY.plusHours(10), "BAY", 1, null);
        record(baySocket, "5.5", JULY.plusDays(3), "BAY", 1, null);
        record(lakeSocket, "20", AUGUST, "LAKE", 1, null);
        record(lakeSocket, "1", OCTOBER, "LAKE", 1, null);

        assertThat(reportDao.getMonthlyConsumptions(operator.getId(), null, null, null, null))
                .extracting(ReportLineDTO::period, ReportLineDTO::kwh, ReportLineDTO::count, ReportLineDTO::hours)
                .containsExactly(
                        tuple("2026_07", new BigDecimal("15.5000"), 2L, new BigDecimal("4.00")),
                        tuple("2026_08", new BigDecimal("20.0000"), 1L, new BigDecimal("2.00")),
                        tuple("2026_10", new BigDecimal("1.0000"), 1L, new BigDecimal("2.00")));
        assertThat(reportDao.getMonthlyConsumptions(operator.getId(), null, null, 2026, 8))
                .extracting(ReportLineDTO::period).containsExactly("2026_08");
        assertThat(reportDao.getMonthlyConsumptions(operator.getId(), null, null, 2025, null)).isEmpty();
    }

    @Test
    void quarterlyConsumptionIsGroupedByYearAndQuarterOfPlugOut() {
        record(baySocket, "10", JULY.plusHours(10), "BAY", 1, null);
        record(lakeSocket, "20", AUGUST, "LAKE", 1, null);
        record(lakeSocket, "1", OCTOBER, "LAKE", 1, null);

        assertThat(reportDao.getQuarterlyConsumptions(operator.getId(), null, null, 2026))
                .extracting(ReportLineDTO::period, ReportLineDTO::kwh, ReportLineDTO::count)
                .containsExactly(
                        tuple("2026_Q3", new BigDecimal("30.0000"), 2L),
                        tuple("2026_Q4", new BigDecimal("1.0000"), 1L));
    }

    @Test
    void kwhPerStayNightIsSummedPerCampsite() {
        record(baySocket, "30", JULY.plusHours(10), "BAY", 2, null);
        record(baySocket, "10", JULY.plusHours(14), "BAY", 1, null);
        record(lakeSocket, "8", JULY.plusHours(11), "LAKE", 0, null);

        assertThat(campsiteReportService.getConsumptionPerStayNight(JULY, JULY.plusDays(1), operator.getId(), null,
                null)).extracting(ReportLineDTO::period, ReportLineDTO::kwh, ReportLineDTO::count)
                .containsExactly(tuple("BAY", new BigDecimal("13.333"), 3L));
    }

    @Test
    void topPitchesAreThePitchesWithTheMostKwhFirst() {
        record(baySocket, "10", JULY.plusHours(10), "BAY", 1, null).setPitchNo("A1");
        record(baySocket, "5", JULY.plusHours(13), "BAY", 1, null).setPitchNo("A1");
        record(lakeSocket, "20", JULY.plusHours(11), "LAKE", 1, null).setPitchNo("L7");
        record(lakeSocket, "1", JULY.plusHours(12), "LAKE", 1, null).setPitchNo("L8");
        record(lakeSocket, "99", JULY.plusHours(14), "LAKE", 1, null);
        consumptionRecordDao.flush();

        assertThat(reportDao.getTopPitches(JULY, JULY.plusDays(1), operator.getId(), null, null, 2))
                .extracting(ReportLineDTO::period, ReportLineDTO::series, ReportLineDTO::kwh, ReportLineDTO::count)
                .containsExactly(
                        tuple("L7", "LAKE", new BigDecimal("20.0000"), 1L),
                        tuple("A1", "BAY", new BigDecimal("15.0000"), 2L));
    }

    @Test
    void accumulatedKwhPerPedestalSumsTheMonthsOfTheSelectedPedestals() {
        record(baySocket, "10", JULY.plusHours(10), "BAY", 1, null);
        record(baySectionSocket, "2.5", JULY.plusHours(12), "BAY", 1, null);
        record(lakeSocket, "20", AUGUST, "LAKE", 1, null);

        List<ReportLineDTO> lines = reportingService.fetchAccumulatedKwhPerPedestal(
                ReportingService.TimeResolution.MONTH, JULY, AUGUST, List.of("HR-BAYREP-P001", "HR-LAKEREP-P001"),
                operator.getId(), null);

        assertThat(lines).extracting(ReportLineDTO::period, ReportLineDTO::series, ReportLineDTO::kwh,
                ReportLineDTO::count).containsExactly(
                tuple("2026_07", "HR-BAYREP-P001", new BigDecimal("12.500"), 2L),
                tuple("2026_07", "HR-LAKEREP-P001", new BigDecimal("0.000"), 0L),
                tuple("2026_07", ReportLineDTO.ALL_SERIES, new BigDecimal("12.500"), 2L),
                tuple("2026_08", "HR-BAYREP-P001", new BigDecimal("0.000"), 0L),
                tuple("2026_08", "HR-LAKEREP-P001", new BigDecimal("20.000"), 1L),
                tuple("2026_08", ReportLineDTO.ALL_SERIES, new BigDecimal("20.000"), 1L));
    }

    @Test
    void averagePerHourReadsTheRecordsOfTheDatabase() {
        record(baySocket, "4", JULY.plusHours(12), "BAY", 1, null);

        List<ReportLineDTO> lines = campsiteReportService.getAverageConsumptionPerHourOfDay(JULY, JULY.plusDays(1),
                bay.getId(), null, null);

        assertThat(lines.get(10).kwh()).isEqualByComparingTo("2");
        assertThat(lines.get(11).kwh()).isEqualByComparingTo("2");
    }

    @Test
    void incidentsOfThePedestalsOfTheCampsiteAreCounted() {
        incident("HR-BAYREP-P001", JULY.plusHours(1));
        incident("HR-BAYREP-P001", JULY.plusHours(2));
        incident("HR-LAKEREP-P001", JULY.plusHours(3));

        assertThat(campsiteReportService.getSupplyIncidentsPerPedestal(JULY, JULY.plusDays(1), bay.getId()))
                .extracting(ReportLineDTO::period, ReportLineDTO::series, ReportLineDTO::count)
                .containsExactly(tuple("NO_SESSION", "HR-BAYREP-P001", 2L));
    }

    private ConsumptionRecord record(Socket socket, String kwh, LocalDateTime plugOut, String campsiteCode,
                                     int nights, Group issuer) {
        SupplySession session = new SupplySession();
        session.setSocket(socket);
        session.setTransactionId(TRANSACTIONS.incrementAndGet());
        session.setCreateDate(plugOut.minusHours(2));
        session.setMaxCurrentA(16);
        supplySessionDao.save(session);

        ConsumptionRecord record = new ConsumptionRecord();
        record.setRecordNo("REP-" + session.getTransactionId());
        record.setSupplySessionId(session.getId());
        record.setSocketId(socket.getId());
        record.setSocketCode(socket.getName());
        record.setPedestalCode(socket.getPedestal().getCode());
        record.setSiteGroupId(socket.getPedestal().getGroup().getId());
        record.setIssuerGroupId(issuer != null ? issuer.getId() : null);
        record.setCampsiteCode(campsiteCode);
        record.setPaymentChannel(PaymentChannel.CARD);
        record.setSettlementFlags(new SettlementFlags(true, false, false));
        record.setKwh(new BigDecimal(kwh));
        record.setPlugIn(plugOut.minusHours(2));
        record.setPlugOut(plugOut);
        record.setDurationSeconds(7200L);
        record.setNights(nights);
        record.setCreateDate(plugOut);
        return consumptionRecordDao.saveAndFlush(record);
    }

    private void incident(String pedestalCode, LocalDateTime at) {
        SupplyIncident incident = new SupplyIncident();
        incident.setIncidentType(SupplyIncident.IncidentType.NO_SESSION);
        incident.setPedestalCode(pedestalCode);
        incident.setMessage("No session for the stop");
        incident.setCreateDate(at);
        supplyIncidentDao.saveAndFlush(incident);
    }
}
