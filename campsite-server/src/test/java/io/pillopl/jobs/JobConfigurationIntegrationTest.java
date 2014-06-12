package io.pillopl.jobs;

import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;

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
}
