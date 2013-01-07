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
    private final GroupService groupService = new GroupService(groupDao, new GroupManager(groupDao));

    private final Group sites = groups.root(GroupType.SITE, "HR SITES");
    private final Group networkAccess = groups.child(sites, "NETWORK ACCESS HR");
    private final Group affiliate = groups.child(networkAccess, "AFFILIATE marko");
    private final Group otherSites = groups.root(GroupType.SITE, "ADRIATIC HOLDING");

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
}
