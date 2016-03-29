package io.pillopl.erp;

import io.pillopl.config.SystemPropertyService;
import io.pillopl.consumption.ExportStatus;
import io.pillopl.erp.ErpBillingLine.LineType;
import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import io.pillopl.group.Market;
import io.pillopl.guest.BillingAccount;
import io.pillopl.guest.Guest;
import io.pillopl.guest.GuestType;
import io.pillopl.guest.SeasonalContract;
import io.pillopl.guest.SeasonalContractService;
import io.pillopl.order.Order;
import io.pillopl.order.OrderItem;
import io.pillopl.order.ProductDefinition;
import io.pillopl.persistence.dao.BillingAccountDao;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
import io.pillopl.persistence.dao.ConsumptionRecordDaoCustom.ErpConsumption;
import io.pillopl.persistence.dao.ErpBillingLineDao;
import io.pillopl.persistence.dao.SystemPropertyDao;
import io.pillopl.pricing.PriceComponent;
import io.pillopl.pricing.PricingService;
import io.pillopl.pricing.Tariff;
import io.pillopl.pricing.TariffElement;
import io.pillopl.pricing.TariffResolver;
import io.pillopl.util.DateService;
import io.pillopl.value.BillingPeriod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpBillingDataServiceTest {

    static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 2, 4, 0);
    static final String ACCOUNT = "400012340001";
    static final BillingPeriod JULY = BillingPeriod.of(7, 2026);

    SystemPropertyDao systemPropertyDao = mock(SystemPropertyDao.class);
    BillingAccountDao billingAccountDao = mock(BillingAccountDao.class);
    SeasonalContractService contractService = mock(SeasonalContractService.class);
    ConsumptionRecordDao recordDao = mock(ConsumptionRecordDao.class);
    ErpBillingLineDao lineDao = mock(ErpBillingLineDao.class);
    DateService dateService = new DateService();

    ErpBillingDataService service = new ErpBillingDataService(new SystemPropertyService(systemPropertyDao),
            dateService, billingAccountDao, contractService, recordDao, lineDao,
            new PricingService(mock(TariffResolver.class)));

    Tariff seasonalTariff;
    ProductDefinition seasonPackage;
    Guest guest;
    BillingAccount account;
    List<ErpConsumption> consumption = new ArrayList<>();

    @BeforeEach
    void setUp() {
        dateService.setClock(Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));
        seasonalTariff = new Tariff("HR SEASONAL", "Seasonal", Market.HR);
        seasonalTariff.setId(4L);
        seasonalTariff.addElement(new TariffElement().addComponent(PriceComponent.energy("0.3800", 1)));
        seasonPackage = new ProductDefinition();
        seasonPackage.setCode("HR-SEASON-PACKAGE");
        seasonPackage.setFixedPriceErpArticleNo("ART-SEASON-FIX");
        seasonPackage.setUnitPriceErpArticleNo("ART-SEASON-KWH");
        guest = new Guest(new Group(GroupType.ISSUER, "NETWORK CARD"), GuestType.SEASONAL, "Horvat");
        guest.setErpPartnerNo("P-1001");
        account = guest.openBillingAccount(ACCOUNT, "Season");
        account.setId(10L);
        when(billingAccountDao.findByErpAccountNoIsNotNullOrderById()).thenReturn(List.of(account));
        when(recordDao.findErpConsumptionPerMonth(any(), any())).thenReturn(consumption);
        when(contractService.findContractsActiveIn(anyLong(), any(), any())).thenReturn(List.of());
    }

    @Test
    void transitPowerIsBilledAtTheCostOfItsRecordsWithTheErpConstants() {
        consumption.add(new ErpConsumption(ACCOUNT, 1L, false, "2026_06", new BigDecimal("4.0000"),
                new BigDecimal("1.80"), 1));
        consumption.add(new ErpConsumption(ACCOUNT, 1L, false, "2026_07", new BigDecimal("16.0000"),
                new BigDecimal("7.20"), 3));
        consumption.add(new ErpConsumption(ACCOUNT, 1L, true, "2026_06", new BigDecimal("99.0000"),
                new BigDecimal("44.55"), 9));

        List<ErpBillingLine> lines = service.findAndCalculateErpBillingLines(7, 2026);

        assertThat(lines).singleElement().satisfies(line -> {
            assertThat(line.getBillingPeriod()).isEqualTo("2026_07");
            assertThat(line.getLineType()).isEqualTo(LineType.POWER);
            assertThat(line.getArticleNo()).isEqualTo("ART-POWER-KWH");
            assertThat(line.getQuantity()).isEqualByComparingTo("20");
            assertThat(line.getAmount()).isEqualByComparingTo("9.00");
            assertThat(line.getUnitPrice()).isEqualByComparingTo("0.4500");
            assertThat(line.getDescription()).isEqualTo("Power 2026_06, 2026_07");
            assertThat(line.getBillingAccountNo()).isEqualTo(ACCOUNT);
            assertThat(line.getErpPartnerNo()).isEqualTo("P-1001");
            assertThat(line.getTaxClass()).isEqualTo("A1");
            assertThat(line.getArticleClassification()).isEqualTo("EL");
            assertThat(line.getSalesOffice()).isEqualTo("CS01");
            assertThat(line.getSalesGroup()).isEqualTo("PWR");
            assertThat(line.getAssignee()).isEqualTo("CAMPSITE-POWER");
            assertThat(line.getOrderType()).isEqualTo("ZPW");
            assertThat(line.getAlternateText()).isEqualTo("Pitch power of your stay");
            assertThat(line.getCurrency()).isEqualTo("EUR");
            assertThat(line.getCreateDate()).isEqualTo(NOW);
            assertThat(line.getExportStatus()).isEqualTo(ExportStatus.NEW);
        });
    }

    @Test
    void organisationGetsTheBillTextForOrganisations() {
        guest.setOrganisationName("Sailing club");
        consumption.add(new ErpConsumption(ACCOUNT, 1L, false, "2026_07", new BigDecimal("16"),
                new BigDecimal("7.20"), 3));

        assertThat(service.findAndCalculateErpBillingLines(7, 2026)).extracting(ErpBillingLine::getAlternateText)
                .containsExactly("Pitch power of your organisation");
    }

    @Test
    void billBelowTheMinimumIsNotSent() {
        consumption.add(new ErpConsumption(ACCOUNT, 1L, false, "2026_07", new BigDecimal("10"),
                new BigDecimal("4.99"), 1));

        assertThat(service.findAndCalculateErpBillingLines(7, 2026)).isEmpty();
    }

    @Test
    void accountClosedBeforeThePeriodIsNotBilled() {
        account.close(LocalDateTime.of(2026, 6, 30, 18, 0));
        consumption.add(new ErpConsumption(ACCOUNT, 1L, false, "2026_06", new BigDecimal("16"),
                new BigDecimal("7.20"), 3));

        assertThat(service.findAndCalculateErpBillingLines(7, 2026)).isEmpty();
        verify(contractService, never()).findContractsActiveIn(anyLong(), any(), any());
    }

    @Test
    void accountWithoutContractsAndConsumptionIsNotBilled() {
        assertThat(service.findAndCalculateErpBillingLines(7, 2026)).isEmpty();
    }

    @Test
    void seasonalContractPaysItsShareOfTheSeasonAndTheKwhAboveThePackage() {
        SeasonalContract contract = contract(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 9, 30), "390.00",
                SeasonalContract.FeePeriod.SEASONAL, "600");
        when(contractService.findContractsActiveIn(eq(10L), eq(JULY.getFirstDay()), eq(JULY.getLastDay())))
                .thenReturn(List.of(contract));
        consumption.add(new ErpConsumption(ACCOUNT, 4L, true, "2026_06", new BigDecimal("590"),
                new BigDecimal("224.20"), 30));
        consumption.add(new ErpConsumption(ACCOUNT, 4L, false, "2026_07", new BigDecimal("30"),
                new BigDecimal("11.40"), 5));
        consumption.add(new ErpConsumption(ACCOUNT, 4L, false, "2026_04", new BigDecimal("50"),
                new BigDecimal("19.00"), 2)); // before the season
        consumption.add(new ErpConsumption(ACCOUNT, 1L, false, "2026_07", new BigDecimal("10"),
                new BigDecimal("4.50"), 1)); // another campsite

        List<ErpBillingLine> lines = service.findAndCalculateErpBillingLines(7, 2026);

        // 31 of 153 season days of 390.00; 20 kWh above 600 at 0.38
        assertThat(lines).extracting(ErpBillingLine::getArticleNo, ErpBillingLine::getLineType,
                        ErpBillingLine::getQuantity, ErpBillingLine::getAmount)
                .containsExactly(
                        tuple("ART-POWER-KWH", LineType.POWER, new BigDecimal("10"), new BigDecimal("4.50")),
                        tuple("ART-SEASON-FIX", LineType.FIXED_FEE, new BigDecimal("0.2026"), new BigDecimal("79.02")),
                        tuple("ART-SEASON-KWH", LineType.PACKAGE_OVERAGE, new BigDecimal("20"),
                                new BigDecimal("7.60")));
        assertThat(lines.get(2).getUnitPrice()).isEqualByComparingTo("0.3800");
        assertThat(lines.get(2).getDescription()).isEqualTo("kWh above the package of 600 kWh, pitch S7");
    }

    @Test
    void consumptionWithinThePackageBringsNoOverage() {
        SeasonalContract contract = contract(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 9, 30), "390.00",
                SeasonalContract.FeePeriod.SEASONAL, "600");
        when(contractService.findContractsActiveIn(anyLong(), any(), any())).thenReturn(List.of(contract));
        consumption.add(new ErpConsumption(ACCOUNT, 4L, true, "2026_06", new BigDecimal("300"),
                new BigDecimal("114.00"), 30));
        consumption.add(new ErpConsumption(ACCOUNT, 4L, false, "2026_07", new BigDecimal("300"),
                new BigDecimal("114.00"), 30));

        assertThat(service.findAndCalculateErpBillingLines(7, 2026)).extracting(ErpBillingLine::getLineType)
                .containsExactly(LineType.FIXED_FEE);
    }

    @Test
    void kwhAboveThePackageBilledBeforeAreNotBilledAgain() {
        SeasonalContract contract = contract(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 9, 30), "0",
                SeasonalContract.FeePeriod.SEASONAL, "600");
        when(contractService.findContractsActiveIn(anyLong(), any(), any())).thenReturn(List.of(contract));
        consumption.add(new ErpConsumption(ACCOUNT, 4L, true, "2026_06", new BigDecimal("650"),
                new BigDecimal("247.00"), 30));
        consumption.add(new ErpConsumption(ACCOUNT, 4L, false, "2026_07", new BigDecimal("25"),
                new BigDecimal("9.50"), 30));

        assertThat(service.findAndCalculateErpBillingLines(7, 2026)).extracting(ErpBillingLine::getQuantity,
                ErpBillingLine::getAmount).containsExactly(tuple(new BigDecimal("25"), new BigDecimal("9.50")));
    }

    @Test
    void monthlyFeeOfAContractStartingInTheMonthIsItsShareOfTheMonthAndThePackageStartsAnew() {
        SeasonalContract contract = contract(LocalDate.of(2026, 7, 16), null, "40.00",
                SeasonalContract.FeePeriod.MONTHLY, "100");
        when(contractService.findContractsActiveIn(anyLong(), any(), any())).thenReturn(List.of(contract));
        consumption.add(new ErpConsumption(ACCOUNT, 4L, true, "2026_07", new BigDecimal("500"),
                new BigDecimal("190.00"), 30)); // a monthly package is not used up by earlier months
        consumption.add(new ErpConsumption(ACCOUNT, 4L, false, "2026_07", new BigDecimal("90"),
                new BigDecimal("34.20"), 30));

        // 16 of 31 days of 40.00
        assertThat(service.findAndCalculateErpBillingLines(7, 2026)).extracting(ErpBillingLine::getLineType,
                ErpBillingLine::getAmount).containsExactly(tuple(LineType.FIXED_FEE, new BigDecimal("20.65")));
    }

    @Test
    void seasonWithoutEndPaysTheWholeFeeWithItsFirstMonth() {
        SeasonalContract contract = contract(LocalDate.of(2026, 7, 10), null, "390.00",
                SeasonalContract.FeePeriod.SEASONAL, "600");

        assertThat(ErpBillingDataService.getFeeFraction(contract, JULY)).isEqualByComparingTo("1");
        assertThat(ErpBillingDataService.getFeeFraction(contract, BillingPeriod.of(8, 2026))).isZero();
    }

    @Test
    void terminationEndsTheShareOfTheSeasonalFeeEarlier() {
        SeasonalContract contract = contract(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 9, 30), "390.00",
                SeasonalContract.FeePeriod.SEASONAL, "600");
        contract.requestTermination(LocalDate.of(2026, 7, 15), NOW);

        // 15 of the 153 days of the season
        assertThat(ErpBillingDataService.getFeeFraction(contract, JULY)).isEqualByComparingTo("0.098039");
        assertThat(ErpBillingDataService.getFeeFraction(contract, BillingPeriod.of(8, 2026))).isZero();
    }

    @Test
    void contractWithoutProductLeavesTheAccountUnbilled() {
        SeasonalContract contract = new SeasonalContract(account, seasonalTariff, null, "S7",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 9, 30));
        contract.setFixedFee(new BigDecimal("390.00"));
        when(contractService.findContractsActiveIn(anyLong(), any(), any())).thenReturn(List.of(contract));
        consumption.add(new ErpConsumption(ACCOUNT, 1L, false, "2026_07", new BigDecimal("16"),
                new BigDecimal("7.20"), 3));

        assertThat(service.findAndCalculateErpBillingLines(7, 2026)).isEmpty();
    }

    @Test
    void kwhAboveThePackageThatCannotBePricedLeaveTheAccountUnbilled() {
        Tariff withoutElements = new Tariff("EMPTY", "nothing applies", Market.HR);
        withoutElements.setId(4L);
        SeasonalContract contract = contract(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 9, 30), "390.00",
                SeasonalContract.FeePeriod.SEASONAL, "10");
        contract.setTariff(withoutElements);
        when(contractService.findContractsActiveIn(anyLong(), any(), any())).thenReturn(List.of(contract));
        consumption.add(new ErpConsumption(ACCOUNT, 4L, false, "2026_07", new BigDecimal("30"),
                new BigDecimal("11.40"), 5));

        assertThat(service.findAndCalculateErpBillingLines(7, 2026)).isEmpty();
    }

    @Test
    void orderedContractNamesItsOrderOnTheLines() {
        SeasonalContract contract = contract(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 9, 30), "390.00",
                SeasonalContract.FeePeriod.SEASONAL, "600");
        Order order = new Order();
        order.setOrderNumber("HR-100042");
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductDefinition(seasonPackage);
        contract.setOrderItem(item);
        when(contractService.findContractsActiveIn(anyLong(), any(), any())).thenReturn(List.of(contract));

        assertThat(service.findAndCalculateErpBillingLines(7, 2026)).extracting(ErpBillingLine::getOrderNumber)
                .containsExactly("HR-100042");
    }

    @Test
    void dateRangeFractionIsTheShareOfTheDaysOfTheMonth() {
        assertThat(ErpBillingDataService.getDateRangeFraction(LocalDate.of(2026, 5, 1), null, JULY))
                .isEqualByComparingTo("1");
        assertThat(ErpBillingDataService.getDateRangeFraction(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                JULY)).isEqualByComparingTo("1");
        assertThat(ErpBillingDataService.getDateRangeFraction(LocalDate.of(2026, 7, 16), null, JULY))
                .isEqualByComparingTo("0.516129");
        assertThat(ErpBillingDataService.getDateRangeFraction(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 7, 1),
                JULY)).isEqualByComparingTo("0.032258");
        assertThat(ErpBillingDataService.getDateRangeFraction(LocalDate.of(2026, 8, 1), null, JULY)).isZero();
        assertThat(ErpBillingDataService.getDateRangeFraction(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 30),
                JULY)).isZero();
    }

    @Test
    void monthIsBilledOnce() {
        when(lineDao.existsByBillingPeriod("2026_07")).thenReturn(true);

        assertThatThrownBy(() -> service.createErpBillingLines(7, 2026)).isInstanceOf(IllegalStateException.class);
        verify(lineDao, never()).saveAll(any());
    }

    @Test
    void billingRunSavesTheLinesAndMarksTheRecordsOfTheBilledAccounts() {
        ErpConsumption power = new ErpConsumption(ACCOUNT, 1L, false, "2026_07", new BigDecimal("16"),
                new BigDecimal("7.20"), 3);
        consumption.add(power);
        when(lineDao.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.createErpBillingLines(7, 2026)).hasSize(1);

        verify(recordDao).markRecordsForErpBilling(eq(Set.of(power)),
                eq(LocalDateTime.of(2025, 7, 1, 0, 0)), eq(LocalDateTime.of(2026, 8, 1, 0, 0)), eq("2026_07"));
    }

    @Test
    void lastMonthIsNotBilledBeforeTheRunDayOfTheMonth() {
        // NOW is the 2nd, the run day defaults to the 3rd
        assertThat(service.createErpBillingLinesOfLastMonth()).isEmpty();
        verify(lineDao, never()).existsByBillingPeriod(anyString());
    }

    @Test
    void lastMonthIsBilledOnlyWhenNotBilledYet() {
        dateService.setClock(Clock.fixed(NOW.plusDays(1).atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()));
        when(lineDao.existsByBillingPeriod("2026_07")).thenReturn(true);

        assertThat(service.createErpBillingLinesOfLastMonth()).isEmpty();
        verify(recordDao, never()).findErpConsumptionPerMonth(any(), any());

        when(lineDao.existsByBillingPeriod("2026_07")).thenReturn(false);
        when(lineDao.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.createErpBillingLinesOfLastMonth();
        verify(recordDao).findErpConsumptionPerMonth(LocalDateTime.of(2025, 7, 1, 0, 0),
                LocalDateTime.of(2026, 8, 1, 0, 0));
    }

    @Test
    void transferredLinesMoveTheirRecordsOncePerAccount() {
        ErpBillingLine fee = line("ART-SEASON-FIX");
        ErpBillingLine overage = line("ART-SEASON-KWH");

        service.markLinesTransferred(List.of(fee, overage));

        assertThat(fee.getExportStatus()).isEqualTo(ExportStatus.TRANSFERRED);
        assertThat(overage.getExportDate()).isEqualTo(NOW);
        verify(recordDao, times(1)).updateErpBilledRecords("2026_07", ACCOUNT, List.of(ExportStatus.WORK),
                ExportStatus.TRANSFERRED);
    }

    @Test
    void bookedLineMovesTheRecordsToProcessed() {
        ErpBillingLine fee = line("ART-SEASON-FIX");
        fee.transferred(NOW);
        when(lineDao.findByBillingPeriodAndBillingAccountNoOrderByArticleNo("2026_07", ACCOUNT))
                .thenReturn(List.of(fee));

        assertThat(service.takeErpAnswer("2026_07", ACCOUNT, "ART-SEASON-FIX", true, null)).isTrue();

        assertThat(fee.getExportStatus()).isEqualTo(ExportStatus.PROCESSED);
        verify(recordDao).updateErpBilledRecords("2026_07", ACCOUNT, List.of(ExportStatus.TRANSFERRED),
                ExportStatus.PROCESSED);
    }

    @Test
    void refusedLinePutsTheRecordsOfTheAccountInError() {
        ErpBillingLine fee = line("ART-SEASON-FIX");
        fee.transferred(NOW);
        when(lineDao.findByBillingPeriodAndBillingAccountNoOrderByArticleNo("2026_07", ACCOUNT))
                .thenReturn(List.of(fee));

        assertThat(service.takeErpAnswer("2026_07", ACCOUNT, "ART-SEASON-FIX", false, "unknown article")).isTrue();

        assertThat(fee.getExportStatus()).isEqualTo(ExportStatus.ERROR);
        assertThat(fee.getErrorText()).isEqualTo("unknown article");
        verify(recordDao).updateErpBilledRecords("2026_07", ACCOUNT,
                List.of(ExportStatus.TRANSFERRED, ExportStatus.PROCESSED), ExportStatus.ERROR);
    }

    @Test
    void answerForALineNeverSentIsIgnored() {
        when(lineDao.findByBillingPeriodAndBillingAccountNoOrderByArticleNo("2026_07", ACCOUNT))
                .thenReturn(List.of(line("ART-SEASON-FIX")));

        assertThat(service.takeErpAnswer("2026_07", ACCOUNT, "ART-SEASON-FIX", true, null)).isFalse();
        assertThat(service.takeErpAnswer("2026_07", ACCOUNT, "ART-OTHER", true, null)).isFalse();
        verify(recordDao, never()).updateErpBilledRecords(anyString(), anyString(), anyCollection(), any());
    }

    @Test
    void refusedLinesAreSentAgainWithTheirRecords() {
        ErpBillingLine refused = line("ART-SEASON-FIX");
        refused.transferred(NOW);
        refused.answered(false, "unknown article", NOW);
        ErpBillingLine booked = line("ART-SEASON-KWH");
        booked.transferred(NOW);
        booked.answered(true, null, NOW);
        when(lineDao.findByBillingPeriodOrderByBillingAccountNoAscArticleNoAsc("2026_07"))
                .thenReturn(List.of(refused, booked));

        assertThat(service.resendRefusedLines("2026_07")).isEqualTo(1);

        assertThat(refused.getExportStatus()).isEqualTo(ExportStatus.NEW);
        assertThat(booked.getExportStatus()).isEqualTo(ExportStatus.PROCESSED);
        verify(recordDao).updateErpBilledRecords("2026_07", null, List.of(ExportStatus.ERROR), ExportStatus.WORK);
    }

    private SeasonalContract contract(LocalDate start, LocalDate end, String fee, SeasonalContract.FeePeriod period,
                                      String packageKwh) {
        SeasonalContract contract = new SeasonalContract(account, seasonalTariff, null, "S7", start, end);
        contract.setId(20L);
        contract.setFixedFee(new BigDecimal(fee));
        contract.setFeePeriod(period);
        contract.addPackageItem(seasonPackage, null, new BigDecimal(packageKwh));
        return contract;
    }

    private ErpBillingLine line(String articleNo) {
        ErpBillingLine line = new ErpBillingLine("2026_07", account, LineType.FIXED_FEE, articleNo);
        line.add(BigDecimal.ONE, new BigDecimal("79.02"));
        return line;
    }
}
