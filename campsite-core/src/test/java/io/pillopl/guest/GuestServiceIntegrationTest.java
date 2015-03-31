package io.pillopl.guest;

import io.pillopl.card.CampingCard;
import io.pillopl.card.CardActivation;
import io.pillopl.card.CardFixture;
import io.pillopl.card.CardStatus;
import io.pillopl.group.AccessDeniedException;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.persistence.dao.BillingAccountDao;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.test.IntegrationTest;
import io.pillopl.user.Userinfo;
import io.pillopl.util.DateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Guests, units, billing accounts and stays on the database: the stay drives the card validity, the check-out is what
 * the stale-session job asks for.
 */
@Transactional
class GuestServiceIntegrationTest extends IntegrationTest {

    static final LocalDate ARRIVAL = LocalDate.of(2026, 7, 1);
    static final LocalDate DEPARTURE = LocalDate.of(2026, 7, 8);
    static final String ACCOUNT = "400077770001";

    @Autowired GuestService guestService;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired UserinfoDao userinfoDao;
    @Autowired BillingAccountDao billingAccountDao;
    @Autowired CardFixture cards;
    @Autowired DateService dateService;

    Group club, bay, lake;
    Userinfo reception;
    Guest guest;
    CampingCard card;

    @BeforeEach
    void setUp() {
        club = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"), "STAY CLUB", "Club");
        bay = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "BAY STAYS", "Bay");
        lake = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "LAKE STAYS", "Lake");
        reception = new Userinfo("bayreception", "Reception", "reception@example.org");
        reception.setGroup(groupService.fetchStaffGroup("HR RECEPTION"));
        reception.getGroups().add(club);
        reception.getGroups().add(bay);
        userinfoDao.saveAndFlush(reception);
        guest = guestService.createGuest(reception, new Guest.Builder(club, GuestType.TRANSIT, "Kovac")
                .firstName("Ivan").carPlate("ZG-1234-AB"));
        card = cards.card("7101", "HR-7101", guest, null, null);
        clockAt(LocalDateTime.of(2026, 7, 1, 15, 0));
    }

    @AfterEach
    void resetClock() {
        dateService.setClock(null);
    }

    @Test
    void checkInHandsOutTheCardValidForTheStayAndBilledToTheGuestsAccount() {
        Stay stay = guestService.checkIn(reception, guest.getId(), bay.getId(), "a12", ARRIVAL, DEPARTURE,
                card.getId(), null, ACCOUNT);

        assertThat(stay.getId()).isNotNull();
        assertThat(stay.getPitchNo()).isEqualTo("A12");
        assertThat(stay.getCheckedInBy()).isEqualTo("bayreception");
        assertThat(card.getValidFrom()).isEqualTo(ARRIVAL);
        assertThat(card.getValidTo()).isEqualTo(DEPARTURE);
        assertThat(card.getBillingAccountNo()).isEqualTo(ACCOUNT);
        assertThat(billingAccountDao.findByErpAccountNo(ACCOUNT).getGuest()).isEqualTo(guest);
        assertThat(card.validate(LocalDateTime.of(2026, 7, 4, 20, 0))).isEqualTo(CardStatus.VALID);
        assertThat(card.validate(LocalDateTime.of(2026, 7, 9, 8, 0))).isEqualTo(CardStatus.NOT_VALID_ANYMORE);
    }

    @Test
    void checkInActivatesABlockedCardAndTakesTheGuestsOnlyUnit() {
        card.setActivation(CardActivation.DEACTIVATED);
        Unit camper = guestService.addUnit(reception, guest.getId(), Unit.UnitType.CAMPER, "ri 555 cc",
                new BigDecimal("7.4"));

        Stay stay = guestService.checkIn(reception, guest.getId(), bay.getId(), "A12", ARRIVAL, DEPARTURE,
                card.getId(), null, null);

        assertThat(card.getActivation()).isEqualTo(CardActivation.ACTIVATED);
        assertThat(stay.getUnit()).isEqualTo(camper);
        assertThat(stay.getUnitPlate()).isEqualTo("RI555CC");
    }

    @Test
    void pitchIsLetToOneStayAtATimeButTheDepartureDayIsTheNextArrivalDay() {
        guestService.checkIn(reception, guest.getId(), bay.getId(), "A12", ARRIVAL, DEPARTURE, null, null, null);
        Guest other = guestService.createGuest(reception, new Guest.Builder(club, GuestType.TRANSIT, "Novak"));

        assertThatThrownBy(() -> guestService.checkIn(reception, other.getId(), bay.getId(), "A12",
                DEPARTURE.minusDays(1), DEPARTURE.plusDays(2), null, null, null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Pitch A12");
        assertThat(guestService.checkIn(reception, other.getId(), bay.getId(), "A12", DEPARTURE,
                DEPARTURE.plusDays(2), null, null, null).getId()).isNotNull();
        assertThat(guestService.checkIn(reception, other.getId(), bay.getId(), "A13", ARRIVAL, DEPARTURE, null,
                null, null).getId()).isNotNull();
    }

    @Test
    void cardIsHandedOutForOneStayAtATime() {
        guestService.checkIn(reception, guest.getId(), bay.getId(), "A12", ARRIVAL, DEPARTURE, card.getId(), null,
                null);

        assertThatThrownBy(() -> guestService.checkIn(reception, guest.getId(), bay.getId(), "B1", ARRIVAL.plusDays(1),
                DEPARTURE, card.getId(), null, null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void checkInRefusesCardsOfOtherGuestsUnmanageableCampsitesAndClosedAccounts() {
        Guest other = guestService.createGuest(reception, new Guest.Builder(club, GuestType.TRANSIT, "Novak"));
        CampingCard otherCard = cards.card("7102", "HR-7102", other, null, null);
        guestService.openBillingAccount(reception, guest.getId(), "400077770009", "2025").close(dateService.getNow());

        assertThatThrownBy(() -> guestService.checkIn(reception, guest.getId(), bay.getId(), "A12", ARRIVAL,
                DEPARTURE, otherCard.getId(), null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> guestService.checkIn(reception, guest.getId(), lake.getId(), "A12", ARRIVAL,
                DEPARTURE, card.getId(), null, null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> guestService.checkIn(reception, guest.getId(), club.getId(), "A12", ARRIVAL,
                DEPARTURE, card.getId(), null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> guestService.checkIn(reception, guest.getId(), bay.getId(), "A12", ARRIVAL,
                DEPARTURE, card.getId(), null, "400077770009")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> guestService.checkIn(reception, 999_999L, bay.getId(), "A12", ARRIVAL,
                DEPARTURE, null, null, null)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deactivatedGuestCannotCheckIn() {
        guestService.changeGuestStatus(reception, guest.getId(), GuestStatus.DEACTIVATED);

        assertThatThrownBy(() -> guestService.checkIn(reception, guest.getId(), bay.getId(), "A12", ARRIVAL,
                DEPARTURE, null, null, null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void guestsOfAnotherIssuerAreNotManageable() {
        Group otherClub = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"), "OTHER STAY CLUB",
                "Other");
        Userinfo admin = new Userinfo("stayadmin", "Admin", "admin@example.org");
        admin.setGroup(groupService.fetchStaffGroup("HR STAFF"));
        admin.getGroups().add(groupService.fetchIssuerGroup("HR ISSUERS"));
        userinfoDao.saveAndFlush(admin);
        Guest outsider = guestService.createGuest(admin, new Guest.Builder(otherClub, GuestType.TRANSIT, "Rossi"));

        assertThatThrownBy(() -> guestService.checkIn(reception, outsider.getId(), bay.getId(), "A12", ARRIVAL,
                DEPARTURE, null, null, null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> guestService.createGuest(reception, new Guest.Builder(otherClub, GuestType.TRANSIT,
                "Bianchi"))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> guestService.createGuest(reception, new Guest.Builder(bay, GuestType.TRANSIT,
                "Bianchi"))).isInstanceOf(IllegalArgumentException.class);
        assertThat(guestService.findGuests(reception, "zg1")).isEmpty();
        assertThat(guestService.findGuests(reception, "ZG-12")).containsExactly(guest);
        assertThat(guestService.findGuests(admin, "ross")).containsExactly(outsider);
    }

    @Test
    void extendedStayKeepsTheCardValidAndMayNotRunIntoTheNextGuest() {
        Stay stay = guestService.checkIn(reception, guest.getId(), bay.getId(), "A12", ARRIVAL, DEPARTURE,
                card.getId(), null, null);
        Guest next = guestService.createGuest(reception, new Guest.Builder(club, GuestType.TRANSIT, "Novak"));
        guestService.checkIn(reception, next.getId(), bay.getId(), "A12", DEPARTURE.plusDays(2),
                DEPARTURE.plusDays(5), null, null, null);

        guestService.changeDeparture(reception, stay.getId(), DEPARTURE.plusDays(2));

        assertThat(card.getValidTo()).isEqualTo(DEPARTURE.plusDays(2));
        assertThatThrownBy(() -> guestService.changeDeparture(reception, stay.getId(), DEPARTURE.plusDays(3)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void checkOutEndsTheCardAndIsReportedToTheStaleSessionJobAfterTheDelay() {
        Stay stay = guestService.checkIn(reception, guest.getId(), bay.getId(), "A12", ARRIVAL, DEPARTURE,
                card.getId(), null, null);
        Stay staying = guestService.checkIn(reception, guest.getId(), bay.getId(), "A13", ARRIVAL, DEPARTURE, null,
                null, null);
        clockAt(LocalDateTime.of(2026, 7, 5, 10, 0));

        guestService.checkOut(reception, stay.getId());

        assertThat(card.getValidTo()).isEqualTo(LocalDate.of(2026, 7, 5));
        assertThat(guestService.findCheckedOutStayIds(List.of(stay.getId(), staying.getId()),
                LocalDateTime.of(2026, 7, 5, 10, 30))).containsExactly(stay.getId());
        assertThat(guestService.findCheckedOutStayIds(List.of(stay.getId()), LocalDateTime.of(2026, 7, 5, 9, 30)))
                .isEmpty();
        assertThat(guestService.findCheckedOutStayIds(List.of(), LocalDateTime.of(2026, 7, 6, 0, 0))).isEmpty();
        assertThatThrownBy(() -> guestService.checkOut(reception, stay.getId()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(guestService.findStaysOfGuest(reception, guest.getId())).hasSize(2);
    }

    @Test
    void erpAccountNumberBelongsToOneGuest() {
        guestService.openBillingAccount(reception, guest.getId(), ACCOUNT, "Season");
        Guest other = guestService.createGuest(reception, new Guest.Builder(club, GuestType.TRANSIT, "Novak"));

        assertThatThrownBy(() -> guestService.openBillingAccount(reception, other.getId(), ACCOUNT, "Season"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> guestService.openBillingAccount(reception, other.getId(), "ACC-1", "Season"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(guestService.openBillingAccount(reception, other.getId(), null, "Ordered").isAwaitingErpNumber())
                .isTrue();
    }

    @Test
    void updateKeepsTheDetailsNotGiven() {
        Guest details = new Guest();
        details.setCity("Zagreb");
        details.setIban("HR1210010051863000160");

        Guest updated = guestService.updateGuest(reception, guest.getId(), details);

        assertThat(updated.getCity()).isEqualTo("Zagreb");
        assertThat(updated.getIban()).isEqualTo("HR1210010051863000160");
        assertThat(updated.getName()).isEqualTo("Kovac");
        assertThat(updated.getCarPlate()).isEqualTo("ZG-1234-AB");
        assertThat(updated.getModDate()).isEqualTo(LocalDateTime.of(2026, 7, 1, 15, 0));
    }

    private void clockAt(LocalDateTime now) {
        dateService.setClock(Clock.fixed(now.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));
    }
}
