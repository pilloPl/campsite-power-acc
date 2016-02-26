package io.pillopl.erp;

import io.pillopl.card.CampingCard;
import io.pillopl.consumption.ConsumptionRecord;
import io.pillopl.consumption.ExportStatus;
import io.pillopl.consumption.PaymentChannel;
import io.pillopl.consumption.SettlementFlags;
import io.pillopl.erp.ErpBillingLine.LineType;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.guest.Guest;
import io.pillopl.guest.GuestType;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.order.Order;
import io.pillopl.order.OrderService;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
import io.pillopl.persistence.dao.SupplySessionDao;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.pricing.PricingStatus;
import io.pillopl.supply.SupplySession;
import io.pillopl.test.IntegrationTest;
import io.pillopl.user.Userinfo;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The monthly ERP billing on the database: a seasonal resident ordered the season package (390.00 for April to
 * September, 600 kWh included, seasonal tariff 0.38 per kWh), his records are billed month by month, the package is
 * used up across the months, the lines follow the ERP file and the ERP's answers and their records follow them.
 */
@Transactional
class ErpBillingDataServiceIntegrationTest extends IntegrationTest {

    static final LocalDate SEASON_START = LocalDate.of(2026, 4, 1);
    static final String ACCOUNT = "400077770001";
    static final Long SEASONAL_TARIFF = 4L;
    static final Long STANDARD_TARIFF = 1L;
    static final AtomicInteger TRANSACTIONS = new AtomicInteger(770_000);

    @Autowired ErpBillingDataService erpBillingDataService;
    @Autowired OrderService orderService;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired UserinfoDao userinfoDao;
    @Autowired CampingCardDao campingCardDao;
    @Autowired ConsumptionRecordDao consumptionRecordDao;
    @Autowired SupplySessionDao supplySessionDao;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired EntityManager entityManager;

    Pedestal pedestal;

    @BeforeEach
    void setUp() {
        Group club = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"), "ERP CLUB", "Club");
        Group bay = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "BAY ERP", "Bay");
        groupManager.addAcceptanceAgreement(club, bay);
        pedestal = infrastructure.pedestal("HR-BAYERP-P001", bay, 1);
        pedestal.getSockets().get(0).setPitchNo("S7");
        Userinfo office = new Userinfo("erpoffice", "Office", "erp-office@example.org");
        office.setGroup(groupService.fetchStaffGroup("HR RECEPTION"));
        office.getGroups().add(club);
        userinfoDao.saveAndFlush(office);
        CampingCard card = campingCardDao.saveAndFlush(new CampingCard.Builder("7701", "HR-7701").build());

        Order order = orderService.createOrderForNewGuest(office, new Guest.Builder(club, GuestType.SEASONAL,
                "Kovac").firstName("Ivo").iban("HR1210010051863000160"));
        orderService.addItem(office, order.getId(), "HR-SEASON-PACKAGE", 1, bay.getId(), "S7", SEASON_START, null,
                null);
        orderService.addItem(office, order.getId(), "HR-TRANSIT-CARD", 1, null, null, null, null, card.getId());
        orderService.submitOrder(office, order.getId());
        orderService.enrichOrderWithErpNumbers(order.getId(), "P-7701", ACCOUNT);
        entityManager.flush();
    }

    @Test
    void seasonalResidentPaysHisShareOfTheSeasonAndTheKwhAboveThePackageMonthByMonth() {
        record("350", LocalDateTime.of(2026, 6, 20, 10, 0), SEASONAL_TARIFF);
        record("8", LocalDateTime.of(2026, 6, 25, 10, 0), STANDARD_TARIFF); // another campsite, priced there

        List<ErpBillingLine> june = erpBillingDataService.createErpBillingLines(6, 2026);

        // 30 of 183 season days of 390.00; the package is not used up
        assertThat(june).extracting(ErpBillingLine::getLineType, ErpBillingLine::getArticleNo,
                        ErpBillingLine::getQuantity, ErpBillingLine::getAmount)
                .containsExactly(
                        tuple(LineType.POWER, "ART-POWER-KWH", new BigDecimal("8.0000"), new BigDecimal("3.60")),
                        tuple(LineType.FIXED_FEE, "ART-SEASON-FIX", new BigDecimal("0.1639"), new BigDecimal("63.93")));
        assertThat(june).allSatisfy(line -> {
            assertThat(line.getId()).isNotNull();
            assertThat(line.getErpPartnerNo()).isEqualTo("P-7701");
            assertThat(line.getOrderNumber()).matches("HR-\\d{6}");
        });

        record("300", LocalDateTime.of(2026, 7, 10, 10, 0), SEASONAL_TARIFF);
        entityManager.clear();

        List<ErpBillingLine> july = erpBillingDataService.createErpBillingLines(7, 2026);

        // 31 of 183 days; 350 + 300 kWh against 600: 50 kWh at the seasonal tariff 0.38
        assertThat(july).extracting(ErpBillingLine::getLineType, ErpBillingLine::getQuantity,
                        ErpBillingLine::getAmount)
                .containsExactly(
                        tuple(LineType.FIXED_FEE, new BigDecimal("0.1694"), new BigDecimal("66.07")),
                        tuple(LineType.PACKAGE_OVERAGE, new BigDecimal("50.0000"), new BigDecimal("19.00")));
        assertThat(july.get(1).getUnitPrice()).isEqualByComparingTo("0.3800");
    }

    @Test
    void recordsBilledByARunFollowTheirLinesThroughTheErpFileAndAnswers() {
        ConsumptionRecord june = record("700", LocalDateTime.of(2026, 6, 20, 10, 0), SEASONAL_TARIFF);
        ConsumptionRecord review = record("20", LocalDateTime.of(2026, 6, 21, 10, 0), SEASONAL_TARIFF);
        review.setExportStatus(ExportStatus.REVIEW);
        consumptionRecordDao.flush();

        List<ErpBillingLine> lines = erpBillingDataService.createErpBillingLines(6, 2026);
        assertThat(exportStatus(june)).isEqualTo(ExportStatus.WORK);
        assertThat(exportStatus(review)).isEqualTo(ExportStatus.REVIEW);
        assertThatThrownBy(() -> erpBillingDataService.createErpBillingLines(6, 2026))
                .isInstanceOf(IllegalStateException.class);

        assertThat(erpBillingDataService.findLinesToTransfer()).containsExactlyElementsOf(lines);
        erpBillingDataService.markLinesTransferred(lines);
        assertThat(erpBillingDataService.findLinesToTransfer()).isEmpty();
        assertThat(exportStatus(june)).isEqualTo(ExportStatus.TRANSFERRED);

        assertThat(erpBillingDataService.takeErpAnswer("2026_06", ACCOUNT, "ART-SEASON-FIX", true, null)).isTrue();
        assertThat(exportStatus(june)).isEqualTo(ExportStatus.PROCESSED);
        assertThat(erpBillingDataService.takeErpAnswer("2026_06", ACCOUNT, "ART-SEASON-KWH", false,
                "article blocked")).isTrue();
        assertThat(exportStatus(june)).isEqualTo(ExportStatus.ERROR);

        assertThat(erpBillingDataService.resendRefusedLines("2026_06")).isEqualTo(1);
        assertThat(exportStatus(june)).isEqualTo(ExportStatus.WORK);
        assertThat(erpBillingDataService.findLinesToTransfer()).extracting(ErpBillingLine::getArticleNo)
                .containsExactly("ART-SEASON-KWH");
        assertThat(erpBillingDataService.findErpBillingLines("2026_06")).extracting(ErpBillingLine::getArticleNo,
                ErpBillingLine::getExportStatus).containsExactly(tuple("ART-SEASON-FIX", ExportStatus.PROCESSED),
                tuple("ART-SEASON-KWH", ExportStatus.NEW));
    }

    @Test
    void recordConfirmedAfterTheRunIsBilledWithTheNextMonth() {
        record("100", LocalDateTime.of(2026, 6, 20, 10, 0), SEASONAL_TARIFF);
        ConsumptionRecord late = record("550", LocalDateTime.of(2026, 6, 28, 10, 0), SEASONAL_TARIFF);
        late.setExportStatus(ExportStatus.REVIEW);
        consumptionRecordDao.flush();
        erpBillingDataService.createErpBillingLines(6, 2026);

        late.setExportStatus(ExportStatus.NEW);
        consumptionRecordDao.flush();
        entityManager.clear();

        // 100 billed in June, 550 confirmed later: 50 kWh above the package in the July run
        assertThat(erpBillingDataService.createErpBillingLines(7, 2026))
                .filteredOn(line -> line.getLineType() == LineType.PACKAGE_OVERAGE)
                .extracting(ErpBillingLine::getQuantity).containsExactly(new BigDecimal("50.0000"));
        assertThat(consumptionRecordDao.findById(late.getId()).orElseThrow().getExportStatus())
                .isEqualTo(ExportStatus.WORK);
    }

    private ExportStatus exportStatus(ConsumptionRecord record) {
        entityManager.flush();
        entityManager.clear();
        return consumptionRecordDao.findById(record.getId()).orElseThrow().getExportStatus();
    }

    private ConsumptionRecord record(String kwh, LocalDateTime plugOut, Long tariffId) {
        SupplySession session = new SupplySession();
        session.setSocket(pedestal.getSockets().get(0));
        session.setTransactionId(TRANSACTIONS.incrementAndGet());
        session.setCreateDate(plugOut.minusHours(5));
        session.setMaxCurrentA(16);
        supplySessionDao.save(session);

        BigDecimal energy = new BigDecimal(kwh);
        ConsumptionRecord record = new ConsumptionRecord();
        record.setRecordNo("ERP-" + session.getTransactionId());
        record.setSupplySessionId(session.getId());
        record.setSocketId(pedestal.getSockets().get(0).getId());
        record.setPitchNo("S7");
        record.setSiteGroupId(pedestal.getGroup().getId());
        record.setBillingAccountNo(ACCOUNT);
        record.setErpPartnerNo("P-7701");
        record.setPaymentChannel(PaymentChannel.CARD);
        record.setSettlementFlags(new SettlementFlags(true, false, false));
        record.setKwh(energy);
        record.setPlugIn(plugOut.minusHours(5));
        record.setPlugOut(plugOut);
        record.setNights(1);
        record.setTariffId(tariffId);
        record.setPricingStatus(PricingStatus.PRICED);
        record.setCost(energy.multiply(new BigDecimal(tariffId.equals(SEASONAL_TARIFF) ? "0.38" : "0.45"))
                .setScale(2, java.math.RoundingMode.HALF_UP));
        record.setCurrency("EUR");
        record.setCreateDate(plugOut);
        return consumptionRecordDao.saveAndFlush(record);
    }
}
