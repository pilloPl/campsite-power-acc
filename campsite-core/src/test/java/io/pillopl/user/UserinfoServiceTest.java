package io.pillopl.user;

import io.pillopl.group.AccessDeniedException;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.group.IsLinkedToGroup;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.persistence.dao.UserroleDao;
import io.pillopl.config.SystemPropertyService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserinfoServiceTest {

    private final UserinfoDao userinfoDao = mock(UserinfoDao.class);
    private final GroupService groupService = mock(GroupService.class);
    private final UserroleDao userroleDao = mock(UserroleDao.class);
    private final SystemPropertyService systemPropertyService = mock(SystemPropertyService.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final UserinfoService userinfoService = new UserinfoService(userinfoDao, userroleDao,
            groupService, mock(io.pillopl.group.AffiliateGroupService.class), passwordEncoder, systemPropertyService);

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
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("'ana'")
                .hasMessageContaining("'admin'");
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
    void renamingAnUnknownUserIsRefusedAsNotFound() {
        when(userinfoDao.findByUsername("ghost")).thenReturn(null);

        assertThatThrownBy(() -> userinfoService.changeName("ghost", "Ghost Rider"))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessageContaining("'ghost'");
    }

    @Test
    void localPartOfAnEmailIsLowerCased() {
        assertThat(userinfoService.getLocalPartOfEmail("Ana.Horvat@Bay.example")).isEqualTo("ana.horvat");
        assertThat(userinfoService.getLocalPartOfEmail("not an email")).isNull();
    }

    @Test
    void failedLoginsAreCountedAndTheMaximumLocksAnActiveUser() {
        Userinfo ana = user("ana", reception, reception);
        ana.setStatus(UserinfoStatus.ACTIVE);
        when(userinfoDao.findByUsername("ana")).thenReturn(ana);
        when(systemPropertyService.getMaxFailedLogins()).thenReturn(2);

        assertThat(userinfoService.recordFailedLogin("ana")).isFalse();
        assertThat(userinfoService.recordFailedLogin("ana")).isTrue();
        assertThat(ana.getStatus()).isEqualTo(UserinfoStatus.LOCKED);
        assertThat(userinfoService.recordFailedLogin("ana")).as("already locked").isFalse();
        assertThat(ana.getFailedLogins()).isEqualTo(3);
        assertThat(userinfoService.recordFailedLogin("nobody")).isFalse();

        userinfoService.recordSuccessfulLogin("ana");
        assertThat(ana.getFailedLogins()).isZero();
        assertThat(ana.getLastLoginDate()).isNotNull();
    }

    @Test
    void passwordIsStoredAsBcryptAndActivatesAnInvitedUser() {
        Userinfo admin = user("admin", hrStaff, hrStaff);
        Userinfo ana = user("ana", reception, reception);
        when(userinfoDao.findByUsername("ana")).thenReturn(ana);
        when(groupService.isManageable(hrStaff, reception)).thenReturn(true);

        userinfoService.setPassword(admin, "ana", "long-enough");

        assertThat(ana.getPasswordHash()).startsWith("$2a$");
        assertThat(passwordEncoder.matches("long-enough", ana.getPasswordHash())).isTrue();
        assertThat(ana.getStatus()).isEqualTo(UserinfoStatus.ACTIVE);
        assertThat(ana.canLogIn()).isTrue();
    }

    @Test
    void passwordsThatAreShortOrTheUsernameAreRefused() {
        Userinfo ana = user("anastasia", reception, reception);
        when(userinfoDao.findByUsername("anastasia")).thenReturn(ana);

        assertThatThrownBy(() -> userinfoService.setPassword(ana, "anastasia", "short"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userinfoService.setPassword(ana, "anastasia", "ANASTASIA"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userinfoService.setPassword(ana, "nobody", "long-enough"))
                .isInstanceOf(io.pillopl.api.exception.ObjectNotFoundException.class);
    }

    @Test
    void statusChangesRefuseInvitedActivationWithoutPasswordAndOwnAccount() {
        Userinfo admin = user("admin", hrStaff, hrStaff);
        Userinfo ana = user("ana", reception, reception);
        when(userinfoDao.findByUsername("ana")).thenReturn(ana);
        when(userinfoDao.findByUsername("admin")).thenReturn(admin);
        when(groupService.isManageable(hrStaff, reception)).thenReturn(true);

        assertThatThrownBy(() -> userinfoService.changeStatus(admin, "ana", UserinfoStatus.INVITED))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userinfoService.changeStatus(admin, "ana", UserinfoStatus.ACTIVE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userinfoService.changeStatus(admin, "admin", UserinfoStatus.LOCKED))
                .isInstanceOf(AccessDeniedException.class);

        ana.setPasswordHash(passwordEncoder.encode("long-enough"));
        ana.setFailedLogins(4);
        userinfoService.changeStatus(admin, "ana", UserinfoStatus.ACTIVE);
        assertThat(ana.getStatus()).isEqualTo(UserinfoStatus.ACTIVE);
        assertThat(ana.getFailedLogins()).isZero();
    }

    @Test
    void onlyRolesTheAdministratorHoldsCanBeGiven() {
        Userinfo admin = user("admin", hrStaff, hrStaff);
        Userrole reception = new Userrole(UserroleName.RECEPTION);
        Userrole marketAdmin = new Userrole(UserroleName.MARKET_ADMIN);
        admin.getRoles().add(reception);
        Userinfo ana = user("ana", this.reception, this.reception);
        when(userinfoDao.findByUsername("ana")).thenReturn(ana);
        when(groupService.isManageable(hrStaff, this.reception)).thenReturn(true);
        when(userroleDao.findByName("RECEPTION")).thenReturn(reception);
        when(userroleDao.findByName("MARKET_ADMIN")).thenReturn(marketAdmin);

        userinfoService.replaceRoles(admin, "ana", java.util.List.of("RECEPTION"));
        assertThat(ana.getRoles()).containsExactly(reception);
        assertThatThrownBy(() -> userinfoService.replaceRoles(admin, "ana", java.util.List.of("MARKET_ADMIN")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> userinfoService.replaceRoles(admin, "ana", java.util.List.of("NO_SUCH_ROLE")))
                .isInstanceOf(IllegalArgumentException.class);
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
