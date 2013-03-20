package io.pillopl.user;

import io.pillopl.group.AccessDeniedException;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.Market;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.persistence.dao.UserroleDao;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class UserinfoServiceIntegrationTest extends IntegrationTest {

    @Autowired UserinfoService userinfoService;
    @Autowired UserinfoDao userinfoDao;
    @Autowired UserroleDao userroleDao;
    @Autowired GroupService groupService;

    @Test
    void newUserGetsTheLocalPartOfTheEmailAsUsernameAndASuffixWhenTaken() {
        Userinfo admin = marketAdmin("HR STAFF");

        Userinfo first = userinfoService.createUser(admin, "Marko@bay.example", "Marko", Market.HR);
        Userinfo second = userinfoService.createUser(admin, "marko@cove.example", "Marko", Market.HR);

        assertThat(first.getUsername()).isEqualTo("marko");
        assertThat(second.getUsername()).isEqualTo("marko-1");
        assertThat(first.getGroup().getName()).isEqualTo("HR SELF SERVICE");
        assertThat(first.getEmailLocale()).isEqualTo("hr");
        assertThat(first.containsRole(UserroleName.SELF_SERVICE_USER)).isTrue();
        assertThat(first.getStatus()).isEqualTo(UserinfoStatus.INVITED);
    }

    @Test
    void userWithAnExistingEmailIsNotCreatedTwice() {
        Userinfo admin = marketAdmin("HR STAFF");
        userinfoService.createUser(admin, "ana@bay.example", "Ana", Market.HR);

        assertThatThrownBy(() -> userinfoService.createUser(admin, "ANA@bay.example", "Ana", Market.HR))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void marketOfThePrincipalIsUsedWhenNoneIsGiven() {
        Userinfo admin = marketAdmin("IT STAFF");

        assertThat(userinfoService.createUser(admin, "luca@lago.example", null, null).getGroup().getName())
                .isEqualTo("IT SELF SERVICE");
    }

    @Test
    void adminOfAnotherMarketMayNotCreateUsersInThisMarket() {
        Userinfo admin = marketAdmin("IT STAFF");

        assertThatThrownBy(() -> userinfoService.createUser(admin, "ana@bay.example", "Ana", Market.HR))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void selfServiceUsersOfTheAdminsSubtreeAreListed() {
        Userinfo admin = marketAdmin("HR STAFF");
        Userinfo marko = userinfoService.createUser(admin, "marko@bay.example", "Marko", Market.HR);
        marketAdmin("FR STAFF");

        assertThat(userinfoService.findSelfServiceUsers(admin.getUsername())).containsExactly(marko);
        assertThat(userinfoService.findByRoles(admin, List.of(userroleDao.findByName("MARKET_ADMIN"))))
                .containsExactly(admin);
    }

    private Userinfo marketAdmin(String staffRoot) {
        Group root = groupService.fetchStaffGroup(staffRoot);
        Userinfo admin = new Userinfo("admin " + staffRoot, "Admin", staffRoot.replace(' ', '.') + "@network.example");
        admin.setGroup(root);
        admin.getGroups().add(root);
        admin.getRoles().add(userroleDao.findByName("MARKET_ADMIN"));
        return userinfoDao.saveAndFlush(admin);
    }
}
