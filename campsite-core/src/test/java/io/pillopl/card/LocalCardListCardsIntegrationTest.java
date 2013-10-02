package io.pillopl.card;

import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.guest.Guest;
import io.pillopl.guest.GuestStatus;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.test.IntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The cards SQL of the local card list gives exactly the cards the online validation would accept at the pedestal.
 */
@Transactional
class LocalCardListCardsIntegrationTest extends IntegrationTest {

    static final LocalDate TODAY = LocalDate.of(2026, 7, 10);

    @Autowired CampingCardDao campingCardDao;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired CardFixture cards;
    @Autowired EntityManager entityManager;

    Group bay, bayBeach, club, clubJunior, discount;
    Pedestal atBayBeach;

    @BeforeEach
    void setUp() {
        Group hrSites = groupService.fetchSiteGroup("HR SITES");
        bay = groupManager.createChildGroup(hrSites, "BAY CAMPSITE", "Bay");
        bayBeach = groupManager.createChildGroup(bay, "BAY BEACH SECTION", "Bay beach");
        Group lake = groupManager.createChildGroup(hrSites, "LAKE CAMPSITE", "Lake");
        Group hrIssuers = groupService.fetchIssuerGroup("HR ISSUERS");
        club = groupManager.createChildGroup(hrIssuers, "CARAVAN CLUB", "Caravan club");
        clubJunior = groupManager.createChildGroup(club, "CARAVAN CLUB JUNIOR", "Junior");
        discount = groupManager.createChildGroup(hrIssuers, "DISCOUNT CARDS", "Discount");
        groupManager.addAcceptanceAgreement(club, bay);
        groupManager.addAcceptanceAgreement(discount, lake);
        atBayBeach = infrastructure.pedestal("HR-BAY-P001", bayBeach, 2);
    }

    @Test
    void listTakesTheValidCardsOfTheIssuersAcceptedAtTheCampsite() {
        Guest member = cards.guest("Kovac", clubJunior);
        Guest other = cards.guest("Novak", discount);
        CampingCard staying = cards.card("9001", "N9001", member, TODAY.minusDays(3), TODAY);
        CampingCard season = cards.card("9002", "N9002", member, null, null);
        cards.card("9003", "N9003", member, TODAY.plusDays(1), TODAY.plusDays(4));
        cards.card("9004", "N9004", member, TODAY.minusDays(9), TODAY.minusDays(1));
        cards.card("9005", "N9005", other, TODAY.minusDays(1), TODAY.plusDays(1));
        CampingCard blocked = cards.card("9006", "N9006", member, null, null);
        blocked.setActivation(CardActivation.DEACTIVATED);
        CampingCard noAccount = cards.card("9007", "N9007", member, null, null);
        noAccount.setBillingAccountNo(null);
        entityManager.flush();

        List<CampingCard> list = findForBayBeach();

        assertThat(list).containsExactly(season, staying);
        for (CampingCard card : list) {
            assertThat(card.validate(TODAY.atTime(12, 0))).isEqualTo(CardStatus.VALID);
            assertThat(groupService.isCardAcceptedAt(atBayBeach.getId(), card)).isTrue();
        }
    }

    @Test
    void cardsOfDeactivatedOrDeletedGuestsAreLeftOut() {
        Guest deactivated = cards.guest("Kovac", clubJunior);
        cards.card("9101", deactivated);
        deactivated.setStatus(GuestStatus.DEACTIVATED);
        Guest deleted = cards.guest("Babic", clubJunior);
        cards.card("9102", deleted);
        deleted.setDeleted(true);
        entityManager.flush();

        assertThat(campingCardDao.findValidCardsForLocalCardList(
                groupService.findIssuerGroupIdsAcceptedAt(bayBeach), LocalDate.now())).isEmpty();
    }

    @Test
    void noAcceptedIssuerMeansNoCards() {
        assertThat(campingCardDao.findValidCardsForLocalCardList(List.of(), TODAY)).isEmpty();
        assertThat(campingCardDao.findValidCardsForLocalCardList(null, TODAY)).isEmpty();
    }

    private List<CampingCard> findForBayBeach() {
        return campingCardDao.findValidCardsForLocalCardList(groupService.findIssuerGroupIdsAcceptedAt(bayBeach),
                TODAY);
    }
}
