package io.pillopl.user;

import io.pillopl.group.AccessMode;
import io.pillopl.group.AffiliateGroupService;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupType;
import io.pillopl.group.Market;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class SettlementPartnerIntegrationTest extends IntegrationTest {

    @Autowired UserinfoService userinfoService;
    @Autowired UserinfoDao userinfoDao;
    @Autowired AffiliateGroupService affiliateGroupService;
    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture fixture;

    @Test
    void pedestalInASectionIsSettledWithTheOwnerOfTheAffiliateCampsite() {
        Group affiliate = affiliateGroupService.createAffiliateSiteGroup("marko", "Marko", Market.HR,
                AccessMode.NETWORK_ACCESS);
        Group section = groupManager.createChildGroup(affiliate, "MARKO SECTION A", "A", GroupType.SITE);
        owner("marko", affiliate, "ERP-4711");
        Pedestal pedestal = fixture.pedestal("HR-MARKO-P1", section, 1);

        assertThat(userinfoService.findSettlementPartnerNo(pedestal)).isEqualTo("ERP-4711");
    }

    @Test
    void pedestalWithoutOwnerNumberHasNoSettlementPartner() {
        Group affiliate = affiliateGroupService.createAffiliateSiteGroup("marko", "Marko", Market.HR,
                AccessMode.NETWORK_ACCESS);
        owner("marko", affiliate, null);

        assertThat(userinfoService.findSettlementPartnerNo(fixture.pedestal("HR-MARKO-P1", affiliate, 1))).isNull();
        assertThat(userinfoService.findSettlementPartnerNo(fixture.pedestal("HR-NOGROUP-P1", null, 1))).isNull();
    }

    @Test
    void twoDifferentNumbersOnTheSameCampsiteGiveNoSettlementPartner() {
        Group affiliate = affiliateGroupService.createAffiliateSiteGroup("marko", "Marko", Market.HR,
                AccessMode.NETWORK_ACCESS);
        owner("marko", affiliate, "ERP-4711");
        owner("ana", affiliate, "ERP-0815");

        assertThat(userinfoService.findSettlementPartnerNo(fixture.pedestal("HR-MARKO-P1", affiliate, 1))).isNull();
    }

    private void owner(String username, Group siteGroup, String partnerNo) {
        Userinfo owner = new Userinfo(username, username, username + "@example.org");
        owner.getGroups().add(siteGroup);
        owner.setSettlementPartnerNo(partnerNo);
        userinfoDao.saveAndFlush(owner);
    }
}
