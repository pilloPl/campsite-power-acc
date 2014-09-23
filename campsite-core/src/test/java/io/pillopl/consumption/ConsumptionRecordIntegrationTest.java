package io.pillopl.consumption;

import io.pillopl.card.CardFixture;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
import io.pillopl.persistence.dao.LocationDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.pricing.PricingStatus;
import io.pillopl.supply.StopReason;
import io.pillopl.supply.SupplyControlUnitImpl;
import io.pillopl.supply.SupplyRequest;
import io.pillopl.supply.SupplySession;
import io.pillopl.supply.SupplySessionManager;
import io.pillopl.test.IntegrationTest;
import io.pillopl.value.SocketLinkId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The one consumption record of a supply session on the database: written by the stop and by the stale-session
 * job, priced with the market default tariff, suspect records in the review view, unique per session.
 */
@Transactional
class ConsumptionRecordIntegrationTest extends IntegrationTest {

    @Autowired SupplyControlUnitImpl controlUnit;
    @Autowired SupplySessionManager sessionManager;
    @Autowired ConsumptionRecordDao consumptionRecordDao;
    @Autowired ConsumptionRecordWriteService writeService;
    @Autowired SocketDao socketDao;
    @Autowired LocationDao locationDao;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired CardFixture cards;
    @Autowired JdbcTemplate jdbcTemplate;

    final SocketLinkId socket1 = SocketLinkId.of("HR-REC-P101", 1);
    final LocalDateTime plugIn = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusHours(3);
    Group bay;
    Group club;

    @BeforeEach
    void setUp() {
        bay = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "BAY RECORDS", "Bay");
        club = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"), "RECORD CLUB", "Club");
        groupManager.addAcceptanceAgreement(club, bay);
        Pedestal pedestal = infrastructure.pedestal("HR-REC-P101", bay, 2);
        pedestal.setBillable(true);
        pedestal.getLocation().setCampsiteCode("BAY");
        locationDao.saveAndFlush(pedestal.getLocation());
        cards.card("8201", "HR-8201", cards.guest("Kovac", club), LocalDate.now().minusDays(3),
                LocalDate.now().plusDays(3));
    }

    @Test
    void stopOfThePedestalWritesOnePricedRecord() {
        Integer transactionId = start("100.5", plugIn);

        controlUnit.stopSupply("HR-REC-P101", "8201", SupplyRequest.stop(transactionId, new BigDecimal("112.25"),
                plugIn.plusHours(2), StopReason.UNPLUGGED));

        ConsumptionRecord record = recordOf(transactionId);
        assertThat(record.getRecordNo()).matches("BAY-" + plugIn.plusHours(2).format(
                DateTimeFormatter.BASIC_ISO_DATE) + "-\\d{6,}");
        assertThat(record.getKwh()).isEqualByComparingTo("11.75");
        assertThat(record.getPlugOut()).isEqualTo(plugIn.plusHours(2));
        assertThat(record.getDurationSeconds()).isEqualTo(7200L);
        assertThat(record.getPedestalCode()).isEqualTo("HR-REC-P101");
        assertThat(record.getCity()).isEqualTo("Rovinj");
        assertThat(record.getSiteGroupId()).isEqualTo(bay.getId());
        assertThat(record.getIssuerGroupId()).isEqualTo(club.getId());
        assertThat(record.getGuestName()).isEqualTo("Kovac");
        assertThat(record.getPaymentChannel()).isEqualTo(PaymentChannel.CARD);
        assertThat(record.getSettlementFlags().isBillable()).isTrue();
        assertThat(record.getStopReason()).isEqualTo(StopReason.UNPLUGGED);
        // HR market default: 0.45 EUR per kWh, 11.75 kWh -> 5.2875 -> 5.29
        assertThat(record.getPricingStatus()).isEqualTo(PricingStatus.PRICED);
        assertThat(record.getCost()).isEqualByComparingTo("5.29");
        assertThat(record.getCurrency()).isEqualTo("EUR");
        assertThat(record.getValidationState()).isEqualTo(RecordValidationState.VALID);
        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.NEW);
    }

    @Test
    void sessionClosedByTheTimeoutJobGetsItsRecordFromTheLastSocketReading() {
        LocalDateTime longAgo = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).minusHours(30);
        Integer transactionId = start("100.5", longAgo);
        Socket socket = socketDao.findByPedestalCodeAndSocketNo("HR-REC-P101", 1);
        socket.setMeterKwh(new BigDecimal("131.75"));
        socket.setModDate(longAgo.plusHours(5));
        socketDao.saveAndFlush(socket);

        controlUnit.closeStaleSessions();

        ConsumptionRecord record = recordOf(transactionId);
        assertThat(record.getStopReason()).isEqualTo(StopReason.TIMEOUT);
        assertThat(record.getKwh()).isEqualByComparingTo("31.25");
        assertThat(record.getPlugOut()).isEqualTo(longAgo.plusHours(5));
        assertThat(record.getPricingStatus()).isEqualTo(PricingStatus.PRICED);
    }

    @Test
    void suspectRecordWaitsInTheReviewViewUntilItIsConfirmed() {
        Integer transactionId = start("100", plugIn);
        controlUnit.stopSupply("HR-REC-P101", "8201", SupplyRequest.stop(transactionId, new BigDecimal("520"),
                plugIn.plusHours(2), StopReason.LOCAL));
        ConsumptionRecord record = recordOf(transactionId);

        assertThat(record.getValidationState()).isEqualTo(RecordValidationState.SUSPECT);
        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.REVIEW);
        assertThat(record.getCost()).isNull();
        assertThat(suspectViewIds()).contains(record.getId());

        writeService.confirmRecord(record.getId(), "admin", "checked on site");
        consumptionRecordDao.flush();

        assertThat(record.getValidationState()).isEqualTo(RecordValidationState.CONFIRMED);
        assertThat(record.getPricingStatus()).isEqualTo(PricingStatus.PRICED);
        assertThat(record.getCost()).isEqualByComparingTo("189.00");
        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.NEW);
        assertThat(suspectViewIds()).doesNotContain(record.getId());
    }

    @Test
    void recordNumbersComeFromTheSequenceAndAreUnique() {
        Integer first = start("10", plugIn);
        controlUnit.stopSupply("HR-REC-P101", "8201", SupplyRequest.stop(first, new BigDecimal("11"),
                plugIn.plusHours(1), StopReason.LOCAL));
        Integer second = start("11", plugIn.plusHours(1).plusMinutes(5));
        controlUnit.stopSupply("HR-REC-P101", "8201", SupplyRequest.stop(second, new BigDecimal("12"),
                plugIn.plusHours(2), StopReason.LOCAL));

        assertThat(recordOf(first).getRecordNo()).isNotEqualTo(recordOf(second).getRecordNo());
    }

    @Test
    void aSessionCannotHaveASecondRecord() {
        Integer transactionId = start("10", plugIn);
        controlUnit.stopSupply("HR-REC-P101", "8201", SupplyRequest.stop(transactionId, new BigDecimal("11"),
                plugIn.plusHours(1), StopReason.LOCAL));
        ConsumptionRecord record = recordOf(transactionId);

        ConsumptionRecord copy = new ConsumptionRecord();
        copy.setRecordNo(record.getRecordNo() + "-COPY");
        copy.setSupplySessionId(record.getSupplySessionId());
        copy.setPaymentChannel(PaymentChannel.CARD);
        copy.setPlugOut(record.getPlugOut());
        copy.setCreateDate(record.getCreateDate());

        assertThatThrownBy(() -> consumptionRecordDao.saveAndFlush(copy))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Integer start(String startMeter, LocalDateTime at) {
        return controlUnit.startSupply(socket1, "8201", SupplyRequest.start(new BigDecimal(startMeter), at))
                .getTransactionId();
    }

    private ConsumptionRecord recordOf(Integer transactionId) {
        SupplySession session = sessionManager.findSessionFor("HR-REC-P101", transactionId);
        consumptionRecordDao.flush();
        return consumptionRecordDao.findBySupplySessionId(session.getId());
    }

    private List<Long> suspectViewIds() {
        return jdbcTemplate.queryForList("SELECT ID FROM CONSUMPTION_RECORD_SUSPECT", Long.class);
    }
}
