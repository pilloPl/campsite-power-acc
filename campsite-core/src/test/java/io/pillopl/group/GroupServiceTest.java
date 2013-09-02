package io.pillopl.group;

import io.pillopl.persistence.dao.GroupDao;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupServiceTest {

    private final InMemoryGroups groups = new InMemoryGroups();
    private final GroupDao groupDao = mock(GroupDao.class);
    private final GroupService groupService = new GroupService(groupDao, new GroupManager(groupDao),
            mock(io.pillopl.persistence.dao.UserinfoDao.class), mock(io.pillopl.persistence.dao.PedestalDao.class),
            mock(io.pillopl.persistence.dao.GuestDao.class));

    private final Group sites = groups.root(GroupType.SITE, "HR SITES");
    private final Group networkAccess = groups.child(sites, "NETWORK ACCESS HR");
    private final Group affiliate = groups.child(networkAccess, "AFFILIATE marko");
    private final Group otherSites = groups.root(GroupType.SITE, "ADRIATIC HOLDING");

    @Test
    void marketIsTakenFromTheNearestGroupWithAMarketPrefix() {
        when(groupDao.findById(affiliate.getId())).thenReturn(Optional.of(affiliate));
        when(groupDao.findById(sites.getId())).thenReturn(Optional.of(sites));

        assertThat(groupService.determineMarketByGroupId(affiliate.getId())).isEqualTo(Market.HR);
        assertThat(groupService.determineMarketByGroupId(sites.getId())).isEqualTo(Market.HR);
        assertThat(groupService.findMarketFromParentGroups(affiliate.getId())).isEqualTo("HR");
    }

    @Test
    void groupWithoutMarketPrefixOnItsLineHasNoMarket() {
        when(groupDao.findById(otherSites.getId())).thenReturn(Optional.of(otherSites));

        assertThat(groupService.determineMarketByGroupId(otherSites.getId())).isNull();
        assertThat(groupService.findMarketFromParentGroups(otherSites.getId())).isNull();
    }

    @Test
    void unknownGroupHasNoMarket() {
        when(groupDao.findById(99L)).thenReturn(Optional.empty());

        assertThat(groupService.determineMarketByGroupId(99L)).isNull();
        assertThat(groupService.determineMarketByGroupId(null)).isNull();
    }

    @Test
    void groupManagesItsSubtree() {
        when(groupDao.findManageable(networkAccess.getId())).thenReturn(List.of(networkAccess.getId(), affiliate.getId()));

        assertThat(groupService.isManageable(networkAccess, affiliate)).isTrue();
        assertThat(groupService.isManageable(networkAccess, sites)).isFalse();
    }

    @Test
    void missingAdminGroupManagesNothing() {
        assertThat(groupService.isManageable(null, affiliate)).isFalse();
    }

    @Test
    void managingAGroupOutsideTheSubtreeIsDenied() {
        when(groupDao.findManageable(networkAccess.getId())).thenReturn(List.of(networkAccess.getId(), affiliate.getId()));

        assertThat(groupService.checkIsManageable(networkAccess, affiliate)).isTrue();
        assertThatThrownBy(() -> groupService.checkIsManageable(networkAccess, sites))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("NETWORK ACCESS HR")
                .hasMessageContaining("HR SITES");
    }

    @Test
    void cardsAreAcceptedOnlyInTheSubtreesOfTheEffectiveAgreements() {
        Group issuers = groups.root(GroupType.ISSUER, "HR ISSUERS");
        Group club = groups.child(issuers, "CARAVAN CLUB");
        InMemoryGroups.agree(club, networkAccess);

        assertThat(groupService.isAcceptedAt(club, affiliate.getId())).isTrue();
        assertThat(groupService.isAcceptedAt(club, sites.getId())).isFalse();
        assertThat(groupService.isAcceptedAt(issuers, affiliate.getId())).isFalse();
        assertThat(groupService.isAcceptedAt(null, affiliate.getId())).isFalse();
    }

    @Test
    void parentLineGoesFromTheGroupToTheRootOrBack() {
        when(groupDao.findById(affiliate.getId())).thenReturn(Optional.of(affiliate));

        assertThat(groupService.fetchParentLineIds(affiliate.getId(), true))
                .containsExactly(affiliate.getId(), networkAccess.getId(), sites.getId());
        assertThat(groupService.fetchParentLineIds(affiliate.getId(), false))
                .containsExactly(sites.getId(), networkAccess.getId(), affiliate.getId());
        assertThat(groupService.fetchParentLineIds(null, true)).isEmpty();
    }

    @Test
    void childGroupMustHaveTheTypeOfItsParent() {
        GroupManager groupManager = new GroupManager(groupDao);

        assertThatThrownBy(() -> groupManager.createChildGroup(sites, "RECEPTION", "Reception", GroupType.STAFF))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Group type mismatch");
    }

    @Test
    void agreementsOnlyLeadFromIssuerToSite() {
        GroupManager groupManager = new GroupManager(groupDao);
        Group issuers = groups.root(GroupType.ISSUER, "HR ISSUERS");

        assertThatThrownBy(() -> groupManager.addAcceptanceAgreement(sites, networkAccess))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> groupManager.addAcceptanceAgreement(issuers, issuers))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
