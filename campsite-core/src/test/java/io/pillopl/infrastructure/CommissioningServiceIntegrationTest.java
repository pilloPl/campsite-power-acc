package io.pillopl.infrastructure;

import io.pillopl.group.AccessMode;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.test.IntegrationTest;
import io.pillopl.user.Userinfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class CommissioningServiceIntegrationTest extends IntegrationTest {

    @Autowired CommissioningService commissioningService;
    @Autowired GroupService groupService;
    @Autowired UserinfoDao userinfoDao;

    @Test
    void installerOfTheMarketFindsTheOpenCommissioningByToken() {
        Userinfo installer = staff("ivo", "HR STAFF");
        Commissioning commissioning = commissioningService.create(commissioningFor(owner("marko", "HR SELF SERVICE")));

        assertThat(commissioning.getToken()).isNotBlank();
        assertThat(commissioningService.findByToken("ivo", commissioning.getToken())).isEqualTo(commissioning);
        assertThat(commissioningService.findAll(installer.getUsername())).containsExactly(commissioning);
    }

    @Test
    void installerOfAnotherMarketDoesNotGetTheCommissioning() {
        staff("luca", "IT STAFF");
        Commissioning commissioning = commissioningService.create(commissioningFor(owner("marko", "HR SELF SERVICE")));

        assertThat(commissioningService.findByToken("luca", commissioning.getToken())).isNull();
        assertThat(commissioningService.findAll("luca")).isEmpty();
    }

    @Test
    void finishedCommissioningCannotBeUsedAgain() {
        staff("ivo", "HR STAFF");
        Commissioning commissioning = commissioningService.create(commissioningFor(owner("marko", "HR SELF SERVICE")));

        commissioningService.finish(commissioning);

        assertThat(commissioning.isDeleted()).isTrue();
        assertThat(commissioning.getDeleteDate()).isNotNull();
        assertThat(commissioningService.findByToken("ivo", commissioning.getToken())).isNull();
    }

    @Test
    void unknownInstallerOrTokenFindsNothing() {
        staff("ivo", "HR STAFF");

        assertThat(commissioningService.findByToken("ghost", "x")).isNull();
        assertThat(commissioningService.findByToken("ivo", "x")).isNull();
        assertThat(commissioningService.findAll("ghost")).isEmpty();
    }

    private Commissioning commissioningFor(Userinfo owner) {
        Commissioning commissioning = new Commissioning();
        commissioning.setAffiliateOwner(owner);
        Location location = new Location("Rovinj", "52210", "Obala", "12");
        location.setCampsiteCode("HRMARKO");
        commissioning.setLocation(location);
        commissioning.setAccessMode(AccessMode.NETWORK_ACCESS);
        commissioning.setListedInApp(true);
        return commissioning;
    }

    private Userinfo staff(String username, String staffGroup) {
        Group group = groupService.fetchStaffGroup(staffGroup);
        Userinfo user = new Userinfo(username, username, username + "@example.org");
        user.setGroup(group);
        user.getGroups().add(group);
        return userinfoDao.saveAndFlush(user);
    }

    private Userinfo owner(String username, String staffGroup) {
        Userinfo user = new Userinfo(username, username, username + "@example.org");
        user.setGroup(groupService.fetchStaffGroup(staffGroup));
        return userinfoDao.saveAndFlush(user);
    }
}
