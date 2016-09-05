package io.pillopl.webservice.console.job;

import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JobWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.JOB;

    @Test
    void jobsAreListedPausedResumedAndFiredAndTheChangesAreAudited() throws Exception {
        call("admin", PATH + "/findJobs", new FindJobsRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobs[0].name").value("auditCleanupJob"))
                .andExpect(jsonPath("$.jobs[1].state").value("NORMAL"));
        call("admin", PATH + "/pauseJob", new JobActionRequest("eventRetryJob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job.state").value("PAUSED"));
        call("admin", PATH + "/runJobNow", new JobActionRequest("eventRetryJob")).andExpect(status().isConflict());
        call("admin", PATH + "/resumeJob", new JobActionRequest("eventRetryJob"))
                .andExpect(jsonPath("$.job.state").value("NORMAL"));
        call("admin", PATH + "/runJobNow", new JobActionRequest("eventRetryJob")).andExpect(status().isOk());

        List<Map<String, Object>> audited = jdbcTemplate.queryForList("SELECT ACTION_NAME FROM AUDIT "
                + "WHERE ACTION_TYPE = 'CONSOLE_ACTION' AND USERID = 'admin' ORDER BY ID");
        assertThat(audited).extracting(row -> row.get("ACTION_NAME"))
                .containsExactly("job/pauseJob", "job/runJobNow", "job/resumeJob", "job/runJobNow");
    }

    @Test
    void unknownJobsAndCallersWithoutTheRightAreRefused() throws Exception {
        call("admin", PATH + "/runJobNow", new JobActionRequest("noSuchJob")).andExpect(status().isNotFound());
        call("nobody", PATH + "/findJobs", new FindJobsRequest()).andExpect(status().isForbidden());
    }
}
