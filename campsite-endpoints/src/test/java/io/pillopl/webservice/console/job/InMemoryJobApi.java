package io.pillopl.webservice.console.job;

import io.pillopl.api.JobApi;
import io.pillopl.api.exception.AccessNotAllowedException;
import io.pillopl.api.exception.ConflictException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.dto.admin.JobDTO;
import io.pillopl.value.Username;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Stand-in of the Quartz jobs (they live in campsite-server) for the web service tests of this module: two
 * jobs, a user "nobody" without CONFIG_EDIT. The real implementation is tested in campsite-server.
 */
@Component
public class InMemoryJobApi implements JobApi {

    private final Map<String, String> states = new TreeMap<>(Map.of("eventRetryJob", "NORMAL",
            "auditCleanupJob", "NORMAL"));

    @Override
    public synchronized List<JobDTO> findJobs(Username authUsername) {
        check(authUsername);
        return states.keySet().stream().map(this::job).toList();
    }

    @Override
    public synchronized JobDTO runJobNow(Username authUsername, String jobName) {
        check(authUsername);
        if ("PAUSED".equals(existing(jobName))) {
            throw new ConflictException("Job " + jobName + " is paused");
        }
        return job(jobName);
    }

    @Override
    public synchronized JobDTO pauseJob(Username authUsername, String jobName) {
        check(authUsername);
        existing(jobName);
        states.put(jobName, "PAUSED");
        return job(jobName);
    }

    @Override
    public synchronized JobDTO resumeJob(Username authUsername, String jobName) {
        check(authUsername);
        existing(jobName);
        states.put(jobName, "NORMAL");
        return job(jobName);
    }

    private String existing(String jobName) {
        String state = states.get(jobName);
        if (state == null) {
            throw ObjectNotFoundException.of("Job", jobName);
        }
        return state;
    }

    private JobDTO job(String name) {
        return new JobDTO(name, "test job", 60L, states.get(name), null, null, false, "STANDBY");
    }

    private static void check(Username authUsername) {
        if ("nobody".equals(authUsername.value())) {
            throw new AccessNotAllowedException("User 'nobody' lacks right CONFIG_EDIT");
        }
    }
}
