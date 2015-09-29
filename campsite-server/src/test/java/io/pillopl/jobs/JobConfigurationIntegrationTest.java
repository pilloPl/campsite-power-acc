package io.pillopl.jobs;

import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZoneId;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JobConfigurationIntegrationTest extends IntegrationTest {

    @Autowired
    Scheduler scheduler;

    @Test
    void staleSessionJobRunsInTheIntervalConfiguredInTheDatabase() throws SchedulerException {
        assertThat(scheduler.checkExists(JobKey.jobKey(JobConfiguration.CLOSE_SESSIONS_ON_CHECKOUT))).isTrue();
        SimpleTrigger trigger = (SimpleTrigger) scheduler.getTrigger(
                TriggerKey.triggerKey(JobConfiguration.CLOSE_SESSIONS_ON_CHECKOUT + "Trigger"));
        assertThat(trigger.getRepeatInterval()).isEqualTo(300_000L);
        assertThat(scheduler.isStarted()).isFalse();
    }

    @Test
    void monitoringMaintenanceAndCardJobsRunInTheirConfiguredIntervals() throws SchedulerException {
        assertThat(repeatIntervalInSeconds(JobConfiguration.PEDESTAL_HEARTBEAT_TIMEOUT)).isEqualTo(60);
        assertThat(repeatIntervalInSeconds(JobConfiguration.UNKNOWN_STATE_EVENT_TRIGGER)).isEqualTo(300);
        assertThat(repeatIntervalInSeconds(JobConfiguration.SOCKET_UTILIZATION_REFRESH)).isEqualTo(3600);
        assertThat(repeatIntervalInSeconds(JobConfiguration.USERINFO_SUMMARY_REFRESH)).isEqualTo(120);
        assertThat(repeatIntervalInSeconds(JobConfiguration.LOCAL_CARD_LIST_DISTRIBUTION)).isEqualTo(3600);
        assertThat(repeatIntervalInSeconds(JobConfiguration.AUDIT_CLEANUP)).isEqualTo(86400);
        assertThat(repeatIntervalInSeconds(JobConfiguration.GROUP_VALIDITY_CONTROL)).isEqualTo(86400);
        assertThat(repeatIntervalInSeconds(JobConfiguration.ARCHIVE_QRPAY_PROCESS)).isEqualTo(3600);
        assertThat(repeatIntervalInSeconds(JobConfiguration.EVENT_RETRY)).isEqualTo(60);
    }

    @Test
    void nightlyJobsStartInTheNight() throws SchedulerException {
        Date start = scheduler.getTrigger(TriggerKey.triggerKey(JobConfiguration.GROUP_VALIDITY_CONTROL + "Trigger"))
                .getStartTime();

        assertThat(start.toInstant().atZone(ZoneId.systemDefault()).getHour()).isEqualTo(JobConfiguration.NIGHTLY_HOUR);
        assertThat(start).isAfter(new Date());
    }

    private long repeatIntervalInSeconds(String jobName) throws SchedulerException {
        assertThat(scheduler.checkExists(JobKey.jobKey(jobName))).isTrue();
        SimpleTrigger trigger = (SimpleTrigger) scheduler.getTrigger(TriggerKey.triggerKey(jobName + "Trigger"));
        return trigger.getRepeatInterval() / 1000;
    }
}
