package io.pillopl.jobs;

import io.pillopl.config.SystemPropertyService;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Quartz jobs of the pedestal backend: a job class, its detail and a trigger with the interval from
 * SYSTEM_PROPERTY (read at start-up).
 */
@Configuration
public class JobConfiguration {

    static final String CLOSE_SESSIONS_ON_CHECKOUT = "closeSessionsOnCheckoutJob";

    @Bean
    public JobDetail closeSessionsOnCheckoutJobDetail() {
        return JobBuilder.newJob(CloseSessionsOnCheckoutJob.class)
                .withIdentity(CLOSE_SESSIONS_ON_CHECKOUT)
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger closeSessionsOnCheckoutJobTrigger(JobDetail closeSessionsOnCheckoutJobDetail,
                                                     SystemPropertyService systemPropertyService) {
        return TriggerBuilder.newTrigger()
                .forJob(closeSessionsOnCheckoutJobDetail)
                .withIdentity(CLOSE_SESSIONS_ON_CHECKOUT + "Trigger")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(systemPropertyService.getCloseSessionsOnCheckoutJobIntervalInSeconds())
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }
}
