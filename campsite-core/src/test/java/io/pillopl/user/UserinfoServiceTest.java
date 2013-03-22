package io.pillopl.user;

import io.pillopl.group.AccessDeniedException;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.group.IsLinkedToGroup;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.persistence.dao.UserroleDao;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserinfoServiceTest {

    private final UserinfoDao userinfoDao = mock(UserinfoDao.class);
    private final GroupService groupService = mock(GroupService.class);
    private final UserinfoService userinfoService = new UserinfoService(userinfoDao, mock(UserroleDao.class),
            groupService, mock(io.pillopl.group.AffiliateGroupService.class));

    private final Group hrStaff = group(GroupType.STAFF, 21L, "HR STAFF");
    private final Group reception = group(GroupType.STAFF, 71L, "HR RECEPTION");
    private final Group hrIssuers = group(GroupType.ISSUER, 1L, "HR ISSUERS");
    private final Group club = group(GroupType.ISSUER, 1001L, "CARAVAN CLUB");

    @Test
    void adminManagesUsersWhoseOwnGroupIsInItsStaffScope() {
        Userinfo admin = user("admin", hrStaff, hrStaff);
        Userinfo ana = user("ana", reception, reception);
        when(groupService.isManageable(hrStaff, reception)).thenReturn(true);

        assertThat(userinfoService.isManageable(admin, ana)).isTrue();
        assertThat(userinfoService.isManageable(ana, admin)).isFalse();
    }

    @Test
    void userMayManageItselfOnlyWithIntrospection() {
        Userinfo ana = user("ana", reception, null);

        assertThat(userinfoService.isManageable(ana, ana, true)).isTrue();
        assertThat(userinfoService.isManageable(ana, ana, false)).isFalse();
    }

    @Test
    void managingAUserOutsideTheScopeIsDenied() {
        Userinfo ana = user("ana", reception, reception);
        Userinfo admin = user("admin", hrStaff, hrStaff);

        assertThatThrownBy(() -> userinfoService.checkIsManageable(ana, admin, true))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void checkingAnUnknownUserIdGivesNoUser() {
        when(userinfoDao.findById(99L)).thenReturn(Optional.empty());

        assertThat(userinfoService.checkIsManageable(user("ana", reception, reception), 99L)).isNull();
    }

    @Test
    void guestIsManageableThroughTheIssuerScopeOfTheUser() {
        Userinfo ana = user("ana", reception, hrIssuers);
        IsLinkedToGroup guest = guestIn(club);
        when(groupService.isManageable(hrIssuers, club)).thenReturn(true);

        assertThat(userinfoService.isManageable(ana, guest)).isTrue();
        assertThat(userinfoService.isManageable(user("ivo", reception, reception), guest)).isFalse();
    }

    @Test
    void marketOfAUserComesFromItsOwnGroup() {
        Userinfo ana = user("ana", reception, reception);
        when(userinfoDao.findByUsername("ana")).thenReturn(ana);
        when(groupService.determineMarketByGroupId(71L)).thenReturn(io.pillopl.group.Market.HR);

        assertThat(userinfoService.determineMarket("ana")).isEqualTo(io.pillopl.group.Market.HR);
    }

    @Test
    void unknownUserOrUserWithoutGroupHasNoMarket() {
        when(userinfoDao.findByUsername("ghost")).thenReturn(null);
        when(userinfoDao.findByUsername("lost")).thenReturn(new Userinfo("lost", "Lost", "lost@example.org"));

        assertThat(userinfoService.determineMarket("ghost")).isNull();
        assertThat(userinfoService.determineMarket("lost")).isNull();
    }

    @Test
    void localPartOfAnEmailIsLowerCased() {
        assertThat(userinfoService.getLocalPartOfEmail("Ana.Horvat@Bay.example")).isEqualTo("ana.horvat");
        assertThat(userinfoService.getLocalPartOfEmail("not an email")).isNull();
    }

    private static Group group(GroupType type, Long id, String name) {
        Group group = new Group(type, name);
        group.setId(id);
        return group;
    }

    private static Userinfo user(String username, Group ownGroup, Group scope) {
        Userinfo user = new Userinfo(username, username, username + "@example.org");
        user.setGroup(ownGroup);
        if (scope != null) {
            user.getGroups().add(scope);
        }
        return user;
    }

    private static IsLinkedToGroup guestIn(Group group) {
        return new IsLinkedToGroup() {
            @Override
            public Group getGroup() {
                return group;
            }

            @Override
            public void setGroup(Group g) {
            }
        };
    }
}
