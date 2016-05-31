package io.pillopl.group;

import io.pillopl.persistence.dao.GroupDao;
import io.pillopl.test.IntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The one agreement semantics, computed by the in-memory walk and by the recursive SQL, on the same tree.
 */
@Transactional
class AcceptanceAgreementIntegrationTest extends IntegrationTest {

    @Autowired
    GroupService groupService;

    @Autowired
    GroupManager groupManager;

    @Autowired
    GroupDao groupDao;

    @Autowired
    EntityManager entityManager;

    // SITE tree under HR SITES
    Group adriatic, bay, bayBeach, pine, lake, affiliate;

    // ISSUER tree under HR ISSUERS
    Group club, clubJunior, clubJuniorSummer, discountCards, discountGold, networkCard;

    @BeforeEach
    void tree() {
        Group hrSites = groupService.fetchSiteGroup("HR SITES");
        adriatic = groupManager.createChildGroup(hrSites, "ADRIATIC CAMPSITES", "Adriatic");
        bay = groupManager.createChildGroup(adriatic, "BAY CAMPSITE", "Bay");
        bayBeach = groupManager.createChildGroup(bay, "BAY BEACH SECTION", "Bay beach");
        pine = groupManager.createChildGroup(adriatic, "PINE CAMPSITE", "Pine");
        lake = groupManager.createChildGroup(hrSites, "LAKE CAMPSITE", "Lake");
        affiliate = groupManager.createChildGroup(groupService.fetchSiteGroup("NETWORK ACCESS HR"),
                "AFFILIATE marko", "Marko's campsite");

        Group hrIssuers = groupService.fetchIssuerGroup("HR ISSUERS");
        networkCard = groupService.fetchNetworkCardIssuerGroup();
        club = groupManager.createChildGroup(hrIssuers, "CARAVAN CLUB", "Caravan club");
        clubJunior = groupManager.createChildGroup(club, "CARAVAN CLUB JUNIOR", "Junior");
        clubJuniorSummer = groupManager.createChildGroup(clubJunior, "CARAVAN CLUB JUNIOR SUMMER", "Summer");
        discountCards = groupManager.createChildGroup(hrIssuers, "DISCOUNT CARDS", "Discount cards");
        discountGold = groupManager.createChildGroup(discountCards, "DISCOUNT GOLD", "Gold");

        groupManager.addAcceptanceAgreement(networkCard, groupService.fetchSiteGroup("NETWORK ACCESS HR"));
        groupManager.addAcceptanceAgreement(networkCard, adriatic);
        groupManager.addAcceptanceAgreement(networkCard, lake);
        groupManager.addAcceptanceAgreement(club, adriatic);
        groupManager.addAcceptanceAgreement(clubJunior, lake);
        groupManager.addAcceptanceAgreement(clubJunior, bayBeach);
        groupManager.addAcceptanceAgreement(discountCards, pine);

        entityManager.flush();
        entityManager.clear();
    }

    private Group reload(Group group) {
        return groupDao.findById(group.getId()).orElseThrow();
    }

    @Test
    void javaAndSqlGiveTheSameAcceptingSitesForEveryIssuerGroup() {
        for (Group issuerGroup : groupService.loadGroupsOfType(GroupType.ISSUER)) {
            assertThat(groupService.findAcceptingSiteGroupIds(issuerGroup))
                    .as("accepting sites of %s", issuerGroup.getName())
                    .containsExactlyInAnyOrderElementsOf(groupService.extractAllAcceptingSiteGroupIds(issuerGroup));
        }
    }

    @Test
    void reverseSqlListsExactlyTheIssuersWhoseJavaAnswerContainsTheSite() {
        List<Group> issuerGroups = groupService.loadGroupsOfType(GroupType.ISSUER);
        for (Group siteGroup : groupService.loadGroupsOfType(GroupType.SITE)) {
            List<Long> expected = new ArrayList<>();
            for (Group issuerGroup : issuerGroups) {
                if (groupService.extractAllAcceptingSiteGroupIds(issuerGroup).contains(siteGroup.getId())) {
                    expected.add(issuerGroup.getId());
                }
            }
            assertThat(groupService.findIssuerGroupIdsAcceptedAt(siteGroup))
                    .as("issuers accepted at %s", siteGroup.getName())
                    .containsExactlyInAnyOrderElementsOf(expected);
        }
    }

    @Test
    void nearestAgreementListReplacesTheListOfTheParent() {
        assertThat(groupService.findAcceptingSiteGroupIds(reload(clubJuniorSummer)))
                .containsExactlyInAnyOrder(lake.getId(), bayBeach.getId());
        assertThat(groupService.findAcceptingSiteGroupIds(reload(club)))
                .containsExactlyInAnyOrder(adriatic.getId(), bay.getId(), bayBeach.getId(), pine.getId());
    }

    @Test
    void issuerWithoutAgreementsOnItsLineIsAcceptedNowhere() {
        Group hrIssuers = groupService.fetchIssuerGroup("HR ISSUERS");

        assertThat(groupService.findAcceptingSiteGroupIds(hrIssuers)).isEmpty();
        assertThat(groupService.extractAllAcceptingSiteGroupIds(hrIssuers)).isEmpty();
    }

    @Test
    void cardsOfAnIssuerAreAcceptedWhereverItsEffectiveListReaches() {
        assertThat(groupService.findIssuerGroupIdsAcceptedAt(reload(bayBeach)))
                .containsExactlyInAnyOrder(networkCard.getId(), club.getId(), clubJunior.getId(),
                        clubJuniorSummer.getId());
        assertThat(groupService.findIssuerGroupIdsAcceptedAt(reload(pine)))
                .containsExactlyInAnyOrder(networkCard.getId(), club.getId(), discountCards.getId(),
                        discountGold.getId());
        assertThat(groupService.findIssuerGroupIdsAcceptedAt(reload(affiliate)))
                .containsExactly(networkCard.getId());
    }

    @Test
    void cardAcceptanceAtASiteGroupFollowsTheInMemoryWalk() {
        assertThat(groupService.isAcceptedAt(reload(clubJuniorSummer), bayBeach.getId())).isTrue();
        assertThat(groupService.isAcceptedAt(reload(clubJuniorSummer), pine.getId())).isFalse();
    }

    @Test
    void agreementsOfTheSeedAndOfTheTreeAreValid() {
        assertThat(groupService.validateGroupTree()).isTrue();
    }
}
