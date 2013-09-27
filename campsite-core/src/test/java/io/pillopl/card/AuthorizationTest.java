package io.pillopl.card;

import io.pillopl.card.ValidationService.ValidationResult;
import io.pillopl.event.Event;
import io.pillopl.event.EventAttribute;
import io.pillopl.event.EventService;
import io.pillopl.event.EventType;
import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import io.pillopl.guest.Guest;
import io.pillopl.guest.GuestType;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.Socket;
import io.pillopl.power.SectionPowerBudget;
import io.pillopl.supply.SupplySessionRegistry;
import io.pillopl.value.SocketLinkId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthorizationTest {

    static final SocketLinkId SOCKET_1 = SocketLinkId.of("HR-BAY-P001", 1);
    static final String RFID = "7001";

    final ValidationService validationService = mock(ValidationService.class);
    final EventService eventService = mock(EventService.class);
    final SectionPowerBudget sectionPowerBudget = mock(SectionPowerBudget.class);
    final SupplySessionRegistry supplySessionRegistry = mock(SupplySessionRegistry.class);
    final HouseCardService houseCardService = mock(HouseCardService.class);
    final Authorization authorization = new Authorization(validationService, eventService, sectionPowerBudget,
            supplySessionRegistry, houseCardService, mock(PedestalService.class));

    final Socket socket = new Socket();
    final CampingCard card = new CampingCard.Builder(RFID, "HR-1")
            .guest(guestNamed("Ana", "Horvat"))
            .validTo(LocalDate.of(2026, 7, 14))
            .build();

    @BeforeEach
    void setUp() {
        socket.setId(11L);
        when(sectionPowerBudget.isWithinSectionBudget(SOCKET_1)).thenReturn(true);
    }

    @Test
    void refusedCardIsAnsweredWithTheValidationStatus() {
        validated(ValidationStatus.EXPIRED, card, null);

        AuthorizationResult result = authorization.authenticate(RFID, SOCKET_1);

        assertThat(result.getStatus()).isEqualTo(ValidationStatus.EXPIRED);
        assertThat(result.getExpiryDate()).isEqualTo(LocalDate.of(2026, 7, 14));
        verify(supplySessionRegistry, never()).createSession(any(), any());
        assertThat(sentTypes()).containsExactly(EventType.AUTHORIZATION);
    }

    @Test
    void cardNotAcceptedAtTheSiteTriggersTheWelcomeCardAdoptionCheck() {
        validated(ValidationStatus.CARD_NOT_ACCEPTED_AT_SITE, card, null);

        AuthorizationResult result = authorization.authenticate(RFID, SOCKET_1);

        assertThat(result.getStatus()).isEqualTo(ValidationStatus.CARD_NOT_ACCEPTED_AT_SITE);
        List<Event> events = sent();
        assertThat(events).extracting(Event::getType)
                .containsExactly(EventType.CHECK_WELCOME_CARD_ADOPTION, EventType.AUTHORIZATION);
        assertThat(EventAttribute.PEDESTAL_CODE.getStringValue(events.get(0))).isEqualTo("HR-BAY-P001");
    }

    @Test
    void houseCardIsAcceptedWithoutSessionAndBudgetCheckAndItsUseIsRecorded() {
        HouseCard houseCard = new HouseCard(HouseCardType.RENTAL, RFID, "R1");
        validated(ValidationStatus.ACCEPTED, null, houseCard);
        when(supplySessionRegistry.hasConcurrentSession(SOCKET_1, RFID)).thenReturn(true);

        AuthorizationResult result = authorization.authenticate(RFID, SOCKET_1);

        assertThat(result.getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
        verify(houseCardService).recordUse(houseCard);
        verify(sectionPowerBudget, never()).isWithinSectionBudget(any());
        verify(supplySessionRegistry, never()).createSession(any(), any());
    }

    @Test
    void socketSupplyingForAnotherCardIsOccupied() {
        validated(ValidationStatus.ACCEPTED, card, null);
        when(supplySessionRegistry.hasConcurrentSession(SOCKET_1, RFID)).thenReturn(true);

        assertThat(authorization.authenticate(RFID, SOCKET_1).getStatus()).isEqualTo(ValidationStatus.SOCKET_OCCUPIED);
        verify(supplySessionRegistry, never()).createSession(any(), any());
    }

    @Test
    void sectionWithoutPowerBudgetLeftMakesTheSocketUnavailable() {
        validated(ValidationStatus.ACCEPTED, card, null);
        when(sectionPowerBudget.isWithinSectionBudget(SOCKET_1)).thenReturn(false);

        assertThat(authorization.authenticate(RFID, SOCKET_1).getStatus()).isEqualTo(ValidationStatus.UNAVAILABLE);
        verify(supplySessionRegistry, never()).createSession(any(), any());
    }

    @Test
    void acceptedCardOpensTheSessionAndTellsThePedestalTheGuestAndExpiry() {
        validated(ValidationStatus.ACCEPTED, card, null);

        AuthorizationResult result = authorization.authenticate(RFID, SOCKET_1);

        assertThat(result.isAccepted()).isTrue();
        assertThat(result.getGuestName()).isEqualTo("Ana Horvat");
        assertThat(result.getExpiryDate()).isEqualTo(LocalDate.of(2026, 7, 14));
        verify(supplySessionRegistry).createSession(SOCKET_1, RFID);
        Event event = sent().get(0);
        assertThat(EventAttribute.SOCKET_ID.getLongValue(event)).isEqualTo(11L);
        assertThat(EventAttribute.AUTHORIZATION_RESULT.getStringValue(event)).isEqualTo("ACCEPTED");
    }

    @Test
    void failingEventQueueDoesNotChangeTheAnswer() {
        validated(ValidationStatus.ACCEPTED, card, null);
        doThrow(new IllegalStateException("queue down")).when(eventService).send(any());

        assertThat(authorization.authenticate(RFID, SOCKET_1).getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
    }

    @Test
    void pedestalWideAuthorizationOnlyAnswersTheValidation() {
        ValidationResult result = new ValidationResult.Builder().validationStatus(ValidationStatus.ACCEPTED)
                .card(card).build();
        when(validationService.validateForNow("HR-BAY-P001", RFID)).thenReturn(result);

        AuthorizationResult answer = authorization.authenticate(RFID, "HR-BAY-P001");

        assertThat(answer.getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
        verify(supplySessionRegistry, never()).hasConcurrentSession(any(), any());
        verify(supplySessionRegistry, never()).createSession(any(), any());
        assertThat(sentTypes()).containsExactly(EventType.AUTHORIZATION);
    }

    @Test
    void pedestalWideAuthorizationRecordsTheUseOfAHouseCard() {
        HouseCard houseCard = new HouseCard(HouseCardType.STAFF, RFID, "S1");
        when(validationService.validateForNow("HR-BAY-P001", RFID)).thenReturn(new ValidationResult.Builder()
                .validationStatus(ValidationStatus.ACCEPTED).houseCard(houseCard).build());

        authorization.authenticate(RFID, "HR-BAY-P001");

        verify(houseCardService, times(1)).recordUse(houseCard);
    }

    private void validated(ValidationStatus status, CampingCard card, HouseCard houseCard) {
        ValidationResult result = new ValidationResult.Builder().validationStatus(status).card(card)
                .houseCard(houseCard).socket(socket).build();
        when(validationService.validateForNow(SOCKET_1, RFID)).thenReturn(result);
    }

    private List<Event> sent() {
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService, org.mockito.Mockito.atLeastOnce()).send(captor.capture());
        return captor.getAllValues();
    }

    private List<EventType> sentTypes() {
        return sent().stream().map(Event::getType).toList();
    }

    private static Guest guestNamed(String firstName, String name) {
        Guest guest = new Guest(new Group(GroupType.ISSUER, "NETWORK CARD"), GuestType.TRANSIT, name);
        guest.setFirstName(firstName);
        return guest;
    }
}
