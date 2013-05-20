package io.pillopl.user;

import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.persistence.dao.UserroleDao;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Not transactional: the materialized view is refreshed with committed data; the users are removed afterwards.
 */
class UserinfoSummaryIntegrationTest extends IntegrationTest {

    @Autowired UserinfoSummaryService userinfoSummaryService;
    @Autowired UserinfoDao userinfoDao;
    @Autowired UserroleDao userroleDao;
    @Autowired GroupService groupService;
    @Autowired TransactionTemplate transactionTemplate;
    @Autowired JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM USERINFO_USERROLE WHERE USERINFO_ID IN (SELECT ID FROM USERINFO WHERE USERNAME LIKE 'mv-%')");
        jdbcTemplate.update("DELETE FROM USERINFO_GROUP WHERE USERINFO_ID IN (SELECT ID FROM USERINFO WHERE USERNAME LIKE 'mv-%')");
        jdbcTemplate.update("DELETE FROM USERINFO WHERE USERNAME LIKE 'mv-%'");
        userinfoSummaryService.refresh();
    }

    @Test
    void summaryListsSelfServiceUsersWithTheirRoleFlagsAfterARefresh() {
        transactionTemplate.executeWithoutResult(status -> {
            Group selfService = groupService.fetchStaffGroup("HR SELF SERVICE");
            user("mv-marko", selfService, "AFFILIATE_OWNER", "AFFILIATE_PRICE_REPORTING");
            user("mv-vesna", selfService, "SEASONAL_RESIDENT");
            user("mv-ana", groupService.fetchStaffGroup("HR RECEPTION"), "RECEPTION");
        });

        userinfoSummaryService.refresh();

        List<UserinfoSummary> rows = transactionTemplate.execute(status -> userinfoSummaryService
                .findSelfServiceUsers(admin(), null, null, PageRequest.of(0, 10)).getContent());
        assertThat(rows).extracting(UserinfoSummary::getUsername).containsExactly("mv-marko", "mv-vesna");
        assertThat(rows.get(0).isAffiliateOwner()).isTrue();
        assertThat(rows.get(0).isSeasonalResident()).isFalse();
        assertThat(rows.get(1).isSeasonalResident()).isTrue();

        List<UserinfoSummary> owners = transactionTemplate.execute(status -> userinfoSummaryService
                .findSelfServiceUsers(admin(), true, null, PageRequest.of(0, 10)).getContent());
        assertThat(owners).extracting(UserinfoSummary::getUsername).containsExactly("mv-marko");
    }

    @Test
    void userWithoutStaffScopeSeesNobody() {
        Userinfo nobody = new Userinfo("mv-nobody", "Nobody", "nobody@example.org");

        assertThat(userinfoSummaryService.findSelfServiceUsers(nobody, null, null, PageRequest.of(0, 10))).isEmpty();
    }

    private Userinfo admin() {
        Userinfo admin = new Userinfo("mv-admin", "Admin", "admin@example.org");
        admin.getGroups().add(groupService.fetchStaffGroup("HR STAFF"));
        return admin;
    }

    private void user(String username, Group group, String... roles) {
        Userinfo user = new Userinfo(username, username, username + "@example.org");
        user.setGroup(group);
        for (String role : roles) {
            user.getRoles().add(userroleDao.findByName(role));
        }
        userinfoDao.save(user);
    }
}
