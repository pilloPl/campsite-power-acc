package io.pillopl.api;

import io.pillopl.dto.admin.JobDTO;
import io.pillopl.value.Username;

import java.util.List;

/**
 * Scheduled jobs of the pedestal backend in the administration tab. CONFIG_EDIT for all calls.
 */
public interface JobApi {

    List<JobDTO> findJobs(Username authUsername);

    /**
     * Fires the job once now, besides its schedule (a running job is not started twice).
     */
    JobDTO runJobNow(Username authUsername, String jobName);

    /**
     * The job does not run on its schedule until resumed (also not through runJobNow).
     */
    JobDTO pauseJob(Username authUsername, String jobName);

    JobDTO resumeJob(Username authUsername, String jobName);
}
