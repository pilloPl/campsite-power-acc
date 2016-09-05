package io.pillopl.jobs;

import io.pillopl.api.JobApi;
import io.pillopl.api.exception.ConflictException;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.dto.admin.JobDTO;
import io.pillopl.user.PrincipalResolver;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserrightName;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.value.Username;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * The jobs of the Quartz scheduler for the administration tab (CONFIG_EDIT). A job is paused and resumed with all
 * its triggers; a paused or running job is not fired by hand. The scheduler keeps its jobs in memory: a pause lasts
 * until the restart, the intervals come from SYSTEM_PROPERTY at start-up.
 */
@Service
@Transactional(readOnly = true)
public class JobControlService implements JobApi {

    private static final Logger LOG = LoggerFactory.getLogger(JobControlService.class);

    static final String STARTED = "STARTED";

    static final String STANDBY = "STANDBY";

    private final Scheduler scheduler;

    private final PrincipalResolver principalResolver;

    public JobControlService(Scheduler scheduler, PrincipalResolver principalResolver) {
        this.scheduler = scheduler;
        this.principalResolver = principalResolver;
    }

    @Override
    public List<JobDTO> findJobs(Username authUsername) {
        try {
            principalResolver.resolve(authUsername, UserrightName.CONFIG_EDIT);
            List<JobKey> keys = new ArrayList<>(scheduler.getJobKeys(GroupMatcher.anyJobGroup()));
            keys.sort(Comparator.comparing(JobKey::getName));
            List<JobKey> running = runningJobs();
            List<JobDTO> jobs = new ArrayList<>();
            for (JobKey key : keys) {
                jobs.add(toDTO(key, running));
            }
            return jobs;
        } catch (SchedulerException e) {
            throw schedulerFailed(e);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public JobDTO runJobNow(Username authUsername, String jobName) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CONFIG_EDIT);
            JobKey key = existingJob(jobName);
            if (isPaused(key)) {
                throw new ConflictException("Job " + jobName + " is paused: resume it first");
            }
            if (runningJobs().contains(key)) {
                throw new ConflictException("Job " + jobName + " is running");
            }
            scheduler.triggerJob(key);
            LOG.info(String.format("Job %s fired by %s", jobName, principal.getUsername()));
            return toDTO(key, runningJobs());
        } catch (SchedulerException e) {
            throw schedulerFailed(e);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public JobDTO pauseJob(Username authUsername, String jobName) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CONFIG_EDIT);
            JobKey key = existingJob(jobName);
            scheduler.pauseJob(key);
            LOG.info(String.format("Job %s paused by %s", jobName, principal.getUsername()));
            return toDTO(key, runningJobs());
        } catch (SchedulerException e) {
            throw schedulerFailed(e);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public JobDTO resumeJob(Username authUsername, String jobName) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.CONFIG_EDIT);
            JobKey key = existingJob(jobName);
            scheduler.resumeJob(key);
            LOG.info(String.format("Job %s resumed by %s", jobName, principal.getUsername()));
            return toDTO(key, runningJobs());
        } catch (SchedulerException e) {
            throw schedulerFailed(e);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    private JobKey existingJob(String jobName) throws SchedulerException {
        if (jobName == null || jobName.isBlank()) {
            throw new InvalidInputException("Job name is missing");
        }
        JobKey key = JobKey.jobKey(jobName.trim());
        if (!scheduler.checkExists(key)) {
            throw ObjectNotFoundException.of("Job", jobName);
        }
        return key;
    }

    private boolean isPaused(JobKey key) throws SchedulerException {
        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(key);
        if (triggers.isEmpty()) {
            return false;
        }
        for (Trigger trigger : triggers) {
            if (scheduler.getTriggerState(trigger.getKey()) != Trigger.TriggerState.PAUSED) {
                return false;
            }
        }
        return true;
    }

    private List<JobKey> runningJobs() throws SchedulerException {
        List<JobKey> running = new ArrayList<>();
        for (JobExecutionContext context : scheduler.getCurrentlyExecutingJobs()) {
            running.add(context.getJobDetail().getKey());
        }
        return running;
    }

    private JobDTO toDTO(JobKey key, List<JobKey> running) throws SchedulerException {
        JobDetail detail = scheduler.getJobDetail(key);
        // the schedule is the trigger "<job>Trigger" of JobConfiguration; runs fired by hand add one-off triggers
        Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(key.getName() + "Trigger"));
        if (trigger == null) {
            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(key);
            trigger = triggers.isEmpty() ? null : triggers.get(0);
        }
        Long interval = trigger instanceof SimpleTrigger simple && simple.getRepeatInterval() > 0
                ? simple.getRepeatInterval() / 1000 : null;
        String state = trigger != null ? scheduler.getTriggerState(trigger.getKey()).name() : "NONE";
        String schedulerState = scheduler.isStarted() && !scheduler.isInStandbyMode() ? STARTED : STANDBY;
        return new JobDTO(key.getName(), detail != null ? detail.getDescription() : null, interval, state,
                trigger != null ? toLocal(trigger.getPreviousFireTime()) : null,
                trigger != null ? toLocal(trigger.getNextFireTime()) : null, running.contains(key), schedulerState);
    }

    private static LocalDateTime toLocal(Date date) {
        return date != null ? LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()) : null;
    }

    private static RuntimeException schedulerFailed(SchedulerException e) {
        LOG.error("Scheduler call failed", e);
        return new SchedulerAccessException("Scheduler call failed: " + e.getMessage(), e);
    }

    /**
     * The scheduler itself failed (a technical error for the console).
     */
    static final class SchedulerAccessException extends RuntimeException {

        SchedulerAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
