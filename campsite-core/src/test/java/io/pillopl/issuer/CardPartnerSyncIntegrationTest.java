package io.pillopl.issuer;

import io.pillopl.card.CampingCard;
import io.pillopl.card.CardActivation;
import io.pillopl.card.CardFixture;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.guest.Guest;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The nightly date conditions of the card list exchange (hand-written SQL) and the export selection.
 */
@Transactional
class CardPartnerSyncIntegrationTest extends IntegrationTest {

    static final LocalDate TODAY = LocalDate.of(2026, 7, 10);

    @Autowired CampingCardDao campingCardDao;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired CardFixture cards;

    Guest networkGuest;

    @BeforeEach
    void setUp() {
        networkGuest = cards.guest("Horvat", groupService.fetchNetworkCardIssuerGroup());
    }

    @Test
    void cardOfARunningStayIsAddedForThePartners() {
        CampingCard card = exported(cards.card("7001", "N7001", networkGuest, TODAY.minusDays(3), TODAY), null);

        campingCardDao.updateExpiredCardsPartnerSyncState(TODAY);

        assertThat(reload(card).getPartnerSyncState()).isEqualTo(PartnerSyncState.ADD);
    }

    @Test
    void seasonCardWithoutEndIsAddedOnceItHasBegun() {
        CampingCard begun = exported(cards.card("7002", "N7002", networkGuest, TODAY, null), null);
        CampingCard notYet = exported(cards.card("7003", "N7003", networkGuest, TODAY.plusDays(1), null), null);

        campingCardDao.updateExpiredCardsPartnerSyncState(TODAY);

        assertThat(reload(begun).getPartnerSyncState()).isEqualTo(PartnerSyncState.ADD);
        assertThat(reload(notYet).getPartnerSyncState()).isEqualTo(PartnerSyncState.NOT);
    }

    @Test
    void cardThePartnersHaveIsDeletedTheDayAfterDeparture() {
        CampingCard departureToday = exported(cards.card("7004", "N7004", networkGuest, TODAY.minusDays(5), TODAY),
                PartnerSyncState.ADD);
        CampingCard departedYesterday = exported(cards.card("7005", "N7005", networkGuest, TODAY.minusDays(5),
                TODAY.minusDays(1)), PartnerSyncState.ADD);

        campingCardDao.updateExpiredCardsPartnerSyncState(TODAY);

        assertThat(reload(departureToday).getPartnerSyncState()).isEqualTo(PartnerSyncState.NOT);
        assertThat(reload(departedYesterday).getPartnerSyncState()).isEqualTo(PartnerSyncState.DELETE);
    }

    @Test
    void cardWithOnlyAnEndIsDeletedWhenTheEndHasPassed() {
        CampingCard card = exported(cards.card("7006", "N7006", networkGuest, null, TODAY.minusDays(1)),
                PartnerSyncState.ADD);

        campingCardDao.updateExpiredCardsPartnerSyncState(TODAY);

        assertThat(reload(card).getPartnerSyncState()).isEqualTo(PartnerSyncState.DELETE);
    }

    @Test
    void expiredCardThePartnersNeverGotIsNothingToDo() {
        CampingCard card = cards.card("7007", "N7007", networkGuest, TODAY.minusDays(9), TODAY.minusDays(2));

        campingCardDao.updateExpiredCardsPartnerSyncState(TODAY);

        assertThat(reload(card).getPartnerSyncState()).isEqualTo(PartnerSyncState.NOT);
    }

    @Test
    void pendingModificationOfARunningStayIsKept() {
        CampingCard card = exported(cards.card("7008", "N7008", networkGuest, TODAY.minusDays(1), TODAY.plusDays(1)),
                PartnerSyncState.ADD);
        card.calculatePartnerSyncState(PartnerSyncState.MODIFY);
        campingCardDao.saveAndFlush(card);

        campingCardDao.updateExpiredCardsPartnerSyncState(TODAY);

        assertThat(reload(card).getPartnerSyncState()).isEqualTo(PartnerSyncState.MODIFY);
    }

    @Test
    void deactivatedDeletedAndNeverExchangedCardsAreLeftAlone() {
        CampingCard deactivated = cards.card("7009", "N7009", networkGuest, TODAY.minusDays(9), TODAY.minusDays(2));
        deactivated.setActivation(CardActivation.DEACTIVATED);
        deactivated.setTargetPartnerSyncState(PartnerSyncState.ADD);
        deactivated.setPartnerSyncState(PartnerSyncState.NOT);
        CampingCard deleted = exported(cards.card("7010", "N7010", networkGuest, TODAY.minusDays(9),
                TODAY.minusDays(2)), PartnerSyncState.ADD);
        deleted.setDeleted(true);
        CampingCard never = cards.card("7011", "N7011", networkGuest, TODAY.minusDays(1), TODAY.plusDays(1));
        never.setPartnerSyncState(PartnerSyncState.NEVER);
        never.setTargetPartnerSyncState(PartnerSyncState.NEVER);
        campingCardDao.saveAllAndFlush(java.util.List.of(deactivated, deleted, never));

        campingCardDao.updateExpiredCardsPartnerSyncState(TODAY);

        assertThat(reload(deactivated).getPartnerSyncState()).isEqualTo(PartnerSyncState.NOT);
        assertThat(reload(deleted).getPartnerSyncState()).isEqualTo(PartnerSyncState.NOT);
        assertThat(reload(never).getPartnerSyncState()).isEqualTo(PartnerSyncState.NEVER);
    }

    @Test
    void exportTakesTheWaitingCardsOfTheIssuerSubtreeOnly() {
        Group club = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"), "SYNC CLUB", "Club");
        Group networkChild = groupManager.createChildGroup(groupService.fetchNetworkCardIssuerGroup(),
                "NETWORK CARD SEASON", "Season");
        CampingCard waiting = cards.card("7012", "N7012", networkGuest, TODAY, TODAY.plusDays(3));
        CampingCard ofChildGroup = cards.card("7013", "N7013", cards.guest("Babic", networkChild), TODAY,
                TODAY.plusDays(3));
        CampingCard done = exported(cards.card("7014", "N7014", networkGuest, TODAY, TODAY.plusDays(3)),
                PartnerSyncState.ADD);
        CampingCard ofClub = cards.card("7015", "N7015", cards.guest("Kovac", club), TODAY, TODAY.plusDays(3));

        assertThat(campingCardDao.findCardsForPartnerExport(groupService.fetchNetworkCardIssuerGroup().getId()))
                .extracting(CampingCard::getId)
                .contains(waiting.getId(), ofChildGroup.getId())
                .doesNotContain(done.getId(), ofClub.getId());
    }

    private CampingCard exported(CampingCard card, PartnerSyncState target) {
        card.setTargetPartnerSyncState(target);
        card.setPartnerSyncState(PartnerSyncState.NOT);
        card.setUploadDate(target != null ? LocalDateTime.of(2026, 7, 1, 2, 0) : null);
        return campingCardDao.saveAndFlush(card);
    }

    private CampingCard reload(CampingCard card) {
        return campingCardDao.findById(card.getId()).orElseThrow();
    }
}
