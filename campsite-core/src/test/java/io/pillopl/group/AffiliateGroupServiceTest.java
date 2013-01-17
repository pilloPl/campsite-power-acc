package io.pillopl.group;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AffiliateGroupServiceTest {

    private final InMemoryGroups groups = new InMemoryGroups();

    private final Group sites = groups.root(GroupType.SITE, "HR SITES");
    private final Group networkAccess = groups.child(sites, "NETWORK ACCESS HR");
    private final Group houseCardsOnly = groups.child(sites, "HOUSE CARDS ONLY HR");

    @Test
    void affiliateUnderNetworkAccessParentIsOpenToNetworkCards() {
        Group affiliate = groups.child(networkAccess, "AFFILIATE marko");

        assertThat(AffiliateGroupService.checkAccessMode(affiliate)).isEqualTo(AccessMode.NETWORK_ACCESS);
    }

    @Test
    void affiliateUnderHouseCardsOnlyParentAcceptsOnlyHouseCards() {
        Group affiliate = groups.child(houseCardsOnly, "AFFILIATE marko");

        assertThat(AffiliateGroupService.checkAccessMode(affiliate)).isEqualTo(AccessMode.HOUSE_CARDS_ONLY);
    }

    @Test
    void accessModeLooksOnlyAtTheDirectParent() {
        Group section = groups.child(networkAccess, "NORTH SECTION");
        Group affiliate = groups.child(section, "AFFILIATE marko");

        assertThat(AffiliateGroupService.checkAccessMode(affiliate)).isNull();
    }

    @Test
    void groupThatIsNotAnAffiliateHasNoAccessMode() {
        assertThat(AffiliateGroupService.checkAccessMode(groups.child(networkAccess, "BAY CAMPSITE"))).isNull();
        assertThat(AffiliateGroupService.checkAccessMode(groups.root(GroupType.SITE, "AFFILIATE marko"))).isNull();
        assertThat(AffiliateGroupService.checkAccessMode((Group) null)).isNull();
    }

    @Test
    void affiliateIsASiteGroupWithTheAffiliatePrefixAndAUsername() {
        assertThat(AffiliateGroupService.isAffiliate(groups.child(networkAccess, "AFFILIATE marko"))).isTrue();
        assertThat(AffiliateGroupService.isAffiliate(groups.child(networkAccess, "AFFILIATE "))).isFalse();
        assertThat(AffiliateGroupService.isAffiliate(groups.child(networkAccess, "AFFILIATES"))).isFalse();
        assertThat(AffiliateGroupService.isAffiliate(groups.root(GroupType.STAFF, "AFFILIATE marko"))).isFalse();
    }
}
