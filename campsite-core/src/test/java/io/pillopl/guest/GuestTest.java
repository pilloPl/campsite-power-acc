package io.pillopl.guest;

import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class GuestTest {

    final Group club = new Group(GroupType.ISSUER, "CLUB");

    @Test
    void streetLineJoinsStreetHouseNumberAndAddition() {
        Guest guest = new Guest.Builder(club, GuestType.TRANSIT, "Horvat").street(" Obala ").houseNo("12")
                .houseNoAdd("a").build();

        assertThat(guest.getStreetLine()).isEqualTo("Obala 12a");
        assertThat(new Guest(club, GuestType.TRANSIT, "Horvat").getStreetLine()).isNull();
    }

    @Test
    void directDebitNeedsTheMethodTheHolderAndTheIban() {
        Guest.Builder builder = new Guest.Builder(club, GuestType.SEASONAL, "Horvat")
                .paymentMethod(Guest.PaymentMethod.DIRECT_DEBIT).bankAccountName("A. Horvat");

        assertThat(builder.build().isDirectDebitPossible()).isFalse();
        assertThat(builder.iban("HR1210010051863000160").build().isDirectDebitPossible()).isTrue();
        assertThat(builder.paymentMethod(Guest.PaymentMethod.INVOICE).build().isDirectDebitPossible()).isFalse();
    }

    @Test
    void updateKeepsWhatIsNotGivenAndTrimsWhatIs() {
        Guest guest = new Guest.Builder(club, GuestType.TRANSIT, "Horvat").city("Split").build();
        Guest details = new Guest();
        details.setEmail(" ana@example.org ");
        details.setCity(" ");

        guest.updateDetails(details, LocalDateTime.of(2026, 7, 1, 9, 0));

        assertThat(guest.getEmail()).isEqualTo("ana@example.org");
        assertThat(guest.getCity()).isEqualTo("Split");
        assertThat(guest.getName()).isEqualTo("Horvat");
    }

    @Test
    void onlyUnitWithPlateIsTakenWhenThereIsExactlyOne() {
        Guest guest = new Guest(club, GuestType.TRANSIT, "Horvat");
        guest.addUnit(Unit.UnitType.TENT, null, null);
        assertThat(guest.getOnlyUnitWithPlate()).isNull();

        Unit camper = guest.addUnit(Unit.UnitType.CAMPER, "ZG 1 AB", new BigDecimal("7"));
        assertThat(guest.getOnlyUnitWithPlate()).isSameAs(camper);

        guest.addUnit(Unit.UnitType.CARAVAN, "ZG 2 AB", new BigDecimal("6"));
        assertThat(guest.getOnlyUnitWithPlate()).isNull();
    }

    @Test
    void billingAccountIsFoundByItsErpNumber() {
        Guest guest = new Guest(club, GuestType.TRANSIT, "Horvat");
        BillingAccount account = guest.openBillingAccount("400012345678", "Stay");
        guest.openBillingAccount(null, "Ordered");

        assertThat(guest.findBillingAccount("400012345678")).isSameAs(account);
        assertThat(guest.findBillingAccount("400099999999")).isNull();
        assertThat(guest.findBillingAccount(null)).isNull();
    }
}
