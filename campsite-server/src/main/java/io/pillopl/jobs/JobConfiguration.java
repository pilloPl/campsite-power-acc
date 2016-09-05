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

import java.util.Map;

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

    static final String ERP_ACCOUNT_EXPORT = "erpAccountExportJob";

    static final String ERP_ACCOUNT_IMPORT = "erpAccountImportJob";

    static final String ERP_BILLING_EXPORT = "erpBillingExportJob";

    static final String ISSUER_EXCHANGE = "issuerExchangeJob";

    /**
     * Hour of the night of the first run of the nightly jobs.
     */
    static final int NIGHTLY_HOUR = 3;

    /**
     * What each job does, shown in the jobs list of the administration tab.
     */
    static final Map<String, String> DESCRIPTIONS = Map.ofEntries(
            Map.entry(CLOSE_SESSIONS_ON_CHECKOUT, "Closes the supply sessions of checked-out stays and silent sessions"),
            Map.entry(PEDESTAL_HEARTBEAT_TIMEOUT, "Marks pedestals without heartbeat as timed out"),
            Map.entry(UNKNOWN_STATE_EVENT_TRIGGER, "Sets sockets without status notification to UNKNOWN"),
            Map.entry(EVENT_RETRY, "Sends the events the queue refused again"),
            Map.entry(GROUP_VALIDITY_CONTROL, "Checks the group trees and the records no export takes"),
            Map.entry(USERINFO_SUMMARY_REFRESH, "Refreshes the self-service user view"),
            Map.entry(SOCKET_UTILIZATION_REFRESH, "Refreshes the socket utilization view of the reports"),
            Map.entry(AUDIT_CLEANUP, "Deletes expired audit rows"),
            Map.entry(LOCAL_CARD_LIST_DISTRIBUTION, "Sends the local card lists to the pedestals"),
            Map.entry(ARCHIVE_QRPAY_PROCESS, "Moves finished QR payments into the archive"),
            Map.entry(ERP_ACCOUNT_EXPORT, "Hands orders waiting for an account to the ERP"),
            Map.entry(ERP_ACCOUNT_IMPORT, "Takes the partner and account numbers from the ERP"),
            Map.entry(ERP_BILLING_EXPORT, "Bills the month and exports the billing lines to the ERP"),
            Map.entry(ISSUER_EXCHANGE, "Exchanges cards and records with the issuer partners"));

    @Bean
    public JobDetail closeSessionsOnCheckoutJobDetail() {
        return JobBuilder.newJob(CloseSessionsOnCheckoutJob.class)
                .withIdentity(CLOSE_SESSIONS_ON_CHECKOUT)
                .withDescription(DESCRIPTIONS.get(CLOSE_SESSIONS_ON_CHECKOUT))
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

    // ---------------------------------------------------------------- issuer exchange

    @Bean
    public JobDetail issuerExchangeJobDetail() {
        return jobDetail(IssuerExchangeJob.class, ISSUER_EXCHANGE);
    }

    @Bean
    public Trigger issuerExchangeJobTrigger(JobDetail issuerExchangeJobDetail,
                                            SystemPropertyService systemPropertyService) {
        return trigger(issuerExchangeJobDetail, systemPropertyService.getIssuerExchangeJobIntervalInSeconds())
                .build();
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

    // ---------------------------------------------------------------- ERP

    @Bean
    public JobDetail erpAccountExportJobDetail() {
        return jobDetail(ErpAccountExportJob.class, ERP_ACCOUNT_EXPORT);
    }

    @Bean
    public Trigger erpAccountExportJobTrigger(JobDetail erpAccountExportJobDetail,
                                              SystemPropertyService systemPropertyService) {
        return trigger(erpAccountExportJobDetail,
                systemPropertyService.getErpAccountExportJobIntervalInSeconds()).build();
    }

    @Bean
    public JobDetail erpAccountImportJobDetail() {
        return jobDetail(ErpAccountImportJob.class, ERP_ACCOUNT_IMPORT);
    }

    @Bean
    public Trigger erpAccountImportJobTrigger(JobDetail erpAccountImportJobDetail,
                                              SystemPropertyService systemPropertyService) {
        return trigger(erpAccountImportJobDetail,
                systemPropertyService.getErpAccountImportJobIntervalInSeconds()).build();
    }

    @Bean
    public JobDetail erpBillingExportJobDetail() {
        return jobDetail(ErpBillingExportJob.class, ERP_BILLING_EXPORT);
    }

    @Bean
    public Trigger erpBillingExportJobTrigger(JobDetail erpBillingExportJobDetail,
                                              SystemPropertyService systemPropertyService) {
        return trigger(erpBillingExportJobDetail, systemPropertyService.getErpExportJobIntervalInSeconds()).build();
    }

    private static JobDetail jobDetail(Class<? extends Job> jobClass, String name) {
        return JobBuilder.newJob(jobClass)
                .withIdentity(name)
                .withDescription(DESCRIPTIONS.get(name))
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
