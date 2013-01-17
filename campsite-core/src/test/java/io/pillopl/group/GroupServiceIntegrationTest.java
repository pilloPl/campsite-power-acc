package io.pillopl.group;

import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class GroupServiceIntegrationTest extends IntegrationTest {

    @Autowired
    GroupService groupService;

    @Autowired
    AffiliateGroupService affiliateGroupService;

    @Autowired
    IssuerCodeService issuerCodeService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void seededTreesAreValid() {
        assertThat(groupService.checkAllGroupsForValidity()).isTrue();
    }

    @Test
    void childOfAnotherTypeThanItsParentMakesTheTreesInvalid() {
        jdbcTemplate.update("INSERT INTO TBL_GROUP (PARENT_ID, NAME, TYPENAME, DISPLAYNAME) VALUES (11, 'X', 'STAFF', 'X')");

        assertThat(groupService.checkAllGroupsForValidity()).isFalse();
    }

    @Test
    void marketOfAGroupWithoutPrefixComesFromItsParent() {
        assertThat(groupService.determineMarketByGroupId(51L)).isEqualTo(Market.HR);
        assertThat(groupService.determineMarketByGroupId(31L)).isEqualTo(Market.HR);
        assertThat(groupService.determineMarketByGroupId(63L)).isEqualTo(Market.FR);
    }

    @Test
    void marketExtensionFindsTheNamedGroupsOfTheMarket() {
        MarketExtension<Market> it = groupService.getExtensionByMarket(Market.IT);

        assertThat(it.findIssuerRoot().getId()).isEqualTo(2L);
        assertThat(it.findSiteRoot().getId()).isEqualTo(12L);
        assertThat(it.findStaffRoot().getId()).isEqualTo(22L);
        assertThat(it.findNetworkAccessSiteGroup().getId()).isEqualTo(52L);
        assertThat(it.findHouseCardsOnlySiteGroup().getId()).isEqualTo(62L);
        assertThat(it.findWelcomeCardPoolIssuerGroup().getId()).isEqualTo(42L);
        assertThat(it.findReceptionStaffGroup().getId()).isEqualTo(72L);
    }

    @Test
    void marketAdminManagesItsCampsitesButNotAnotherMarket() {
        Group hrSites = groupService.fetchByGroupId(11L);
        Group affiliate = affiliateGroupService.createAffiliateSiteGroup("marko", "Marko's campsite", Market.HR,
                AccessMode.NETWORK_ACCESS);

        assertThat(groupService.isManageable(hrSites, affiliate)).isTrue();
        assertThat(groupService.isManageable(groupService.fetchByGroupId(12L), affiliate)).isFalse();
        assertThat(groupService.isManageable(affiliate, hrSites)).isFalse();
    }

    @Test
    void affiliateCampsiteGetsTheAccessModeOfTheParentItWasCreatedUnder() {
        Group open = affiliateGroupService.createAffiliateSiteGroup("marko", "Marko", Market.HR,
                AccessMode.NETWORK_ACCESS);
        Group closed = affiliateGroupService.createAffiliateSiteGroup("ana", "Ana", Market.FR,
                AccessMode.HOUSE_CARDS_ONLY);

        assertThat(open.getName()).isEqualTo("AFFILIATE marko");
        assertThat(affiliateGroupService.checkAccessMode(open.getId())).isEqualTo(AccessMode.NETWORK_ACCESS);
        assertThat(affiliateGroupService.checkAccessMode(closed.getId())).isEqualTo(AccessMode.HOUSE_CARDS_ONLY);
        assertThat(affiliateGroupService.isAffiliate(open.getId())).isTrue();
        assertThat(affiliateGroupService.findAffiliateSiteGroup("ana")).isEqualTo(closed);
        assertThat(groupService.determineMarketByGroupId(closed.getId())).isEqualTo(Market.FR);
    }

    @Test
    void groupThatIsNoAffiliateHasNoAccessMode() {
        assertThat(affiliateGroupService.checkAccessMode(51L)).isNull();
        assertThat(affiliateGroupService.checkAccessMode(999_999L)).isNull();
        assertThat(affiliateGroupService.isAffiliate(51L)).isFalse();
    }

    @Test
    void networkCardIssuerCodeComesFromItsMarket() {
        assertThat(issuerCodeService.defineIssuer(groupService.fetchNetworkCardIssuerGroup())).isEqualTo("CPN-HR");
    }

    @Test
    void siteGroupIdsOfAMarketCanBeFilteredByDisplayName() {
        assertThat(groupService.fetchSiteGroupIds(Market.HR, "network")).containsExactly(51L);
        assertThat(groupService.fetchSiteGroupIds(Market.HR, null)).containsExactly(11L, 51L, 61L);
    }

    @Test
    void manageableIdsOfSeveralGroupsAreMerged() {
        assertThat(groupService.findAllManageableGroupIDsForGroupIDs(new Long[]{51L, 61L, 999_999L}))
                .containsExactly(51L, 61L);
    }
}
