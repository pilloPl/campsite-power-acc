package io.pillopl.supply;

import io.pillopl.card.CampingCard;
import io.pillopl.card.HouseCard;
import io.pillopl.card.HouseCardType;
import io.pillopl.card.ValidationService;
import io.pillopl.card.ValidationService.ValidationResult;
import io.pillopl.card.ValidationStatus;
import io.pillopl.config.SystemPropertyService;
import io.pillopl.consumption.ConsumptionRecordWriteService;
import io.pillopl.event.Event;
import io.pillopl.event.EventAttribute;
import io.pillopl.event.EventService;
import io.pillopl.event.EventType;
import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import io.pillopl.guest.Guest;
import io.pillopl.guest.GuestType;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.SupplyIncidentDao;
import io.pillopl.pricing.PricingService;
import io.pillopl.supply.SupplyIncident.IncidentType;
import io.pillopl.util.DateService;
import io.pillopl.value.SocketLinkId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplyControlUnitImplTest {

    static final String PEDESTAL = "HR-BAY-P001";
    static final SocketLinkId SOCKET_1 = SocketLinkId.of(PEDESTAL, 1);
    static final String RFID = "7001";
    static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 10, 18, 0);
    static final LocalDateTime PLUG_IN = LocalDateTime.of(2026, 7, 10, 9, 30);

    final DateService dateService = new DateService();
    final SupplySessionManager sessionManager = mock(SupplySessionManager.class);
    final ValidationService validationService = mock(ValidationService.class);
    final PedestalService pedestalService = mock(PedestalService.class);
    final PricingService pricingService = mock(PricingService.class);
    final EventService eventService = mock(EventService.class);
    final SupplyIncidentDao supplyIncidentDao = mock(SupplyIncidentDao.class);
    final SystemPropertyService systemPropertyService = mock(SystemPropertyService.class);
    @SuppressWarnings("unchecked")
    final ObjectProvider<CheckedOutStayFinder> stayFinderProvider = mock(ObjectProvider.class);
    final CheckedOutStayFinder stayFinder = mock(CheckedOutStayFinder.class);
    final ConsumptionRecordWriteService consumptionRecordWriteService = mock(ConsumptionRecordWriteService.class);

    final SupplyControlUnitImpl controlUnit = new SupplyControlUnitImpl(dateService, sessionManager,
            validationService, pedestalService, pricingService, eventService, supplyIncidentDao,
            systemPropertyService, stayFinderProvider, consumptionRecordWriteService,
            mock(PlatformTransactionManager.class));

    final Pedestal pedestal = new Pedestal();
    final Socket socket = new Socket();
    final CampingCard card = new CampingCard.Builder(RFID, "HR-1")
            .guest(guestNamed("Ana", "Horvat"))
            .validTo(LocalDate.of(2026, 7, 14))
            .build();

    @BeforeEach
    void setUp() {
        dateService.setClock(Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));
        pedestal.setCode(PEDESTAL);
        pedestal.setBillable(true);
        socket.setId(11L);
        socket.setSocketNo(1);
        socket.setPedestal(pedestal);
        socket.setMeterKwh(new BigDecimal("1250.000"));
        card.setId(21L);
        when(systemPropertyService.getStaleSessionTimeoutInHours()).thenReturn(24);
        when(systemPropertyService.getCheckoutSessionCloseDelayInMinutes()).thenReturn(30);
    }

    // ------------------------------------------------------------------------------------------ start

    @Test
    void refusedCardGetsTransactionIdZeroAndNoSessionIsCreated() {
        validatedForStart(ValidationStatus.EXPIRED, card, null);

        SupplyResponse response = controlUnit.startSupply(SOCKET_1, RFID, start());

        assertThat(response.getStatus()).isEqualTo(ValidationStatus.EXPIRED);
        assertThat(response.getTransactionId()).isZero();
        assertThat(response.getExpiryDate()).isEqualTo(LocalDate.of(2026, 7, 14));
        verify(sessionManager, never()).createNewSessionFor(any(), any(), any(), any());
        verify(eventService, never()).send(any());
    }

    @Test
    void acceptedStartCreatesTheSessionAndAnswersCurrentPriceAndGreeting() {
        ValidationResult result = validatedForStart(ValidationStatus.ACCEPTED, card, null);
        SupplySession session = session(card, null, true);
        when(sessionManager.createNewSessionFor(result, null, new BigDecimal("1250.000"), PLUG_IN))
                .thenReturn(session);
        when(pricingService.getPriceInformation(11L, 21L, PLUG_IN)).thenReturn("0.45 EUR/kWh");

        SupplyResponse response = controlUnit.startSupply(SOCKET_1, RFID, start());

        assertThat(response.getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
        assertThat(response.getTransactionId()).isEqualTo(1001);
        assertThat(response.getMaxCurrentA()).isEqualTo(16);
        assertThat(response.getPriceInformation()).isEqualTo("0.45 EUR/kWh");
        assertThat(response.getGreeting()).isEqualTo("Ana Horvat");
        Event event = sentEvent();
        assertThat(event.getType()).isEqualTo(EventType.SUPPLY_STARTED);
        assertThat(EventAttribute.TRANSACTION_ID.getIntegerValue(event)).isEqualTo(1001);
    }

    @Test
    void repeatedStartFindsItsSessionAgainAndIsNotAnnouncedTwice() {
        validatedForStart(ValidationStatus.ACCEPTED, card, null);
        when(sessionManager.findSessionFor(SOCKET_1, RFID, new BigDecimal("1250.000"), PLUG_IN, false))
                .thenReturn(session(card, null, true));

        SupplyResponse response = controlUnit.startSupply(SOCKET_1, RFID, start());

        assertThat(response.getTransactionId()).isEqualTo(1001);
        verify(sessionManager, never()).createNewSessionFor(any(), any(), any(), any());
        verify(eventService, never()).send(any());
    }

    @Test
    void houseCardSupplyGetsNoPriceAndNoGreeting() {
        HouseCard houseCard = new HouseCard(HouseCardType.STAFF, RFID, "S1");
        ValidationResult result = validatedForStart(ValidationStatus.ACCEPTED, null, houseCard);
        when(sessionManager.createNewSessionFor(any(), any(), any(), any())).thenReturn(session(null, houseCard, true));

        SupplyResponse response = controlUnit.startSupply(SOCKET_1, RFID, start());

        assertThat(response.isAccepted()).isTrue();
        assertThat(response.getPriceInformation()).isNull();
        assertThat(response.getGreeting()).isNull();
        verify(sessionManager).findSessionFor(SOCKET_1, RFID, new BigDecimal("1250.000"), PLUG_IN, true);
        verify(sessionManager).createNewSessionFor(result, null, new BigDecimal("1250.000"), PLUG_IN);
    }

    @Test
    void startDateOfAControllerAfterPowerLossIsReplacedByTheSystemDate() {
        when(validationService.validateFor(eq(SOCKET_1), eq(RFID), eq(NOW), eq(false)))
                .thenReturn(result(ValidationStatus.CARD_INVALID, null, null));

        controlUnit.startSupply(SOCKET_1, RFID, SupplyRequest.start(BigDecimal.TEN,
                LocalDateTime.of(1970, 1, 1, 0, 4)));

        verify(validationService).validateFor(SOCKET_1, RFID, NOW, false);
    }

    @Test
    void onlyDatesOf1970AreReplaced() {
        assertThat(SupplyControlUnitImpl.adjustEpochZeroAfterPowerLoss(LocalDateTime.of(1970, 3, 2, 10, 0), NOW))
                .isEqualTo(NOW);
        assertThat(SupplyControlUnitImpl.adjustEpochZeroAfterPowerLoss(null, NOW)).isEqualTo(NOW);
        assertThat(SupplyControlUnitImpl.adjustEpochZeroAfterPowerLoss(PLUG_IN, NOW)).isEqualTo(PLUG_IN);
    }

    // ------------------------------------------------------------------------------------------ stop

    @Test
    void stopWithoutTransactionIdIsAcceptedAndLoggedAsIncident() {
        SupplyResponse response = controlUnit.stopSupply(PEDESTAL, RFID, SupplyRequest.stop(null,
                new BigDecimal("1262.5"), NOW, StopReason.LOCAL));

        assertThat(response.getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
        assertThat(loggedIncident().getIncidentType()).isEqualTo(IncidentType.NO_TRANSACTION_ID);
    }

    @Test
    void stopOfAnUnknownTransactionIsAcceptedAndLoggedAsIncident() {
        SupplyResponse response = controlUnit.stopSupply(PEDESTAL, RFID, stop(4711));

        assertThat(response.getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
        SupplyIncident incident = loggedIncident();
        assertThat(incident.getIncidentType()).isEqualTo(IncidentType.NO_SESSION);
        assertThat(incident.getTransactionId()).isEqualTo(4711);
        assertThat(incident.getPedestalCode()).isEqualTo(PEDESTAL);
        assertThat(incident.getMeterKwh()).isEqualByComparingTo("1262.5");
        verify(sessionManager, never()).deactivate(any(), any(), any());
        verify(consumptionRecordWriteService, never()).createConsumptionRecord(any(), any(), any(), any(),
                anyBoolean(), any(), any());
    }

    @Test
    void stopOfASessionClosedByTheSystemIsAcceptedAndLoggedAsIncident() {
        SupplySession closed = session(card, null, false);
        closed.setStopReason(StopReason.TIMEOUT);
        when(sessionManager.findSessionFor(PEDESTAL, 1001)).thenReturn(closed);

        SupplyResponse response = controlUnit.stopSupply(PEDESTAL, RFID, stop(1001));

        assertThat(response.getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
        SupplyIncident incident = loggedIncident();
        assertThat(incident.getIncidentType()).isEqualTo(IncidentType.SESSION_ALREADY_CLOSED);
        assertThat(incident.getSupplySessionId()).isEqualTo(31L);
    }

    @Test
    void stopClosesTheSessionWithTheReasonAndStoresTheStopReading() {
        SupplySession session = session(card, null, true);
        when(sessionManager.findSessionFor(PEDESTAL, 1001)).thenReturn(session);
        validatedForStop(ValidationStatus.ACCEPTED, card, null);

        SupplyResponse response = controlUnit.stopSupply(PEDESTAL, RFID, SupplyRequest.stop(1001,
                new BigDecimal("1262.5"), NOW, StopReason.UNPLUGGED));

        assertThat(response.getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
        assertThat(response.getExpiryDate()).isEqualTo(LocalDate.of(2026, 7, 14));
        verify(sessionManager).deactivate(session, StopReason.UNPLUGGED, NOW);
        verify(pedestalService).updateMeterReading(socket, new BigDecimal("1262.5"), NOW);
        Event event = sentEvent();
        assertThat(event.getType()).isEqualTo(EventType.SUPPLY_STOPPED);
        assertThat(EventAttribute.STOP_REASON.getStringValue(event)).isEqualTo("UNPLUGGED");
        assertThat(event.getAttribute(EventAttribute.BILLABLE)).isEqualTo(true);
        assertThat(event.getAttribute(EventAttribute.START_TIME)).isEqualTo(PLUG_IN);
        verify(supplyIncidentDao, never()).save(any());
        verify(consumptionRecordWriteService).createConsumptionRecord(eq(session), any(), any(), any(), eq(true),
                eq(StopReason.UNPLUGGED), eq(NOW));
    }

    @Test
    void stopWithAnotherCardIsAnsweredWithTheStatusOfThatCard() {
        when(sessionManager.findSessionFor(PEDESTAL, 1001)).thenReturn(session(card, null, true));
        validatedForStop(ValidationStatus.ACCEPTED, card, null);
        when(validationService.validateFor(SOCKET_1, "9999", null, false))
                .thenReturn(result(ValidationStatus.CARD_NOT_ACCEPTED_AT_SITE, null, null));

        SupplyResponse response = controlUnit.stopSupply(PEDESTAL, "9999", stop(1001));

        assertThat(response.getStatus()).isEqualTo(ValidationStatus.CARD_NOT_ACCEPTED_AT_SITE);
        verify(sessionManager).deactivate(any(), eq(StopReason.LOCAL), eq(NOW));
    }

    @Test
    void stopOfAHouseCardSessionIsBookedAsNotBilled() {
        HouseCard houseCard = new HouseCard(HouseCardType.STAFF, RFID, "S1");
        SupplySession session = session(null, houseCard, true);
        when(sessionManager.findSessionFor(PEDESTAL, 1001)).thenReturn(session);
        validatedForStop(ValidationStatus.ACCEPTED, null, houseCard);

        SupplyResponse response = controlUnit.stopSupply(PEDESTAL, RFID, stop(1001));

        assertThat(response.getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
        assertThat(response.getExpiryDate()).isNull();
        assertThat(sentEvent().getAttribute(EventAttribute.BILLABLE)).isEqualTo(false);
        verify(consumptionRecordWriteService).createConsumptionRecord(eq(session), any(), any(), any(), eq(false),
                eq(StopReason.LOCAL), eq(NOW));
    }

    @Test
    void consumptionOfAHouseCardIsNotBilled() {
        ValidationResult result = result(ValidationStatus.ACCEPTED, null, new HouseCard(HouseCardType.STAFF, RFID, "S1"));

        assertThat(controlUnit.shouldConsumptionBeBilled(result)).isFalse();
    }

    @Test
    void consumptionOnANotBillablePedestalIsNotBilled() {
        pedestal.setBillable(false);

        assertThat(controlUnit.shouldConsumptionBeBilled(result(ValidationStatus.ACCEPTED, card, null))).isFalse();
    }

    @Test
    void consumptionOnAFreeSocketIsNotBilled() {
        ValidationResult result = result(ValidationStatus.ACCEPTED, card, null);
        result.setFreeSocket(true);

        assertThat(controlUnit.shouldConsumptionBeBilled(result)).isFalse();
        assertThat(controlUnit.shouldConsumptionBeBilled(result(ValidationStatus.ACCEPTED, card, null))).isTrue();
    }

    @Test
    void invalidMeterValueIsReplacedByTheLastReadingOfTheSocketAndLeavesTheConsumptionOpen() {
        Consumption consumption = controlUnit.determineConsumption(socket, SupplyRequest.stop(1001, null, NOW,
                null).withStartOf(session(card, null, true)));

        assertThat(consumption.stopMeterKwh()).isEqualByComparingTo("1250.000");
        assertThat(consumption.isKnown()).isFalse();
    }

    @Test
    void stopReadingBelowTheStartReadingGivesANegativeConsumption() {
        Consumption consumption = controlUnit.determineConsumption(socket, SupplyRequest.stop(1001,
                new BigDecimal("1200"), NOW, null).withStartOf(session(card, null, true)));

        assertThat(consumption.consumptionKwh()).isEqualByComparingTo("-50");
        assertThat(consumption.isNegative()).isTrue();
    }

    @Test
    void durationRunsFromThePlugInOfTheSessionToThePlugOut() {
        SupplySession session = session(card, null, true);

        SupplyDuration duration = controlUnit.determineDuration(SupplyRequest.stop(1001, BigDecimal.ONE,
                LocalDateTime.of(1970, 1, 1, 0, 2), null), session, NOW);

        assertThat(duration.plugIn()).isEqualTo(PLUG_IN);
        assertThat(duration.plugOut()).isEqualTo(NOW);
        assertThat(duration.durationInSeconds()).isEqualTo(8 * 3600 + 30 * 60);
    }

    // ------------------------------------------------------------------------------------------ stale sessions

    @Test
    void sessionOlderThanTheTimeoutIsClosedWithTheLastReadingOfTheSocket() {
        SupplySession stale = session(card, null, true);
        socket.setModDate(LocalDateTime.of(2026, 7, 10, 12, 0));
        when(sessionManager.findStaleSessions(NOW, 24)).thenReturn(List.of(stale));
        when(sessionManager.getSession(31L)).thenReturn(stale);
        validatedForStop(ValidationStatus.ACCEPTED, card, null);

        int closed = controlUnit.closeStaleSessions();

        assertThat(closed).isEqualTo(1);
        verify(sessionManager).deactivate(stale, StopReason.TIMEOUT, LocalDateTime.of(2026, 7, 10, 12, 0));
        Event event = sentEvent();
        assertThat(EventAttribute.STOP_REASON.getStringValue(event)).isEqualTo("TIMEOUT");
        assertThat(event.getAttribute(EventAttribute.STOP_METER_KWH)).isEqualTo(new BigDecimal("1250.000"));
        verify(consumptionRecordWriteService).createConsumptionRecord(eq(stale), any(), any(), any(), eq(true),
                eq(StopReason.TIMEOUT), eq(NOW));
    }

    @Test
    void sessionOfACheckedOutStayIsClosedWithCheckout() {
        SupplySession ofCheckedOutStay = session(card, null, true);
        ofCheckedOutStay.setStayId(501L);
        SupplySession ofOngoingStay = session(card, null, true);
        ofOngoingStay.setId(32L);
        ofOngoingStay.setStayId(502L);
        when(stayFinderProvider.getIfAvailable()).thenReturn(stayFinder);
        when(sessionManager.findActiveSessionsWithStay()).thenReturn(List.of(ofCheckedOutStay, ofOngoingStay));
        when(stayFinder.findCheckedOutStayIds(Set.of(501L, 502L), NOW.minusMinutes(30))).thenReturn(Set.of(501L));
        when(sessionManager.getSession(31L)).thenReturn(ofCheckedOutStay);
        validatedForStop(ValidationStatus.ACCEPTED, card, null);

        assertThat(controlUnit.closeStaleSessions()).isEqualTo(1);
        verify(sessionManager).deactivate(ofCheckedOutStay, StopReason.CHECKOUT, NOW);
        verify(sessionManager, never()).getSession(32L);
    }

    @Test
    void withoutStaysNoSessionIsClosedOnCheckout() {
        assertThat(controlUnit.closeCheckedOutSessions(NOW)).isZero();
        verify(sessionManager, never()).findActiveSessionsWithStay();
    }

    @Test
    void sessionAlreadyStoppedByThePedestalIsNotClosedAgain() {
        SupplySession stale = session(card, null, true);
        when(sessionManager.findStaleSessions(NOW, 24)).thenReturn(List.of(stale));
        when(sessionManager.getSession(31L)).thenReturn(session(card, null, false));

        assertThat(controlUnit.closeStaleSessions()).isZero();
        verify(sessionManager, never()).deactivate(any(), any(), any());
    }

    @Test
    void readingOutsideTheSessionIsNotItsStopDate() {
        SupplySession session = session(card, null, true);
        socket.setModDate(PLUG_IN.minusHours(1));

        assertThat(SupplyControlUnitImpl.lastReadingDate(session, socket, NOW)).isEqualTo(NOW);
    }

    // ------------------------------------------------------------------------------------------ helpers

    private SupplyRequest start() {
        return SupplyRequest.start(new BigDecimal("1250.000"), PLUG_IN);
    }

    private SupplyRequest stop(Integer transactionId) {
        return SupplyRequest.stop(transactionId, new BigDecimal("1262.5"), NOW, null);
    }

    private ValidationResult validatedForStart(ValidationStatus status, CampingCard card, HouseCard houseCard) {
        ValidationResult result = result(status, card, houseCard);
        when(validationService.validateFor(SOCKET_1, RFID, PLUG_IN, false)).thenReturn(result);
        return result;
    }

    private void validatedForStop(ValidationStatus status, CampingCard card, HouseCard houseCard) {
        when(validationService.validateFor(eq(SOCKET_1), eq(RFID), isNull(), anyBoolean()))
                .thenReturn(result(status, card, houseCard));
    }

    private ValidationResult result(ValidationStatus status, CampingCard card, HouseCard houseCard) {
        ValidationResult result = new ValidationResult.Builder().validationStatus(status).card(card)
                .houseCard(houseCard).socket(socket).build();
        result.setGuest(card != null ? card.getGuest() : null);
        return result;
    }

    private SupplySession session(CampingCard card, HouseCard houseCard, boolean active) {
        SupplySession session = new SupplySession();
        session.setId(31L);
        session.setSocket(socket);
        session.setCard(card);
        session.setHouseCard(houseCard);
        session.setTransactionId(1001);
        session.setStartMeterKwh(new BigDecimal("1250.000"));
        session.setRemoteDate(PLUG_IN);
        session.setCreateDate(PLUG_IN);
        session.setMaxCurrentA(16);
        session.setActive(active);
        return session;
    }

    private SupplyIncident loggedIncident() {
        ArgumentCaptor<SupplyIncident> captor = ArgumentCaptor.forClass(SupplyIncident.class);
        verify(supplyIncidentDao).save(captor.capture());
        return captor.getValue();
    }

    private Event sentEvent() {
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(captor.capture());
        return captor.getValue();
    }

    private static Guest guestNamed(String firstName, String name) {
        Guest guest = new Guest(new Group(GroupType.ISSUER, "NETWORK CARD"), GuestType.TRANSIT, name);
        guest.setFirstName(firstName);
        return guest;
    }
}
