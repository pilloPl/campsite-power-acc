package io.pillopl.infrastructure;

import io.pillopl.group.AccessMode;
import io.pillopl.group.AffiliateGroupService;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.Market;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class AccessModeSwitchIntegrationTest extends IntegrationTest {

    @Autowired AffiliateGroupService affiliateGroupService;
    @Autowired GroupService groupService;
    @Autowired InfrastructureFixture fixture;

    @Test
    void switchToNetworkAccessMovesTheCampsiteAndListsItsPedestalsAsRequested() {
        Group affiliate = affiliateGroupService.createAffiliateSiteGroup("marko", "Marko", Market.HR,
                AccessMode.HOUSE_CARDS_ONLY);
        Pedestal pedestal = fixture.pedestal("HR-MARKO-P1", affiliate, 2);
        pedestal.setListedInApp(false);

        AccessMode mode = affiliateGroupService.switchAccessMode(affiliate.getId(), AccessMode.NETWORK_ACCESS, true);

        assertThat(mode).isEqualTo(AccessMode.NETWORK_ACCESS);
        assertThat(affiliate.getParent().getName()).isEqualTo("NETWORK ACCESS HR");
        assertThat(pedestal.isListedInApp()).isTrue();
        assertThat(pedestal.isRemoteControl()).isTrue();
    }

    @Test
    void networkAccessCampsiteNotListedInTheAppIsNotRemoteControlled() {
        Group affiliate = affiliateGroupService.createAffiliateSiteGroup("marko", "Marko", Market.HR,
                AccessMode.HOUSE_CARDS_ONLY);
        Pedestal pedestal = fixture.pedestal("HR-MARKO-P1", affiliate, 1);

        affiliateGroupService.switchAccessMode(affiliate.getId(), AccessMode.NETWORK_ACCESS, false);

        assertThat(pedestal.isListedInApp()).isFalse();
        assertThat(pedestal.isRemoteControl()).isFalse();
    }

    @Test
    void switchToHouseCardsOnlyHidesThePedestalsAndStopsCampsiteSettlement() {
        Group affiliate = affiliateGroupService.createAffiliateSiteGroup("ana", "Ana", Market.FR,
                AccessMode.NETWORK_ACCESS);
        Pedestal pedestal = fixture.pedestal("FR-ANA-P1", affiliate, 1);
        pedestal.setSettledByCampsite(true);

        AccessMode mode = affiliateGroupService.switchAccessMode(affiliate.getId(), AccessMode.HOUSE_CARDS_ONLY, true);

        assertThat(mode).isEqualTo(AccessMode.HOUSE_CARDS_ONLY);
        assertThat(affiliate.getParent().getName()).isEqualTo("HOUSE CARDS ONLY FR");
        assertThat(pedestal.isListedInApp()).isFalse();
        assertThat(pedestal.isRemoteControl()).isFalse();
        assertThat(pedestal.isSettledByCampsite()).isFalse();
    }

    @Test
    void groupThatIsNoAffiliateCampsiteIsNotSwitched() {
        Group networkAccess = groupService.fetchSiteGroup("NETWORK ACCESS HR");

        assertThat(affiliateGroupService.switchAccessMode(networkAccess.getId(), AccessMode.HOUSE_CARDS_ONLY, false))
                .isNull();
        assertThat(networkAccess.getParent().getName()).isEqualTo("HR SITES");
    }
}
