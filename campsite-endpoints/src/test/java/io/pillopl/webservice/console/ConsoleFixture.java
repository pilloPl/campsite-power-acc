package io.pillopl.webservice.console;

import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.persistence.dao.UserroleDao;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserinfoStatus;
import io.pillopl.user.UserroleName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Console users with roles and scope groups for the web service tests.
 */
@Component
public class ConsoleFixture {

    @Autowired
    UserinfoDao userinfoDao;

    @Autowired
    UserroleDao userroleDao;

    @Autowired
    GroupService groupService;

    /**
     * Market administrator of HR: all rights, scope HR STAFF / HR SITES / HR ISSUERS.
     */
    public Userinfo hrAdmin(String username) {
        return user(username, UserroleName.MARKET_ADMIN, staff("HR STAFF"), staff("HR STAFF"), site("HR SITES"),
                groupService.fetchIssuerGroup("HR ISSUERS"));
    }

    public Userinfo user(String username, UserroleName role, Group ownGroup, Group... scope) {
        Userinfo user = new Userinfo(username, username, username + "@example.org");
        user.setStatus(UserinfoStatus.ACTIVE);
        user.setGroup(ownGroup);
        for (Group group : scope) {
            user.getGroups().add(group);
        }
        if (role != null) {
            user.getRoles().add(userroleDao.findByName(role.name()));
        }
        return userinfoDao.saveAndFlush(user);
    }

    public Group staff(String name) {
        return groupService.fetchStaffGroup(name);
    }

    public Group issuer(String name) {
        return groupService.fetchIssuerGroup(name);
    }

    public Group site(String name) {
        return groupService.fetchSiteGroup(name);
    }
}
