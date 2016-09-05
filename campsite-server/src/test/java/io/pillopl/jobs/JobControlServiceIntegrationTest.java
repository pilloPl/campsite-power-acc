package io.pillopl.jobs;

import io.pillopl.api.exception.AccessNotAllowedException;
import io.pillopl.api.exception.ConflictException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.dto.admin.JobDTO;
import io.pillopl.group.GroupService;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.persistence.dao.UserroleDao;
import io.pillopl.test.IntegrationTest;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserinfoStatus;
import io.pillopl.user.UserroleName;
import io.pillopl.value.Username;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The jobs of the real Quartz scheduler (not started in the tests: STANDBY).
 */
class JobControlServiceIntegrationTest extends IntegrationTest {

    static final Username ADMIN = Username.of("jobadmin");

    @Autowired JobControlService jobControlService;
    @Autowired Scheduler scheduler;
    @Autowired UserinfoDao userinfoDao;
    @Autowired UserroleDao userroleDao;
    @Autowired GroupService groupService;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        user("jobadmin", UserroleName.MARKET_ADMIN);
        user("jobdesk", UserroleName.RECEPTION);
    }

    @AfterEach
    void tearDown() throws SchedulerException {
        scheduler.resumeJob(JobKey.jobKey(JobConfiguration.AUDIT_CLEANUP));
        for (String username : List.of("jobadmin", "jobdesk")) {
            jdbcTemplate.update("DELETE FROM USERINFO_USERROLE WHERE USERINFO_ID IN "
                    + "(SELECT ID FROM USERINFO WHERE USERNAME = ?)", username);
            jdbcTemplate.update("DELETE FROM USERINFO WHERE USERNAME = ?", username);
        }
    }

    @Test
    void everyJobIsListedWithDescriptionIntervalAndState() {
        List<JobDTO> jobs = jobControlService.findJobs(ADMIN);

        assertThat(jobs).hasSize(JobConfiguration.DESCRIPTIONS.size());
        assertThat(jobs).extracting(JobDTO::description).doesNotContainNull();
        JobDTO eventRetry = jobs.stream().filter(j -> j.name().equals(JobConfiguration.EVENT_RETRY)).findFirst()
                .orElseThrow();
        assertThat(eventRetry.intervalSeconds()).isEqualTo(60L);
        assertThat(eventRetry.state()).isEqualTo("NORMAL");
        assertThat(eventRetry.running()).isFalse();
        assertThat(eventRetry.schedulerState()).isEqualTo(JobControlService.STANDBY);
        assertThat(eventRetry.nextFireTime()).isNotNull();
    }

    @Test
    void pausedJobIsNotFiredByHandUntilResumed() {
        assertThat(jobControlService.pauseJob(ADMIN, JobConfiguration.AUDIT_CLEANUP).state()).isEqualTo("PAUSED");
        assertThatThrownBy(() -> jobControlService.runJobNow(ADMIN, JobConfiguration.AUDIT_CLEANUP))
                .isInstanceOf(ConflictException.class);

        assertThat(jobControlService.resumeJob(ADMIN, JobConfiguration.AUDIT_CLEANUP).state()).isEqualTo("NORMAL");
        assertThat(jobControlService.runJobNow(ADMIN, JobConfiguration.AUDIT_CLEANUP).name())
                .isEqualTo(JobConfiguration.AUDIT_CLEANUP);
    }

    @Test
    void unknownJobsAndUsersWithoutConfigEditAreRefused() {
        assertThatThrownBy(() -> jobControlService.pauseJob(ADMIN, "noSuchJob"))
                .isInstanceOf(ObjectNotFoundException.class);
        assertThatThrownBy(() -> jobControlService.findJobs(Username.of("jobdesk")))
                .isInstanceOf(AccessNotAllowedException.class);
    }

    private void user(String username, UserroleName role) {
        Userinfo user = new Userinfo(username, username, username + "@example.org");
        user.setStatus(UserinfoStatus.ACTIVE);
        user.setGroup(groupService.fetchStaffGroup("HR STAFF"));
        user.getRoles().add(userroleDao.findByName(role.name()));
        userinfoDao.saveAndFlush(user);
    }
}
