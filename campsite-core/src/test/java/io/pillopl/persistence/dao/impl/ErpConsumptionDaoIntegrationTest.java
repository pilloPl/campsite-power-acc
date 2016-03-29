package io.pillopl.persistence.dao.impl;

import io.pillopl.consumption.ConsumptionRecord;
import io.pillopl.consumption.ExportStatus;
import io.pillopl.consumption.PaymentChannel;
import io.pillopl.consumption.SettlementFlags;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.guest.BillingAccount;
import io.pillopl.guest.Guest;
import io.pillopl.guest.GuestType;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.persistence.dao.BillingAccountDao;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
import io.pillopl.persistence.dao.ConsumptionRecordDaoCustom.ErpConsumption;
import io.pillopl.persistence.dao.GuestDao;
import io.pillopl.persistence.dao.SupplySessionDao;
import io.pillopl.pricing.PricingStatus;
import io.pillopl.supply.SupplySession;
import io.pillopl.test.IntegrationTest;
import jakarta.persistence.EntityManager;
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
 * The ERP billing SQL on CONSUMPTION_RECORD: the monthly consumption of the network's billing accounts grouped with
 * TO_CHAR in GROUP BY, only what the network bills, and the export status of the billed records.
 */
@Transactional
class ErpConsumptionDaoIntegrationTest extends IntegrationTest {

    static final LocalDateTime JUNE_END = LocalDateTime.of(2026, 6, 30, 22, 0);
    static final LocalDateTime JULY = LocalDateTime.of(2026, 7, 10, 12, 0);
    static final LocalDateTime AUGUST = LocalDateTime.of(2026, 8, 1, 0, 0);
    static final String RESIDENT = "400066660001";
    static final String TRANSIT = "400066660002";
    static final AtomicInteger TRANSACTIONS = new AtomicInteger(660_000);

    @Autowired ConsumptionRecordDao consumptionRecordDao;
    @Autowired SupplySessionDao supplySessionDao;
    @Autowired GuestDao guestDao;
    @Autowired BillingAccountDao billingAccountDao;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired EntityManager entityManager;

    Pedestal pedestal;

    @BeforeEach
    void setUp() {
        Group bay = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "BAY ERP SQL", "Bay");
        Group club = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"), "ERP SQL CLUB",
                "Club");
        pedestal = infrastructure.pedestal("HR-ERPSQL-P001", bay, 1);
        account(club, "Resident", RESIDENT);
        account(club, "Transit", TRANSIT);
    }

    @Test
    void consumptionIsGroupedPerAccountTariffAndMonthOfPlugOut() {
        record(RESIDENT, "10", "3.80", JUNE_END, 4L);
        record(RESIDENT, "20", "7.60", JULY, 4L);
        record(RESIDENT, "5", "1.90", JULY.plusDays(2), 4L);
        record(RESIDENT, "2", "0.90", JULY, 1L);
        record(TRANSIT, "8", "3.60", JULY, 1L);

        assertThat(consumptionRecordDao.findErpConsumptionPerMonth(JUNE_END.minusMonths(1), AUGUST))
                .extracting(ErpConsumption::billingAccountNo, ErpConsumption::tariffId, ErpConsumption::billed,
                        ErpConsumption::supplyPeriod, ErpConsumption::kwh, ErpConsumption::cost,
                        ErpConsumption::records)
                .containsExactly(
                        tuple(RESIDENT, 4L, false, "2026_06", new BigDecimal("10.0000"), new BigDecimal("3.80"), 1L),
                        tuple(RESIDENT, 1L, false, "2026_07", new BigDecimal("2.0000"), new BigDecimal("0.90"), 1L),
                        tuple(RESIDENT, 4L, false, "2026_07", new BigDecimal("25.0000"), new BigDecimal("9.50"), 2L),
                        tuple(TRANSIT, 1L, false, "2026_07", new BigDecimal("8.0000"), new BigDecimal("3.60"), 1L));
    }

    @Test
    void onlyPricedCardSuppliesTheNetworkBillsOnItsOwnAccountsAreBilled() {
        record(TRANSIT, "1", "0.45", JULY, 1L);
        record("400066669999", "1", "0.45", JULY, 1L); // issuer's own member number, not a network account
        record(TRANSIT, "1", "0.45", JULY, 1L).setExportStatus(ExportStatus.REVIEW);
        ConsumptionRecord rejected = record(TRANSIT, "1", "0.45", JULY, 1L);
        rejected.markSuspect("too much");
        rejected.reject("checker", JULY, "meter fault");
        rejected.setExportStatus(ExportStatus.NEW);
        record(TRANSIT, "1", "0.45", JULY, 1L).setSettlementFlags(new SettlementFlags(true, true, false));
        record(TRANSIT, "1", "0.00", JULY, 1L).setSettlementFlags(new SettlementFlags(false, false, false));
        ConsumptionRecord failed = record(TRANSIT, "1", null, JULY, 1L);
        failed.setPricingStatus(PricingStatus.PRICING_FAILED);
        record(TRANSIT, "1", "0.45", AUGUST, 1L); // the period ends before August
        consumptionRecordDao.flush();

        assertThat(consumptionRecordDao.findErpConsumptionPerMonth(JULY.minusMonths(1), AUGUST))
                .extracting(ErpConsumption::billingAccountNo, ErpConsumption::records)
                .containsExactly(tuple(TRANSIT, 1L));
    }

    @Test
    void billedRecordsCarryTheirPeriodAndFollowTheirErpLines() {
        ConsumptionRecord july = record(RESIDENT, "20", "7.60", JULY, 4L);
        ConsumptionRecord transit = record(TRANSIT, "8", "3.60", JULY, 1L);
        ConsumptionRecord inReview = record(RESIDENT, "3", "1.14", JULY, 4L);
        inReview.setExportStatus(ExportStatus.REVIEW);
        consumptionRecordDao.flush();

        record(RESIDENT, "4", "1.80", JULY, 1L); // another tariff of the account, not on the lines
        assertThat(consumptionRecordDao.markRecordsForErpBilling(List.of(billedRow(RESIDENT, 4L, "2026_07")),
                JULY.minusMonths(1), AUGUST, "2026_07")).isEqualTo(1);
        assertThat(consumptionRecordDao.markRecordsForErpBilling(List.of(), JULY.minusMonths(1), AUGUST,
                "2026_07")).isZero();

        assertThat(consumptionRecordDao.findErpConsumptionPerMonth(JULY.minusMonths(1), AUGUST))
                .extracting(ErpConsumption::billingAccountNo, ErpConsumption::billed, ErpConsumption::kwh)
                .containsExactly(tuple(RESIDENT, false, new BigDecimal("4.0000")),
                        tuple(RESIDENT, true, new BigDecimal("20.0000")),
                        tuple(TRANSIT, false, new BigDecimal("8.0000")));

        assertThat(consumptionRecordDao.updateErpBilledRecords("2026_07", RESIDENT, List.of(ExportStatus.WORK),
                ExportStatus.TRANSFERRED)).isEqualTo(1);
        assertThat(consumptionRecordDao.updateErpBilledRecords("2026_07", null, List.of(ExportStatus.WORK),
                ExportStatus.TRANSFERRED)).isZero();
        entityManager.clear();
        assertThat(consumptionRecordDao.findById(july.getId()).orElseThrow().getExportStatus())
                .isEqualTo(ExportStatus.TRANSFERRED);
        assertThat(consumptionRecordDao.findById(transit.getId()).orElseThrow().getExportStatus())
                .isEqualTo(ExportStatus.NEW);
        assertThat(consumptionRecordDao.findById(inReview.getId()).orElseThrow().getExportStatus())
                .isEqualTo(ExportStatus.REVIEW);
    }

    @Test
    void nullTariffRowMarksOnlyRecordsWithoutTariff() {
        ConsumptionRecord imported = record(TRANSIT, "6", "2.70", JULY, null);
        ConsumptionRecord priced = record(TRANSIT, "8", "3.60", JULY, 1L);

        assertThat(consumptionRecordDao.markRecordsForErpBilling(List.of(billedRow(TRANSIT, null, "2026_07")),
                JULY.minusMonths(1), AUGUST, "2026_07")).isEqualTo(1);
        entityManager.clear();
        assertThat(consumptionRecordDao.findById(imported.getId()).orElseThrow().getExportStatus())
                .isEqualTo(ExportStatus.WORK);
        assertThat(consumptionRecordDao.findById(priced.getId()).orElseThrow().getExportStatus())
                .isEqualTo(ExportStatus.NEW);
    }

    private static ErpConsumption billedRow(String accountNo, Long tariffId, String supplyPeriod) {
        return new ErpConsumption(accountNo, tariffId, false, supplyPeriod, BigDecimal.ONE, BigDecimal.ONE, 1);
    }

    private void account(Group club, String name, String accountNo) {
        Guest guest = guestDao.save(new Guest(club, GuestType.SEASONAL, name));
        billingAccountDao.saveAndFlush(new BillingAccount(guest, accountNo, name));
    }

    private ConsumptionRecord record(String accountNo, String kwh, String cost, LocalDateTime plugOut,
                                     Long tariffId) {
        SupplySession session = new SupplySession();
        session.setSocket(pedestal.getSockets().get(0));
        session.setTransactionId(TRANSACTIONS.incrementAndGet());
        session.setCreateDate(plugOut.minusHours(2));
        session.setMaxCurrentA(16);
        supplySessionDao.save(session);

        ConsumptionRecord record = new ConsumptionRecord();
        record.setRecordNo("ERPSQL-" + session.getTransactionId());
        record.setSupplySessionId(session.getId());
        record.setSiteGroupId(pedestal.getGroup().getId());
        record.setBillingAccountNo(accountNo);
        record.setPaymentChannel(PaymentChannel.CARD);
        record.setSettlementFlags(new SettlementFlags(true, false, false));
        record.setKwh(new BigDecimal(kwh));
        record.setPlugIn(plugOut.minusHours(2));
        record.setPlugOut(plugOut);
        record.setNights(1);
        record.setCost(cost != null ? new BigDecimal(cost) : null);
        record.setPricingStatus(PricingStatus.PRICED);
        record.setTariffId(tariffId);
        record.setCreateDate(plugOut);
        return consumptionRecordDao.saveAndFlush(record);
    }
}
