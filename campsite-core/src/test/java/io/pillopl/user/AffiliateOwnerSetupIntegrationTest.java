package io.pillopl.user;

import io.pillopl.group.AccessMode;
import io.pillopl.group.AffiliateGroupService;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.group.Market;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class AffiliateOwnerSetupIntegrationTest extends IntegrationTest {

    @Autowired UserinfoService userinfoService;
    @Autowired UserinfoDao userinfoDao;
    @Autowired GroupService groupService;
    @Autowired AffiliateGroupService affiliateGroupService;

    @Test
    void newAffiliateOwnerGetsAHouseCardsOnlyCampsiteGroupAndTheOwnerRole() {
        Userinfo marko = selfServiceUser("marko", "HR SELF SERVICE");

        userinfoService.setupAffiliateOwner(marko);

        Group affiliate = marko.getGroup(GroupType.SITE);
        assertThat(affiliate.getName()).isEqualTo("AFFILIATE marko");
        assertThat(affiliateGroupService.checkAccessMode(affiliate.getId())).isEqualTo(AccessMode.HOUSE_CARDS_ONLY);
        assertThat(marko.getGroups()).containsExactly(affiliate);
        assertThat(marko.containsRole(UserroleName.AFFILIATE_OWNER)).isTrue();
        assertThat(marko.containsRole(UserroleName.AFFILIATE_PRICE_REPORTING)).isFalse();
        assertThat(userinfoService.checkAffiliateOwnerSetup("marko")).isEqualTo(marko);
    }

    @Test
    void affiliateOwnerOutsideTheHomeMarketAlsoGetsPriceReports() {
        Userinfo luca = selfServiceUser("luca", "IT SELF SERVICE");

        userinfoService.setupAffiliateOwner(luca);

        assertThat(luca.containsRole(UserroleName.AFFILIATE_PRICE_REPORTING)).isTrue();
    }

    @Test
    void repeatedSetupKeepsTheAccessModeOfTheExistingGroup() {
        Userinfo marko = selfServiceUser("marko", "HR SELF SERVICE");
        affiliateGroupService.createAffiliateSiteGroup("marko", "Marko", Market.HR, AccessMode.NETWORK_ACCESS);

        userinfoService.setupAffiliateOwner(marko);
        userinfoService.setupAffiliateOwner(marko);

        assertThat(affiliateGroupService.checkAccessMode(marko.getGroup(GroupType.SITE).getId()))
                .isEqualTo(AccessMode.NETWORK_ACCESS);
        assertThat(marko.getRoles()).filteredOn(r -> r.is(UserroleName.AFFILIATE_OWNER)).hasSize(1);
    }

    @Test
    void ownerWithoutAnyAffiliateGroupIsNotSetUpYet() {
        selfServiceUser("marko", "HR SELF SERVICE");

        assertThat(userinfoService.checkAffiliateOwnerSetup("marko")).isNull();
    }

    @Test
    void unknownOwnerIsABrokenSetup() {
        assertThatThrownBy(() -> userinfoService.checkAffiliateOwnerSetup("ghost"))
                .isInstanceOf(AffiliateSetupException.class).hasMessageContaining("not found");
    }

    @Test
    void ownerWithAWronglyNamedSiteGroupIsABrokenSetup() {
        Userinfo marko = selfServiceUser("marko", "HR SELF SERVICE");
        marko.getGroups().add(groupService.fetchSiteGroup("NETWORK ACCESS HR"));

        assertThatThrownBy(() -> userinfoService.checkAffiliateOwnerSetup("marko"))
                .isInstanceOf(AffiliateSetupException.class).hasMessageContaining("naming is wrong");
    }

    @Test
    void affiliateGroupNotAssociatedToTheOwnerIsABrokenSetup() {
        selfServiceUser("marko", "HR SELF SERVICE");
        affiliateGroupService.createAffiliateSiteGroup("marko", "Marko", Market.HR, AccessMode.NETWORK_ACCESS);

        assertThatThrownBy(() -> userinfoService.checkAffiliateOwnerSetup("marko"))
                .isInstanceOf(AffiliateSetupException.class).hasMessageContaining("not associated");
    }

    @Test
    void ownerAssociatedToAnotherAffiliateGroupIsABrokenSetup() {
        Userinfo marko = selfServiceUser("marko", "HR SELF SERVICE");
        affiliateGroupService.createAffiliateSiteGroup("marko", "Marko", Market.HR, AccessMode.NETWORK_ACCESS);
        marko.getGroups().add(affiliateGroupService.createAffiliateSiteGroup("ana", "Ana", Market.HR,
                AccessMode.NETWORK_ACCESS));

        assertThatThrownBy(() -> userinfoService.checkAffiliateOwnerSetup("marko"))
                .isInstanceOf(AffiliateSetupException.class).hasMessageContaining("not the one");
    }

    private Userinfo selfServiceUser(String username, String staffGroup) {
        Userinfo user = new Userinfo(username, username, username + "@example.org");
        user.setGroup(groupService.fetchStaffGroup(staffGroup));
        return userinfoDao.saveAndFlush(user);
    }
}
