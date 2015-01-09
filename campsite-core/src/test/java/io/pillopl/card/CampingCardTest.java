package io.pillopl.card;

import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import io.pillopl.guest.Guest;
import io.pillopl.guest.GuestStatus;
import io.pillopl.guest.GuestType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CampingCardTest {

    static final LocalDateTime NOON_JULY_10 = LocalDateTime.of(2026, 7, 10, 12, 0);

    final Guest guest = new Guest(new Group(GroupType.ISSUER, "NETWORK CARD"), GuestType.TRANSIT, "Horvat");

    CampingCard.Builder stayCard() {
        return new CampingCard.Builder("3054198966", "HR-000123")
                .guest(guest)
                .validFrom(LocalDate.of(2026, 7, 1))
                .validTo(LocalDate.of(2026, 7, 14))
                .billingAccountNo("400012345678");
    }

    @Test
    void cardIsValidDuringTheStay() {
        assertThat(stayCard().build().validate(NOON_JULY_10)).isEqualTo(CardStatus.VALID);
    }

    @Test
    void cardIsValidOnArrivalAndOnDepartureDay() {
        CampingCard card = stayCard().build();

        assertThat(card.validate(LocalDateTime.of(2026, 7, 1, 0, 5))).isEqualTo(CardStatus.VALID);
        assertThat(card.validate(LocalDateTime.of(2026, 7, 14, 23, 55))).isEqualTo(CardStatus.VALID);
    }

    @Test
    void deactivatedCardIsRefusedBeforeAnyOtherCheck() {
        CampingCard card = stayCard().activation(CardActivation.DEACTIVATED).guest(null).build();

        assertThat(card.validate(LocalDateTime.of(2027, 1, 1, 0, 0))).isEqualTo(CardStatus.CARD_DEACTIVATED);
    }

    @Test
    void cardIsNotValidBeforeArrival() {
        assertThat(stayCard().build().validate(LocalDateTime.of(2026, 6, 30, 23, 59)))
                .isEqualTo(CardStatus.NOT_VALID_YET);
    }

    @Test
    void cardIsNotValidAfterDeparture() {
        assertThat(stayCard().build().validate(LocalDateTime.of(2026, 7, 15, 0, 1)))
                .isEqualTo(CardStatus.NOT_VALID_ANYMORE);
    }

    @Test
    void cardWithoutValidityDatesIsValidAnyDay() {
        CampingCard card = stayCard().validFrom(null).validTo(null).build();

        assertThat(card.validate(LocalDateTime.of(2030, 1, 1, 0, 0))).isEqualTo(CardStatus.VALID);
        assertThat(card.validate(null)).isEqualTo(CardStatus.VALID);
    }

    @Test
    void cardWithoutGuestIsRefused() {
        assertThat(stayCard().guest(null).build().validate(NOON_JULY_10)).isEqualTo(CardStatus.NO_GUEST);
    }

    @Test
    void cardOfADeactivatedGuestIsRefused() {
        guest.setStatus(GuestStatus.DEACTIVATED);

        assertThat(stayCard().build().validate(NOON_JULY_10)).isEqualTo(CardStatus.GUEST_DEACTIVATED);
    }

    @Test
    void billingAccountIsCheckedLast() {
        assertThat(stayCard().billingAccountNo(null).build().validate(NOON_JULY_10))
                .isEqualTo(CardStatus.BILLING_ACCOUNT_INVALID);
        assertThat(stayCard().billingAccountNo("ACC-1234").build().validate(NOON_JULY_10))
                .isEqualTo(CardStatus.BILLING_ACCOUNT_INVALID);
    }

    @Test
    void billingAccountNumberHasSixToTwentyDigits() {
        assertThat(CampingCard.isValidBillingAccountNo("123456")).isTrue();
        assertThat(CampingCard.isValidBillingAccountNo("12345678901234567890")).isTrue();
        assertThat(CampingCard.isValidBillingAccountNo("12345")).isFalse();
        assertThat(CampingCard.isValidBillingAccountNo("123456789012345678901")).isFalse();
        assertThat(CampingCard.isValidBillingAccountNo("   ")).isFalse();
        assertThat(CampingCard.isValidBillingAccountNo("12 345 678")).isFalse();
    }

    @Test
    void cardOfAGuestWithBillingAccountsMustBeBilledToOneOfThem() {
        guest.openBillingAccount("400099999999", "Reception bill");

        assertThat(stayCard().build().validate(NOON_JULY_10)).isEqualTo(CardStatus.BILLING_ACCOUNT_INVALID);

        guest.openBillingAccount("400012345678", "Season 2026");
        assertThat(stayCard().build().validate(NOON_JULY_10)).isEqualTo(CardStatus.VALID);
    }

    @Test
    void cardBilledToAClosedAccountIsRefused() {
        guest.openBillingAccount("400012345678", "Season 2025").close(NOON_JULY_10.minusDays(1));

        assertThat(stayCard().build().validate(NOON_JULY_10)).isEqualTo(CardStatus.BILLING_ACCOUNT_INVALID);
    }

    @Test
    void cardBilledToAnAccountAwaitingItsErpNumberIsRefused() {
        guest.openBillingAccount(null, "Season 2026, ordered");

        assertThat(stayCard().billingAccountNo("400012345678").build().validate(NOON_JULY_10))
                .isEqualTo(CardStatus.BILLING_ACCOUNT_INVALID);
    }

    @Test
    void cardOfAnIssuerThatBillsItsMembersNeedsOnlyAWellFormedAccountNumber() {
        assertThat(guest.getBillingAccounts()).isEmpty();

        assertThat(stayCard().build().validate(NOON_JULY_10)).isEqualTo(CardStatus.VALID);
    }
}
