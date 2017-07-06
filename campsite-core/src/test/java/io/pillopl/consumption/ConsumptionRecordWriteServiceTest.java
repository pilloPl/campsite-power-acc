package io.pillopl.consumption;

import io.pillopl.card.CampingCard;
import io.pillopl.card.HouseCard;
import io.pillopl.card.HouseCardType;
import io.pillopl.card.ValidationService.ValidationResult;
import io.pillopl.config.SystemPropertyService;
import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import io.pillopl.group.attribute.IssuerTenantService;
import io.pillopl.group.attribute.SiteTenantService;
import io.pillopl.guest.Guest;
import io.pillopl.guest.GuestType;
import io.pillopl.infrastructure.Location;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
import io.pillopl.pricing.PriceCalculation;
import io.pillopl.pricing.PricingInput;
import io.pillopl.pricing.PricingService;
import io.pillopl.pricing.PricingStatus;
import io.pillopl.supply.Consumption;
import io.pillopl.supply.StopReason;
import io.pillopl.supply.SupplyDuration;
import io.pillopl.supply.SupplySession;
import io.pillopl.util.DateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsumptionRecordWriteServiceTest {

    static final LocalDateTime PLUG_IN = LocalDateTime.of(2026, 7, 10, 18, 0);
    static final LocalDateTime PLUG_OUT = LocalDateTime.of(2026, 7, 12, 9, 0);

    final ConsumptionRecordDao dao = mock(ConsumptionRecordDao.class);
    final SystemPropertyService systemPropertyService = mock(SystemPropertyService.class);
    final PricingService pricingService = mock(PricingService.class);
    final SiteTenantService siteTenantService = mock(SiteTenantService.class);
    final IssuerTenantService issuerTenantService = mock(IssuerTenantService.class);
    final SuspectRecordNotifier notifier = mock(SuspectRecordNotifier.class);
    @SuppressWarnings("unchecked")
    final ObjectProvider<SuspectRecordNotifier> notifierProvider = mock(ObjectProvider.class);

    final ConsumptionRecordWriteService writeService = new ConsumptionRecordWriteService(dao,
            new ConsumptionRecordFactory(systemPropertyService, dao), pricingService, siteTenantService,
            issuerTenantService, new DateService(), notifierProvider);

    final Group site = group(5L, GroupType.SITE);
    final Group club = group(7L, GroupType.ISSUER);
    final Pedestal pedestal = new Pedestal();
    final Socket socket = new Socket();
    final Guest guest = new Guest(club, GuestType.TRANSIT, "Horvat");
    final CampingCard card = new CampingCard.Builder("7001", "HR-1").guest(guest).billingAccountNo("BA-1").build();
    final SupplySession session = new SupplySession();

    @BeforeEach
    void setUp() {
        when(systemPropertyService.getSuspectRecordKwhPerNightThreshold()).thenReturn(BigDecimal.valueOf(300));
        when(dao.nextRecordNumber()).thenReturn(7L);
        when(dao.save(any(ConsumptionRecord.class))).thenAnswer(i -> i.getArgument(0));
        when(notifierProvider.getIfAvailable()).thenReturn(notifier);
        when(siteTenantService.findSiteTenantNameFor(pedestal)).thenReturn("Bay Camping d.o.o.");
        when(issuerTenantService.findIssuerTenantNameFor(club)).thenReturn("Camping Club");
        Location location = new Location("Pula", "52100", "Obala", "1");
        location.setCampsiteCode("BAY");
        location.setName("Bay Camping");
        location.setCountryIso("HR");
        pedestal.setId(3L);
        pedestal.setCode("HR-BAY-P001");
        pedestal.setGroup(site);
        pedestal.setLocation(location);
        pedestal.setBillable(true);
        socket.setId(11L);
        socket.setPedestal(pedestal);
        socket.setSocketCode("BAY-P001-1");
        socket.setPitchNo("A12");
        guest.setId(31L);
        guest.setFirstName("Ana");
        card.setId(21L);
        session.setId(101L);
        session.setTransactionId(1001);
        session.setSocket(socket);
        session.setCard(card);
        session.setMaxCurrentA(16);
        session.setStayId(55L);
    }

    @Test
    void recordOfACampingCardIsASnapshotOfPedestalAddressGuestAndTenants() {
        priced("6.30");

        ConsumptionRecord record = create(validated(card, null), "14.0", true);

        assertThat(record.getRecordNo()).isEqualTo("BAY-20260712-000007");
        assertThat(record.getSupplySessionId()).isEqualTo(101L);
        assertThat(record.getSocketCode()).isEqualTo("BAY-P001-1");
        assertThat(record.getPitchNo()).isEqualTo("A12");
        assertThat(record.getPedestalReferenceId()).isEqualTo("N/A");
        assertThat(record.getCity()).isEqualTo("Pula");
        assertThat(record.getCampsiteName()).isEqualTo("Bay Camping");
        assertThat(record.getSiteGroupId()).isEqualTo(5L);
        assertThat(record.getSiteTenant()).isEqualTo("Bay Camping d.o.o.");
        assertThat(record.getIssuerGroupId()).isEqualTo(7L);
        assertThat(record.getIssuerTenant()).isEqualTo("Camping Club");
        assertThat(record.getGuestName()).isEqualTo("Ana Horvat");
        assertThat(record.getBillingAccountNo()).isEqualTo("BA-1");
        assertThat(record.getStayId()).isEqualTo(55L);
        assertThat(record.getPaymentChannel()).isEqualTo(PaymentChannel.CARD);
        assertThat(record.getSettlementFlags()).isEqualTo(new SettlementFlags(true, false, false));
        assertThat(record.getNights()).isEqualTo(2);
        assertThat(record.getDurationSeconds()).isEqualTo(39 * 3600L);
        assertThat(record.getStopReason()).isEqualTo(StopReason.UNPLUGGED);
    }

    @Test
    void pricedRecordStoresStatusCostAndTariffAndWaitsForExport() {
        priced("6.30");

        ConsumptionRecord record = create(validated(card, null), "14.0", true);

        assertThat(record.getPricingStatus()).isEqualTo(PricingStatus.PRICED);
        assertThat(record.getCost()).isEqualByComparingTo("6.30");
        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.NEW);
        ArgumentCaptor<PricingInput> input = ArgumentCaptor.forClass(PricingInput.class);
        verify(pricingService).calculateCost(input.capture());
        assertThat(input.getValue().isBillable()).isTrue();
        assertThat(input.getValue().getSocketId()).isEqualTo(11L);
        assertThat(input.getValue().getCardId()).isEqualTo(21L);
        assertThat(input.getValue().getKwh()).isEqualByComparingTo("14.0");
    }

    @Test
    void failedPricingLeavesTheCostEmptyAndPutsTheRecordIntoReview() {
        when(pricingService.calculateCost(any(PricingInput.class)))
                .thenReturn(PriceCalculation.failed(null, "No tariff"));

        ConsumptionRecord record = create(validated(card, null), "14.0", true);

        assertThat(record.getPricingStatus()).isEqualTo(PricingStatus.PRICING_FAILED);
        assertThat(record.getCost()).isNull();
        assertThat(record.getPricingFailureReason()).isEqualTo("No tariff");
        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.REVIEW);
        assertThat(record.getValidationState()).isEqualTo(RecordValidationState.VALID);
    }

    @Test
    void notBilledSupplyIsPricedAsNotBillable() {
        when(pricingService.calculateCost(any(PricingInput.class))).thenReturn(PriceCalculation.notBillable());

        ConsumptionRecord record = create(validated(card, null), "14.0", false);

        ArgumentCaptor<PricingInput> input = ArgumentCaptor.forClass(PricingInput.class);
        verify(pricingService).calculateCost(input.capture());
        assertThat(input.getValue().isBillable()).isFalse();
        assertThat(record.getPricingStatus()).isEqualTo(PricingStatus.NOT_BILLABLE);
        assertThat(record.getCost()).isEqualByComparingTo("0");
        assertThat(record.getSettlementFlags().isBillable()).isFalse();
    }

    @Test
    void billedSupplyWithUnknownConsumptionCannotBePriced() {
        ConsumptionRecord record = create(validated(card, null), null, true);

        assertThat(record.getPricingStatus()).isEqualTo(PricingStatus.PRICING_FAILED);
        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.REVIEW);
        assertThat(record.getPricingFailureReason()).isEqualTo(ConsumptionRecordWriteService.UNKNOWN_CONSUMPTION);
        verify(pricingService, never()).calculateCost(any(PricingInput.class));
    }

    @Test
    void houseCardRecordHasNoGuestAndIsMarkedAsRental() {
        HouseCard houseCard = new HouseCard(HouseCardType.RENTAL, "9001", "HC-1");
        houseCard.setId(41L);
        session.setCard(null);
        session.setHouseCard(houseCard);
        when(pricingService.calculateCost(any(PricingInput.class))).thenReturn(PriceCalculation.notBillable());

        ConsumptionRecord record = create(validated(null, houseCard), "3.0", false);

        assertThat(record.getHouseCardId()).isEqualTo(41L);
        assertThat(record.getHouseCardType()).isEqualTo("RENTAL");
        assertThat(record.getCardRfid()).isEqualTo("9001");
        assertThat(record.getGuestId()).isNull();
        assertThat(record.getIssuerGroupId()).isNull();
        assertThat(record.getPaymentChannel()).isEqualTo(PaymentChannel.HOUSE_CARD);
        assertThat(record.getSettlementFlags()).isEqualTo(new SettlementFlags(false, false, true));
    }

    @Test
    void houseCardOfTheSessionIsUsedWhenTheValidationHasNone() {
        HouseCard houseCard = new HouseCard(HouseCardType.RENTAL, "9002", "HC-2");
        houseCard.setId(42L);
        session.setHouseCard(houseCard);
        when(pricingService.calculateCost(any(PricingInput.class))).thenReturn(PriceCalculation.notBillable());

        ConsumptionRecord record = create(validated(null, null), "3.0", false);

        assertThat(record.getHouseCardId()).isEqualTo(42L);
        assertThat(record.getCardId()).isNull();
        assertThat(record.getPaymentChannel()).isEqualTo(PaymentChannel.HOUSE_CARD);
    }

    @Test
    void campsiteSettlingItsOwnGuestsIsFlaggedOnTheRecord() {
        pedestal.setSettledByCampsite(true);
        priced("1.00");

        ConsumptionRecord record = create(validated(card, null), "2.0", true);

        assertThat(record.getSettlementFlags().isSettledByCampsite()).isTrue();
    }

    @Test
    void supplyPaidThroughTheAppIsSettledLikeACampingCardSupply() {
        session.setCard(null);
        pedestal.setSettledByCampsite(true);
        priced("1.00");

        ConsumptionRecord record = create(validated(null, null), "2.0", true);

        assertThat(record.getPaymentChannel()).isEqualTo(PaymentChannel.QR_APP);
        assertThat(record.getCardId()).isNull();
        assertThat(record.getSettlementFlags()).isEqualTo(new SettlementFlags(true, true, false));
    }

    @Test
    void addressOfAPedestalWithoutLocationIsNotAvailable() {
        pedestal.setLocation(null);
        priced("1.00");

        ConsumptionRecord record = create(validated(card, null), "2.0", true);

        assertThat(record.getCity()).isEqualTo("N/A");
        assertThat(record.getStreet()).isEqualTo("N/A");
        assertThat(record.getRecordNo()).startsWith("NA-20260712-");
    }

    @Test
    void sessionWithoutSocketGetsNoRecord() {
        session.setSocket(null);
        ValidationResult withoutSocket = validated(card, null);
        withoutSocket.setSocket(null);

        assertThatThrownBy(() -> create(withoutSocket, "2.0", true))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Supply session 101 has no socket");
        verify(dao, never()).save(any(ConsumptionRecord.class));
    }

    @Test
    void suspectRecordIsNotPricedAndTheTeamIsNotified() {
        ConsumptionRecord record = create(validated(card, null), "650", true);

        assertThat(record.getValidationState()).isEqualTo(RecordValidationState.SUSPECT);
        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.REVIEW);
        assertThat(record.getPricingStatus()).isNull();
        verify(pricingService, never()).calculateCost(any(PricingInput.class));
        verify(notifier).suspectRecordCreated(record);
    }

    @Test
    void suspectRecordIsSavedEvenWhenNoNotifierExists() {
        when(notifierProvider.getIfAvailable()).thenReturn(null);

        ConsumptionRecord record = create(validated(card, null), "-2", true);

        assertThat(record.isSuspect()).isTrue();
        verify(dao).save(record);
    }

    @Test
    void failingNotificationDoesNotLoseTheSuspectRecord() {
        org.mockito.Mockito.doThrow(new IllegalStateException("mail server down")).when(notifier)
                .suspectRecordCreated(any(ConsumptionRecord.class));

        ConsumptionRecord record = create(validated(card, null), "650", true);

        assertThat(record.isSuspect()).isTrue();
        verify(dao).save(record);
        verify(notifier).suspectRecordCreated(record);
    }

    @Test
    void sessionWithARecordGetsNoSecondRecord() {
        ConsumptionRecord existing = new ConsumptionRecord();
        existing.setRecordNo("BAY-20260712-000001");
        when(dao.findBySupplySessionId(101L)).thenReturn(existing);

        ConsumptionRecord record = create(validated(card, null), "14.0", true);

        assertThat(record).isSameAs(existing);
        verify(dao, never()).save(any(ConsumptionRecord.class));
    }

    @Test
    void confirmedSuspectRecordIsPricedAndExported() {
        ConsumptionRecord record = create(validated(card, null), "650", true);
        when(dao.findById(9L)).thenReturn(Optional.of(record));
        priced("292.50");

        writeService.confirmRecord(9L, "admin", "pool heater, plausible");

        assertThat(record.getValidationState()).isEqualTo(RecordValidationState.CONFIRMED);
        assertThat(record.getReviewedBy()).isEqualTo("admin");
        assertThat(record.getReviewComment()).isEqualTo("pool heater, plausible");
        assertThat(record.getPricingStatus()).isEqualTo(PricingStatus.PRICED);
        assertThat(record.getCost()).isEqualByComparingTo("292.50");
        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.NEW);
    }

    @Test
    void rejectedRecordIsNeverExported() {
        ConsumptionRecord record = create(validated(card, null), "650", true);
        when(dao.findById(9L)).thenReturn(Optional.of(record));

        writeService.rejectRecord(9L, "admin", "meter replaced");

        assertThat(record.getValidationState()).isEqualTo(RecordValidationState.REJECTED);
        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.IGNORED);
    }

    @Test
    void recordNotInReviewCannotBeReviewed() {
        priced("6.30");
        ConsumptionRecord record = create(validated(card, null), "14.0", true);
        when(dao.findById(9L)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> writeService.rejectRecord(9L, "admin", null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unknownRecordIsNotReviewed() {
        when(dao.findById(9L)).thenReturn(Optional.empty());

        assertThat(writeService.confirmRecord(9L, "admin", null)).isNull();
    }

    @Test
    void confirmedImportedRecordKeepsThePriceOfItsPartner() {
        ConsumptionRecord imported = create(validated(card, null), "650", true);
        imported.setSourceName("PARTNER");
        imported.setCost(new BigDecimal("210.00"));
        imported.setCurrency("EUR");
        when(dao.findById(9L)).thenReturn(Optional.of(imported));

        writeService.confirmRecord(9L, "admin", "partner checked the meter");

        assertThat(imported.getValidationState()).isEqualTo(RecordValidationState.CONFIRMED);
        assertThat(imported.getPricingStatus()).isEqualTo(PricingStatus.PRICED);
        assertThat(imported.getCost()).isEqualByComparingTo("210.00");
        assertThat(imported.getExportStatus()).isEqualTo(ExportStatus.NEW);
        verify(pricingService, never()).calculateCost(any(PricingInput.class));
    }

    @Test
    void recordWithFailedPricingIsPricedAgainAfterTheTariffFixAndGoesToExport() {
        ConsumptionRecord record = create(validated(card, null), null, true);
        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.REVIEW);
        record.setKwh(new BigDecimal("14.0"));
        when(dao.findById(9L)).thenReturn(Optional.of(record));
        priced("6.30");

        writeService.repriceRecord(9L, "office");

        assertThat(record.getPricingStatus()).isEqualTo(PricingStatus.PRICED);
        assertThat(record.getCost()).isEqualByComparingTo("6.30");
        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.NEW);
    }

    @Test
    void suspectImportedRejectedAndExportedRecordsAreNotPricedAgain() {
        ConsumptionRecord suspect = create(validated(card, null), "650", true);
        when(dao.findById(1L)).thenReturn(Optional.of(suspect));
        assertThatThrownBy(() -> writeService.repriceRecord(1L, "office"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("suspect");

        priced("6.30");
        ConsumptionRecord imported = create(validated(card, null), "14.0", true);
        imported.setSourceName("PARTNER");
        when(dao.findById(2L)).thenReturn(Optional.of(imported));
        assertThatThrownBy(() -> writeService.repriceRecord(2L, "office"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("imported");

        ConsumptionRecord exported = create(validated(card, null), "14.0", true);
        exported.setExportStatus(ExportStatus.TRANSFERRED);
        when(dao.findById(3L)).thenReturn(Optional.of(exported));
        assertThatThrownBy(() -> writeService.repriceRecord(3L, "office"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("TRANSFERRED");

        when(dao.findById(4L)).thenReturn(Optional.empty());
        assertThat(writeService.repriceRecord(4L, "office")).isNull();
    }

    @Test
    void onlyAFailedExportIsQueuedAgain() {
        priced("6.30");
        ConsumptionRecord record = create(validated(card, null), "14.0", true);
        when(dao.findById(9L)).thenReturn(Optional.of(record));
        assertThatThrownBy(() -> writeService.requeueExport(9L, "office"))
                .isInstanceOf(IllegalStateException.class);

        record.setExportStatus(ExportStatus.ERROR);
        writeService.requeueExport(9L, "office");

        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.NEW);
        when(dao.findById(10L)).thenReturn(Optional.empty());
        assertThat(writeService.requeueExport(10L, "office")).isNull();
    }

    private ConsumptionRecord create(ValidationResult validationResult, String kwh, boolean billed) {
        BigDecimal consumption = kwh != null ? new BigDecimal(kwh) : null;
        return writeService.createConsumptionRecord(session, validationResult,
                new Consumption(new BigDecimal("100"), consumption != null ? new BigDecimal("100").add(consumption)
                        : null, consumption),
                new SupplyDuration(PLUG_IN, PLUG_OUT, java.time.Duration.between(PLUG_IN, PLUG_OUT).getSeconds()),
                billed, StopReason.UNPLUGGED, PLUG_OUT);
    }

    private ValidationResult validated(CampingCard campingCard, HouseCard houseCard) {
        ValidationResult result = new ValidationResult();
        result.setCard(campingCard);
        result.setHouseCard(houseCard);
        result.setGuest(campingCard != null ? campingCard.getGuest() : null);
        result.setSocket(socket);
        return result;
    }

    private void priced(String cost) {
        PriceCalculation calculation = mock(PriceCalculation.class);
        when(calculation.getStatus()).thenReturn(PricingStatus.PRICED);
        when(calculation.getCost()).thenReturn(new BigDecimal(cost));
        when(calculation.getCurrency()).thenReturn("EUR");
        when(calculation.getTariffId()).thenReturn(1L);
        when(calculation.getTariffName()).thenReturn("STANDARD");
        when(pricingService.calculateCost(any(PricingInput.class))).thenReturn(calculation);
    }

    private static Group group(Long id, GroupType type) {
        Group group = new Group();
        group.setId(id);
        group.setType(type.name());
        return group;
    }
}
