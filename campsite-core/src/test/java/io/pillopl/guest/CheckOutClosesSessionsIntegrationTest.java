package io.pillopl.guest;

import io.pillopl.card.CampingCard;
import io.pillopl.card.CardFixture;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.supply.StopReason;
import io.pillopl.supply.SupplyControlUnitImpl;
import io.pillopl.supply.SupplyRequest;
import io.pillopl.supply.SupplySession;
import io.pillopl.supply.SupplySessionManager;
import io.pillopl.test.IntegrationTest;
import io.pillopl.user.Userinfo;
import io.pillopl.util.DateService;
import io.pillopl.value.SocketLinkId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A supply session started with the card of a stay belongs to the stay; the check-out of the stay closes it once the
 * configured delay (CheckoutSessionCloseDelayInMinutes, 30) has passed.
 */
@Transactional
class CheckOutClosesSessionsIntegrationTest extends IntegrationTest {

    static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

    @Autowired GuestService guestService;
    @Autowired SupplyControlUnitImpl controlUnit;
    @Autowired SupplySessionManager sessionManager;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired UserinfoDao userinfoDao;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired CardFixture cards;
    @Autowired DateService dateService;

    final SocketLinkId socket = SocketLinkId.of("HR-COVE-P001", 1);
    Userinfo reception;
    Guest guest;
    CampingCard card;
    Stay stay;

    @BeforeEach
    void setUp() {
        Group club = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"), "COVE CLUB", "Club");
        Group cove = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "COVE CAMPSITE", "Cove");
        groupManager.addAcceptanceAgreement(club, cove);
        infrastructure.pedestal("HR-COVE-P001", cove, 2).setBillable(true);
        reception = new Userinfo("covereception", "Reception", "reception@example.org");
        reception.setGroup(groupService.fetchStaffGroup("HR RECEPTION"));
        reception.getGroups().add(club);
        reception.getGroups().add(cove);
        userinfoDao.saveAndFlush(reception);
        guest = guestService.createGuest(reception, new Guest.Builder(club, GuestType.TRANSIT, "Kovac")
                .carPlate("ZG 1234 AB"));
        guestService.addUnit(reception, guest.getId(), Unit.UnitType.CARAVAN, "ZG 77 TR", new BigDecimal("6.5"));
        card = cards.card("7201", "HR-7201", guest, null, null);
        clockAt(NOW.minusHours(3));
        stay = guestService.checkIn(reception, guest.getId(), cove.getId(), "C7", NOW.toLocalDate().minusDays(1),
                NOW.toLocalDate().plusDays(3), card.getId(), null, CardFixture.BILLING_ACCOUNT_NO);
    }

    @AfterEach
    void resetClock() {
        dateService.setClock(null);
    }

    @Test
    void sessionOfAStayCardBelongsToTheStayAndCarriesThePlateOfItsUnit() {
        SupplySession session = start(socket, NOW.minusHours(2));

        assertThat(session.getStayId()).isEqualTo(stay.getId());
        assertThat(session.getUnitPlate()).isEqualTo("ZG77TR");
    }

    @Test
    void checkOutClosesTheOpenSessionOfTheStayAfterTheDelayWithReasonCheckout() {
        SupplySession session = start(socket, NOW.minusHours(2));
        clockAt(NOW);
        guestService.checkOut(reception, stay.getId());

        clockAt(NOW.plusMinutes(20));
        assertThat(controlUnit.closeStaleSessions()).isZero();
        assertThat(session.isActive()).isTrue();

        clockAt(NOW.plusMinutes(31));
        assertThat(controlUnit.closeStaleSessions()).isEqualTo(1);
        assertThat(session.isActive()).isFalse();
        assertThat(session.getStopReason()).isEqualTo(StopReason.CHECKOUT);
    }

    @Test
    void sessionOfAStayStillOnTheCampsiteStaysOpen() {
        SupplySession session = start(socket, NOW.minusHours(2));
        clockAt(NOW.plusHours(1));

        assertThat(controlUnit.closeStaleSessions()).isZero();
        assertThat(session.isActive()).isTrue();
    }

    @Test
    void cardWithoutAnOpenStayStartsASessionWithoutStay() {
        clockAt(NOW);
        guestService.checkOut(reception, stay.getId());
        card.setValidTo(null);

        SupplySession session = start(SocketLinkId.of("HR-COVE-P001", 2), NOW.plusMinutes(5));

        assertThat(session.getStayId()).isNull();
        assertThat(session.getUnitPlate()).isNull();
    }

    private SupplySession start(SocketLinkId socketLinkId, LocalDateTime plugIn) {
        Integer transactionId = controlUnit.startSupply(socketLinkId, "7201",
                SupplyRequest.start(new BigDecimal("10.0"), plugIn)).getTransactionId();
        return sessionManager.findSessionFor("HR-COVE-P001", transactionId);
    }

    private void clockAt(LocalDateTime now) {
        dateService.setClock(Clock.fixed(now.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));
    }
}
