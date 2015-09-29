package io.pillopl.jobs;

import io.pillopl.config.SystemPropertyService;
import org.quartz.DateBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
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

    static final String PEDESTAL_HEARTBEAT_TIMEOUT = "pedestalHeartbeatTimeoutJob";

    static final String UNKNOWN_STATE_EVENT_TRIGGER = "unknownStateEventTriggerJob";

    static final String EVENT_RETRY = "eventRetryJob";

    static final String GROUP_VALIDITY_CONTROL = "groupValidityControlJob";

    static final String USERINFO_SUMMARY_REFRESH = "userinfoSummaryRefreshJob";

    static final String SOCKET_UTILIZATION_REFRESH = "socketUtilizationRefreshJob";

    static final String AUDIT_CLEANUP = "auditCleanupJob";

    static final String LOCAL_CARD_LIST_DISTRIBUTION = "localCardListDistributionJob";

    static final String ARCHIVE_QRPAY_PROCESS = "archiveQrPayProcessJob";

    /**
     * Hour of the night of the first run of the nightly jobs.
     */
    static final int NIGHTLY_HOUR = 3;

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

    // ---------------------------------------------------------------- monitoring

    @Bean
    public JobDetail pedestalHeartbeatTimeoutJobDetail() {
        return jobDetail(PedestalHeartbeatTimeoutJob.class, PEDESTAL_HEARTBEAT_TIMEOUT);
    }

    @Bean
    public Trigger pedestalHeartbeatTimeoutJobTrigger(JobDetail pedestalHeartbeatTimeoutJobDetail,
                                                      SystemPropertyService systemPropertyService) {
        return trigger(pedestalHeartbeatTimeoutJobDetail,
                systemPropertyService.getPedestalHeartbeatTimeoutJobIntervalInSeconds()).build();
    }

    @Bean
    public JobDetail unknownStateEventTriggerJobDetail() {
        return jobDetail(UnknownStateEventTriggerJob.class, UNKNOWN_STATE_EVENT_TRIGGER);
    }

    @Bean
    public Trigger unknownStateEventTriggerJobTrigger(JobDetail unknownStateEventTriggerJobDetail,
                                                      SystemPropertyService systemPropertyService) {
        return trigger(unknownStateEventTriggerJobDetail,
                systemPropertyService.getUnknownStateEventTriggerJobIntervalInSeconds()).build();
    }

    @Bean
    public JobDetail eventRetryJobDetail() {
        return jobDetail(EventRetryJob.class, EVENT_RETRY);
    }

    @Bean
    public Trigger eventRetryJobTrigger(JobDetail eventRetryJobDetail, SystemPropertyService systemPropertyService) {
        return trigger(eventRetryJobDetail, systemPropertyService.getEventRetryJobIntervalInSeconds()).build();
    }

    @Bean
    public JobDetail socketUtilizationRefreshJobDetail() {
        return jobDetail(SocketUtilizationRefreshJob.class, SOCKET_UTILIZATION_REFRESH);
    }

    @Bean
    public Trigger socketUtilizationRefreshJobTrigger(JobDetail socketUtilizationRefreshJobDetail,
                                                      SystemPropertyService systemPropertyService) {
        return trigger(socketUtilizationRefreshJobDetail,
                systemPropertyService.getSocketUtilizationRefreshJobIntervalInSeconds()).build();
    }

    // ---------------------------------------------------------------- groups, users, audit

    @Bean
    public JobDetail groupValidityControlJobDetail() {
        return jobDetail(GroupValidityControlJob.class, GROUP_VALIDITY_CONTROL);
    }

    @Bean
    public Trigger groupValidityControlJobTrigger(JobDetail groupValidityControlJobDetail,
                                                  SystemPropertyService systemPropertyService) {
        return trigger(groupValidityControlJobDetail,
                systemPropertyService.getGroupValidityControlJobIntervalInSeconds())
                .startAt(DateBuilder.tomorrowAt(NIGHTLY_HOUR, 0, 0))
                .build();
    }

    @Bean
    public JobDetail userinfoSummaryRefreshJobDetail() {
        return jobDetail(UserinfoSummaryRefreshJob.class, USERINFO_SUMMARY_REFRESH);
    }

    @Bean
    public Trigger userinfoSummaryRefreshJobTrigger(JobDetail userinfoSummaryRefreshJobDetail,
                                                    SystemPropertyService systemPropertyService) {
        return trigger(userinfoSummaryRefreshJobDetail,
                systemPropertyService.getUserinfoSummaryRefreshJobIntervalInSeconds()).build();
    }

    @Bean
    public JobDetail auditCleanupJobDetail() {
        return jobDetail(AuditCleanupJob.class, AUDIT_CLEANUP);
    }

    @Bean
    public Trigger auditCleanupJobTrigger(JobDetail auditCleanupJobDetail,
                                          SystemPropertyService systemPropertyService) {
        return trigger(auditCleanupJobDetail, systemPropertyService.getAuditCleanupJobIntervalInSeconds())
                .startAt(DateBuilder.tomorrowAt(NIGHTLY_HOUR, 30, 0))
                .build();
    }

    // ---------------------------------------------------------------- cards

    @Bean
    public JobDetail localCardListDistributionJobDetail() {
        return jobDetail(LocalCardListDistributionJob.class, LOCAL_CARD_LIST_DISTRIBUTION);
    }

    @Bean
    public Trigger localCardListDistributionJobTrigger(JobDetail localCardListDistributionJobDetail,
                                                       SystemPropertyService systemPropertyService) {
        return trigger(localCardListDistributionJobDetail,
                systemPropertyService.getLocalCardListDistributionJobIntervalInSeconds()).build();
    }

    // ---------------------------------------------------------------- QR payment

    @Bean
    public JobDetail archiveQrPayProcessJobDetail() {
        return jobDetail(ArchiveQrPayProcessJob.class, ARCHIVE_QRPAY_PROCESS);
    }

    @Bean
    public Trigger archiveQrPayProcessJobTrigger(JobDetail archiveQrPayProcessJobDetail,
                                                 SystemPropertyService systemPropertyService) {
        return trigger(archiveQrPayProcessJobDetail,
                systemPropertyService.getArchiveQrPayProcessJobIntervalInSeconds()).build();
    }

    private static JobDetail jobDetail(Class<? extends Job> jobClass, String name) {
        return JobBuilder.newJob(jobClass)
                .withIdentity(name)
                .storeDurably()
                .build();
    }

    private static TriggerBuilder<SimpleTrigger> trigger(JobDetail jobDetail, int intervalInSeconds) {
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName() + "Trigger")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(intervalInSeconds)
                        .repeatForever()
                        .withMisfireHandlingInstructionNextWithRemainingCount());
    }
}
