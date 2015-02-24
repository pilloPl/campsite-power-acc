package io.pillopl.card;

import io.pillopl.card.ValidationService.ValidationResult;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.guest.Guest;
import io.pillopl.guest.GuestType;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.util.DateService;
import io.pillopl.value.SocketLinkId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ValidationServiceTest {

    static final String PEDESTAL = "HR-BAY-P001";
    static final SocketLinkId SOCKET_1 = SocketLinkId.of(PEDESTAL, 1);
    static final LocalDateTime DURING_STAY = LocalDateTime.of(2026, 7, 10, 12, 0);

    final PedestalService pedestalService = mock(PedestalService.class);
    final CampingCardDao campingCardDao = mock(CampingCardDao.class);
    final GroupService groupService = mock(GroupService.class);
    final HouseCardService houseCardService = mock(HouseCardService.class);
    final DateService dateService = new DateService();
    @SuppressWarnings("unchecked")
    final ObjectProvider<QrPaySupplyAuthorizer> qrPayProvider = mock(ObjectProvider.class);
    final QrPaySupplyAuthorizer qrPayAuthorizer = mock(QrPaySupplyAuthorizer.class);
    final ValidationService validationService = new ValidationService(pedestalService, campingCardDao, dateService,
            groupService, houseCardService, qrPayProvider);

    final Pedestal pedestal = new Pedestal();
    final Socket socket = new Socket();
    final Guest guest = new Guest(new Group(GroupType.ISSUER, "NETWORK CARD"), GuestType.TRANSIT, "Horvat");

    @BeforeEach
    void setUp() {
        pedestal.setId(1L);
        pedestal.setCode(PEDESTAL);
        socket.setPedestal(pedestal);
        socket.setSocketNo(1);
        when(pedestalService.findByCode(PEDESTAL)).thenReturn(pedestal);
        when(pedestalService.findSocket(PEDESTAL, 1)).thenReturn(socket);
        when(qrPayProvider.getIfAvailable()).thenReturn(qrPayAuthorizer);
        dateService.setClock(Clock.fixed(DURING_STAY.atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()));
    }

    @Test
    void unknownSocketIsInvalid() {
        ValidationResult result = validationService.validateFor(SocketLinkId.of(PEDESTAL, 3), "7001", DURING_STAY,
                false);

        assertThat(result.getStatus()).isEqualTo(ValidationStatus.SOCKET_INVALID);
        verify(houseCardService, never()).getHouseCardForPedestal(any(), any());
    }

    @Test
    void unknownOrDeletedPedestalIsInvalid() {
        assertThat(validationService.validateForNow("HR-NONE-P001", "7001").getStatus())
                .isEqualTo(ValidationStatus.SOCKET_INVALID);

        pedestal.setDeleted(true);

        assertThat(validationService.validateForNow(SOCKET_1, "7001").getStatus())
                .isEqualTo(ValidationStatus.SOCKET_INVALID);
    }

    @Test
    void houseCardIsAcceptedWithoutFurtherValidation() {
        HouseCard houseCard = new HouseCard(HouseCardType.STAFF, "7001", "S1");
        when(houseCardService.getHouseCardForPedestal(PEDESTAL, "7001")).thenReturn(houseCard);

        ValidationResult result = validationService.validateFor(SOCKET_1, "7001", DURING_STAY, false);

        assertThat(result.getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
        assertThat(result.isHouseCardAccepted()).isTrue();
        assertThat(result.getSocket()).isEqualTo(socket);
        verify(campingCardDao, never()).findByCardRfid(any());
    }

    @Test
    void unknownCardIsInvalid() {
        ValidationResult result = validationService.validateFor(SOCKET_1, "7002", DURING_STAY, false);

        assertThat(result.getStatus()).isEqualTo(ValidationStatus.CARD_INVALID);
        assertThat(result.getCard()).isNull();
    }

    @Test
    void cardNotAcceptedOnTheCampsiteIsRefused() {
        card("7003", stayCard());
        when(groupService.isCardAcceptedAt(anyLong(), any(CampingCard.class))).thenReturn(false);

        assertThat(validationService.validateFor(SOCKET_1, "7003", DURING_STAY, false).getStatus())
                .isEqualTo(ValidationStatus.CARD_NOT_ACCEPTED_AT_SITE);
    }

    @Test
    void acceptedValidCardIsAccepted() {
        CampingCard card = card("7004", stayCard());
        when(groupService.isCardAcceptedAt(1L, card)).thenReturn(true);

        ValidationResult result = validationService.validateForNow(SOCKET_1, "7004");

        assertThat(result.getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
        assertThat(result.getCard()).isEqualTo(card);
        assertThat(result.getGuest()).isEqualTo(guest);
        assertThat(result.isCheckStatus()).isFalse();
        assertThat(result.isFreeSocket()).isFalse();
    }

    @Test
    void cardIsValidatedAtTheRemoteDateOfThePedestal() {
        CampingCard card = card("7005", stayCard());
        when(groupService.isCardAcceptedAt(1L, card)).thenReturn(true);

        assertThat(validationService.validateFor(SOCKET_1, "7005", LocalDateTime.of(2026, 8, 1, 9, 0), false)
                .getStatus()).isEqualTo(ValidationStatus.EXPIRED);
    }

    @Test
    void deactivatedCardIsBlockedAndCardWithoutBillingAccountInvalid() {
        CampingCard blocked = card("7006", stayCard().activation(CardActivation.DEACTIVATED));
        CampingCard noAccount = card("7007", stayCard().billingAccountNo(null));
        when(groupService.isCardAcceptedAt(anyLong(), any(CampingCard.class))).thenReturn(true);

        assertThat(validationService.validateFor(SOCKET_1, "7006", DURING_STAY, false).getStatus())
                .isEqualTo(ValidationStatus.BLOCKED);
        assertThat(validationService.validateFor(SOCKET_1, "7007", DURING_STAY, false).getStatus())
                .isEqualTo(ValidationStatus.CARD_INVALID);
        assertThat(blocked).isNotEqualTo(noAccount);
    }

    @Test
    void runningSupplyOnlyNeedsAGuestToBill() {
        card("7008", stayCard().activation(CardActivation.DEACTIVATED));
        card("7009", stayCard().guest(null));

        ValidationResult withGuest = validationService.validateFor(SOCKET_1, "7008", DURING_STAY, true);
        ValidationResult withoutGuest = validationService.validateFor(SOCKET_1, "7009", DURING_STAY, true);

        assertThat(withGuest.getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
        assertThat(withGuest.isCheckStatus()).isTrue();
        assertThat(withoutGuest.getStatus()).isEqualTo(ValidationStatus.CARD_INVALID);
        verify(groupService, never()).isCardAcceptedAt(anyLong(), any(CampingCard.class));
    }

    @Test
    void runningSupplyOnAFreeSocketIsMarkedFree() {
        card("7010", stayCard());
        socket.setFreeSocket(true);

        ValidationResult result = validationService.validateFor(SOCKET_1, "7010", DURING_STAY, true);

        assertThat(result.getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
        assertThat(result.isFreeSocket()).isTrue();
    }

    @Test
    void pedestalWideValidationKnowsNoFreeSocket() {
        card("7011", stayCard());

        assertThat(validationService.validateForNow(PEDESTAL, "7011").isFreeSocket()).isFalse();
    }

    @Test
    void remoteSupplyStartOfAQrPaymentIsAcceptedWithoutCard() {
        when(qrPayAuthorizer.isSupplyAuthorized(1005L, socket, false)).thenReturn(true);

        ValidationResult result = validationService.validateFor(SOCKET_1, "QR1005", DURING_STAY, false);

        assertThat(result.getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
        assertThat(result.isQrPayAccepted()).isTrue();
        assertThat(result.getQrPayProcessId()).isEqualTo(1005L);
        assertThat(result.getCard()).isNull();
        verify(campingCardDao, never()).findByCardRfid(any());
    }

    @Test
    void qrPaymentAuthTagOfAnotherSocketOrStateIsInvalid() {
        when(qrPayAuthorizer.isSupplyAuthorized(1005L, socket, false)).thenReturn(false);

        ValidationResult result = validationService.validateFor(SOCKET_1, "QR1005", DURING_STAY, false);

        assertThat(result.getStatus()).isEqualTo(ValidationStatus.CARD_INVALID);
        assertThat(result.isQrPayAccepted()).isFalse();
    }

    @Test
    void stopOfAQrPaymentSupplyOnlyNeedsTheProcess() {
        when(qrPayAuthorizer.isSupplyAuthorized(1005L, socket, true)).thenReturn(true);

        assertThat(validationService.validateFor(SOCKET_1, "QR1005", null, true).isQrPayAccepted()).isTrue();
    }

    @Test
    void withoutQrPaymentQrAuthTagsAreInvalid() {
        when(qrPayProvider.getIfAvailable()).thenReturn(null);

        assertThat(validationService.validateFor(SOCKET_1, "QR1005", DURING_STAY, false).getStatus())
                .isEqualTo(ValidationStatus.CARD_INVALID);
    }

    @Test
    void everyCardStatusHasAnAnswerForThePedestal() {
        assertThat(ValidationService.translate(CardStatus.VALID)).isEqualTo(ValidationStatus.ACCEPTED);
        assertThat(ValidationService.translate(CardStatus.NOT_VALID_YET)).isEqualTo(ValidationStatus.EXPIRED);
        assertThat(ValidationService.translate(CardStatus.NOT_VALID_ANYMORE)).isEqualTo(ValidationStatus.EXPIRED);
        assertThat(ValidationService.translate(CardStatus.CARD_DEACTIVATED)).isEqualTo(ValidationStatus.BLOCKED);
        assertThat(ValidationService.translate(CardStatus.GUEST_DEACTIVATED)).isEqualTo(ValidationStatus.BLOCKED);
        assertThat(ValidationService.translate(CardStatus.NO_GUEST)).isEqualTo(ValidationStatus.CARD_INVALID);
        assertThat(ValidationService.translate(CardStatus.BILLING_ACCOUNT_INVALID))
                .isEqualTo(ValidationStatus.CARD_INVALID);
    }

    private CampingCard.Builder stayCard() {
        return new CampingCard.Builder("x", "x").guest(guest).validFrom(LocalDate.of(2026, 7, 1))
                .validTo(LocalDate.of(2026, 7, 14)).billingAccountNo("400012345678");
    }

    private CampingCard card(String rfid, CampingCard.Builder builder) {
        CampingCard card = builder.build();
        card.setCardRfid(rfid);
        when(campingCardDao.findByCardRfid(rfid)).thenReturn(card);
        return card;
    }
}
