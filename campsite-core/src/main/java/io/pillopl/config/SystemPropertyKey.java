package io.pillopl.config;

/**
 * Keys of the configuration kept in table SYSTEM_PROPERTY. The enum name is stored as PROPERTY_KEY.
 * Every key has a getter with its default in {@link SystemPropertyService}.
 */
public enum SystemPropertyKey {

    // general
    DeveloperEmail,
    DbAvailabilityAlarmThreshold,
    MailSenderAddress,

    // pedestal link and monitoring
    PedestalHeartbeatIntervalInSeconds,
    PedestalHeartbeatTimeoutInMinutes,
    PedestalHeartbeatTimeoutJobIntervalInSeconds,
    UnknownStateTimeoutInMinutes,
    UnknownStateEventTriggerJobIntervalInSeconds,
    GroupValidityControlJobIntervalInSeconds,

    // commissioning
    DecommissioningMailExcludedMarkets,

    // supply sessions
    StaleSessionTimeoutInHours,
    CheckoutSessionCloseDelayInMinutes,
    CloseSessionsOnCheckoutJobIntervalInSeconds,

    // QR payment
    QrPayArchiveDelayInHours,
    ArchiveQrPayProcessJobIntervalInSeconds,

    // consumption records
    SuspectRecordKwhPerNightThreshold,
    ReportTopPitchesMaxRows,

    // local card lists
    LocalCardListDefaultMaxEntries,
    LocalCardListDistributionConcurrentThreads,
    LocalCardListDistributionMaximumDurationInMinutes,
    LocalCardListDistributionWaitTimeAfterDeleteInSeconds,
    LocalCardListDistributionJobIntervalInSeconds,

    // materialized views
    UserinfoSummaryRefreshJobIntervalInSeconds,
    SocketUtilizationRefreshJobIntervalInSeconds,

    // ERP
    ErpArticleTaxClass,
    ErpExportJobIntervalInSeconds,

    // audit
    AuditErrorEventsExpiryPeriodInDays,
    AuditSuccessEventsExpiryPeriodInDays,
    AuditCleanupJobIntervalInSeconds
}
