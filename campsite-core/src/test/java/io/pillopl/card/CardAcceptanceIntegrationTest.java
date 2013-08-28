package io.pillopl.card;

import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.guest.Guest;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.test.IntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cards are accepted at the pedestals of the SITE subtrees the guest's issuer (or its nearest ancestor with
 * agreements) has an agreement with.
 */
@Transactional
class CardAcceptanceIntegrationTest extends IntegrationTest {

    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired CardFixture cards;
    @Autowired EntityManager entityManager;

    Group bay, bayBeach, lake, club, clubJunior;
    Pedestal atBayBeach, atLake;

    @BeforeEach
    void setUp() {
        Group hrSites = groupService.fetchSiteGroup("HR SITES");
        bay = groupManager.createChildGroup(hrSites, "BAY CAMPSITE", "Bay");
        bayBeach = groupManager.createChildGroup(bay, "BAY BEACH SECTION", "Bay beach");
        lake = groupManager.createChildGroup(hrSites, "LAKE CAMPSITE", "Lake");
        Group hrIssuers = groupService.fetchIssuerGroup("HR ISSUERS");
        club = groupManager.createChildGroup(hrIssuers, "CARAVAN CLUB", "Caravan club");
        clubJunior = groupManager.createChildGroup(club, "CARAVAN CLUB JUNIOR", "Junior");
        groupManager.addAcceptanceAgreement(club, bay);
        atBayBeach = infrastructure.pedestal("HR-BAY-P001", bayBeach, 2);
        atLake = infrastructure.pedestal("HR-LAKE-P001", lake, 2);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void cardOfAClubMemberIsAcceptedInTheSubtreeOfTheAgreement() {
        CampingCard card = cards.card("2001", cards.guest("Kovac", groupService.fetchByGroupId(clubJunior.getId())));

        assertThat(groupService.isCardAcceptedAt(atBayBeach.getId(), card)).isTrue();
        assertThat(groupService.isCardAcceptedAt("HR-BAY-P001", card)).isTrue();
    }

    @Test
    void cardIsNotAcceptedOutsideTheAgreement() {
        CampingCard card = cards.card("2002", cards.guest("Kovac", groupService.fetchByGroupId(clubJunior.getId())));

        assertThat(groupService.isCardAcceptedAt(atLake.getId(), card)).isFalse();
        assertThat(groupService.isCardAcceptedAt("HR-LAKE-P001", card)).isFalse();
    }

    @Test
    void nearestAgreementListReplacesTheListsAbove() {
        Group junior = groupService.fetchByGroupId(clubJunior.getId());
        groupManager.addAcceptanceAgreement(junior, groupService.fetchByGroupId(lake.getId()));
        entityManager.flush();
        entityManager.clear();
        CampingCard card = cards.card("2003", cards.guest("Kovac", groupService.fetchByGroupId(clubJunior.getId())));

        assertThat(groupService.isCardAcceptedAt("HR-LAKE-P001", card)).isTrue();
        assertThat(groupService.isCardAcceptedAt("HR-BAY-P001", card)).isFalse();
    }

    @Test
    void cardWithoutGuestOrIssuerWithoutAgreementIsNotAccepted() {
        Guest noAgreement = cards.guest("Novak", groupService.fetchIssuerGroup("HR ISSUERS"));

        assertThat(groupService.isCardAcceptedAt(atBayBeach.getId(), cards.card("2004", noAgreement))).isFalse();
        assertThat(groupService.isCardAcceptedAt(atBayBeach.getId(), cards.card("2005", null))).isFalse();
        assertThat(groupService.isCardAcceptedAt(atBayBeach.getId(), null)).isFalse();
    }

    @Test
    void deletedPedestalAcceptsNoCard() {
        CampingCard card = cards.card("2006", cards.guest("Kovac", groupService.fetchByGroupId(clubJunior.getId())));
        Pedestal pedestal = entityManager.find(Pedestal.class, atBayBeach.getId());
        pedestal.setDeleted(true);
        entityManager.flush();

        assertThat(groupService.isCardAcceptedAt(atBayBeach.getId(), card)).isFalse();
        assertThat(groupService.isCardAcceptedAt("HR-BAY-P001", card)).isFalse();
    }
}
