package io.pillopl.user;

import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.persistence.dao.UserroleDao;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class UserinfoDaoIntegrationTest extends IntegrationTest {

    @Autowired UserinfoDao userinfoDao;
    @Autowired UserroleDao userroleDao;
    @Autowired GroupService groupService;

    @Test
    void usersAreFoundByTheirStaffGroupAndRole() {
        Group reception = groupService.fetchStaffGroup("HR RECEPTION");
        Userrole receptionRole = userroleDao.findByName("RECEPTION");
        Userinfo ana = user("ana", "ana@bay.example", reception, receptionRole);
        user("ivo", "ivo@bay.example", reception, userroleDao.findByName("SERVICE_TECHNICIAN"));

        assertThat(userinfoDao.findByGroupsAndUserroles(List.of(reception), List.of(receptionRole))).containsExactly(ana);
        assertThat(userinfoDao.findByGroupsAndSearchAndUserroles(List.of(reception), userroleDao.findAll(), "IVO",
                PageRequest.of(0, 10)).getTotalElements()).isEqualTo(1);
    }

    @Test
    void emailIsFoundIgnoringCase() {
        Userinfo ana = user("ana", "Ana@Bay.example", groupService.fetchStaffGroup("HR RECEPTION"),
                userroleDao.findByName("RECEPTION"));

        assertThat(userinfoDao.findByEmail("ana@bay.example")).containsExactly(ana);
    }

    @Test
    void usersWithTheGroupInTheirScopeAreFound() {
        Group hrSites = groupService.fetchSiteGroup("HR SITES");
        Userinfo ana = user("ana", "ana@bay.example", groupService.fetchStaffGroup("HR RECEPTION"),
                userroleDao.findByName("RECEPTION"));
        ana.getGroups().add(hrSites);
        userinfoDao.saveAndFlush(ana);

        assertThat(userinfoDao.findByAssociatedGroup(hrSites)).containsExactly(ana);
    }

    private Userinfo user(String username, String email, Group staffGroup, Userrole role) {
        Userinfo user = new Userinfo(username, username, email);
        user.setGroup(staffGroup);
        user.getRoles().add(role);
        return userinfoDao.saveAndFlush(user);
    }
}
