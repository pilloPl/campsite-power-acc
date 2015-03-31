package io.pillopl.guest;

import io.pillopl.card.CampingCard;
import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StayTest {

    static final LocalDate ARRIVAL = LocalDate.of(2026, 7, 1);
    static final LocalDate DEPARTURE = LocalDate.of(2026, 7, 8);

    final Guest guest = new Guest(new Group(GroupType.ISSUER, "NETWORK CARD"), GuestType.TRANSIT, "Horvat");
    final Group bay = new Group(GroupType.SITE, "BAY");

    Stay stayWithCard() {
        Stay stay = new Stay(guest, bay, "A12", ARRIVAL, DEPARTURE);
        stay.setCard(new CampingCard.Builder("3054198966", "HR-1").guest(guest).validFrom(ARRIVAL)
                .validTo(DEPARTURE).build());
        return stay;
    }

    @Test
    void stayCoversTheDaysFromArrivalToDepartureInclusive() {
        Stay stay = stayWithCard();

        assertThat(stay.covers(ARRIVAL)).isTrue();
        assertThat(stay.covers(DEPARTURE)).isTrue();
        assertThat(stay.covers(ARRIVAL.minusDays(1))).isFalse();
        assertThat(stay.covers(DEPARTURE.plusDays(1))).isFalse();
        assertThat(stay.getNights()).isEqualTo(7);
    }

    @Test
    void stayLeavingOnItsArrivalDayCountsOneNight() {
        assertThat(new Stay(guest, bay, null, ARRIVAL, ARRIVAL).getNights()).isEqualTo(1);
    }

    @Test
    void departureBeforeArrivalIsNoStay() {
        assertThatThrownBy(() -> new Stay(guest, bay, "A12", DEPARTURE, ARRIVAL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void extendedStayExtendsTheCard() {
        Stay stay = stayWithCard();

        stay.changeDeparture(DEPARTURE.plusDays(3));

        assertThat(stay.getCard().getValidTo()).isEqualTo(DEPARTURE.plusDays(3));
        assertThatThrownBy(() -> stay.changeDeparture(ARRIVAL.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void earlyCheckOutEndsTheCardOnTheCheckOutDay() {
        Stay stay = stayWithCard();

        stay.checkOut(LocalDateTime.of(2026, 7, 5, 10, 30), "reception");

        assertThat(stay.isCheckedOut()).isTrue();
        assertThat(stay.covers(LocalDate.of(2026, 7, 5))).isFalse();
        assertThat(stay.getCard().getValidTo()).isEqualTo(LocalDate.of(2026, 7, 5));
        assertThat(stay.getCheckedOutBy()).isEqualTo("reception");
    }

    @Test
    void lateCheckOutKeepsTheCardValidityOfTheStay() {
        Stay stay = stayWithCard();

        stay.checkOut(LocalDateTime.of(2026, 7, 9, 9, 0), "reception");

        assertThat(stay.getCard().getValidTo()).isEqualTo(DEPARTURE);
    }

    @Test
    void stayIsCheckedOutOnlyOnceAndThenNotChangedAnyMore() {
        Stay stay = stayWithCard();
        stay.checkOut(LocalDateTime.of(2026, 7, 8, 9, 0), "reception");

        assertThatThrownBy(() -> stay.checkOut(LocalDateTime.of(2026, 7, 8, 10, 0), "reception"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> stay.changeDeparture(DEPARTURE.plusDays(1)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void plateOfTheStayIsTheUnitsOtherwiseTheGuestsTrailerOrCar() {
        Stay stay = stayWithCard();
        guest.setCarPlate("zg 1234-ab");
        assertThat(stay.getUnitPlate()).isEqualTo("ZG1234AB");

        guest.setTrailerPlate("ZG 77-TR");
        assertThat(stay.getUnitPlate()).isEqualTo("ZG77TR");

        stay.setUnit(guest.addUnit(Unit.UnitType.CAMPER, "ri 555 cc", new BigDecimal("7.4")));
        assertThat(stay.getUnitPlate()).isEqualTo("RI555CC");
    }

    @Test
    void tentHasNoPlate() {
        assertThat(guest.addUnit(Unit.UnitType.TENT, "X 1", null).getPlate()).isNull();
    }
}
