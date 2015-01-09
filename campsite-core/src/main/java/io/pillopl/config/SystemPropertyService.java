package io.pillopl.config;

import io.pillopl.persistence.dao.SystemPropertyDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration kept in table SYSTEM_PROPERTY, cached in memory and loaded lazily on first access.
 * <p>
 * Every key has its own getter with the default coded here: a missing row or a value that cannot be parsed
 * falls back to that default (unparsable values are logged as errors). Callers use the named getters;
 * the generic {@code getProperty*} methods are for the getters and for administration screens.
 */
@Service
public class SystemPropertyService {

    private static final Logger LOG = LoggerFactory.getLogger(SystemPropertyService.class);

    private final Map<SystemPropertyKey, String> systemProperties = new ConcurrentHashMap<>();

    private final SystemPropertyDao systemPropertyDao;

    public SystemPropertyService(SystemPropertyDao systemPropertyDao) {
        this.systemPropertyDao = systemPropertyDao;
    }

    public synchronized void reloadSystemProperties() {
        systemProperties.clear();
        List<SystemProperty> systemPropertyList = systemPropertyDao.findAll();
        for (SystemProperty systemProperty : systemPropertyList) {
            systemProperties.put(systemProperty.getKey(), systemProperty.getValue());
        }
    }

    /**
     * Lazy initialization, don't access the field systemProperties directly.
     *
     * @return read-only map of the keys present in the database and their raw values
     */
    public Map<SystemPropertyKey, String> getSystemProperties() {
        if (systemProperties.isEmpty()) {
            reloadSystemProperties();
        }
        return Collections.unmodifiableMap(systemProperties);
    }

    /**
     * Stores the value (insert or update of the row) and reloads the cache.
     */
    @Transactional
    public void setProperty(SystemPropertyKey key, String value, String description) {
        SystemProperty systemProperty = systemPropertyDao.findByKey(key)
                .orElseGet(() -> new SystemProperty(key, value, description != null ? description : key.name()));
        systemProperty.setValue(value);
        if (description != null) {
            systemProperty.setDescription(description);
        }
        systemPropertyDao.saveAndFlush(systemProperty);
        reloadSystemProperties();
    }

    public String getProperty(SystemPropertyKey key, String defaultValue) {
        String value = getSystemProperties().get(key);
        return (value != null) ? value : defaultValue;
    }

    public int getPropertyInt(SystemPropertyKey key, int defaultValue) {
        String value = getProperty(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            LOG.error("Can't parse int for SystemProperty {}, value={}, using default {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    public long getPropertyLong(SystemPropertyKey key, long defaultValue) {
        String value = getProperty(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            LOG.error("Can't parse long for SystemProperty {}, value={}, using default {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    public double getPropertyDouble(SystemPropertyKey key, double defaultValue) {
        String value = getProperty(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            LOG.error("Can't parse double for SystemProperty {}, value={}, using default {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    public BigDecimal getPropertyBigDecimal(SystemPropertyKey key, BigDecimal defaultValue) {
        String value = getProperty(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            LOG.error("Can't parse decimal for SystemProperty {}, value={}, using default {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    public boolean getPropertyBoolean(SystemPropertyKey key, boolean defaultValue) {
        String value = getProperty(key, null);
        if (value == null) {
            return defaultValue;
        }
        String trimmed = value.trim();
        if ("true".equalsIgnoreCase(trimmed)) {
            return true;
        }
        if ("false".equalsIgnoreCase(trimmed)) {
            return false;
        }
        LOG.error("Can't parse boolean for SystemProperty {}, value={}, using default {}", key, value, defaultValue);
        return defaultValue;
    }

    // ---------------------------------------------------------------- general

    public String getDeveloperEmail() {
        return getProperty(SystemPropertyKey.DeveloperEmail, null);
    }

    public int getDbAvailabilityAlarmThreshold() {
        return getPropertyInt(SystemPropertyKey.DbAvailabilityAlarmThreshold, 20);
    }

    public String getMailSenderAddress() {
        return getProperty(SystemPropertyKey.MailSenderAddress, "noreply@campsite.local");
    }

    // ---------------------------------------------------------------- commissioning

    /**
     * Comma separated markets (e.g. "IT,FR") whose affiliate owners get no mail when a pedestal goes back to the
     * supplier.
     */
    public String getDecommissioningMailExcludedMarkets() {
        return getProperty(SystemPropertyKey.DecommissioningMailExcludedMarkets, "");
    }

    // ---------------------------------------------------------------- pedestal link and monitoring

    public int getPedestalHeartbeatIntervalInSeconds() {
        return getPropertyInt(SystemPropertyKey.PedestalHeartbeatIntervalInSeconds, 600);
    }

    public int getPedestalHeartbeatTimeoutInMinutes() {
        return getPropertyInt(SystemPropertyKey.PedestalHeartbeatTimeoutInMinutes, 15);
    }

    public int getPedestalHeartbeatTimeoutJobIntervalInSeconds() {
        return getPropertyInt(SystemPropertyKey.PedestalHeartbeatTimeoutJobIntervalInSeconds, 60);
    }

    public int getUnknownStateTimeoutInMinutes() {
        return getPropertyInt(SystemPropertyKey.UnknownStateTimeoutInMinutes, 180);
    }

    public int getUnknownStateEventTriggerJobIntervalInSeconds() {
        return getPropertyInt(SystemPropertyKey.UnknownStateEventTriggerJobIntervalInSeconds, 300);
    }

    /**
     * Interval of the control job checking the group trees (nightly by default, the first run in the night).
     */
    public int getGroupValidityControlJobIntervalInSeconds() {
        return getPropertyInt(SystemPropertyKey.GroupValidityControlJobIntervalInSeconds, 86400);
    }

    // ---------------------------------------------------------------- supply sessions

    /**
     * Supply session without a stop and without a check-out of its stay is closed with TIMEOUT after this time.
     */
    public int getStaleSessionTimeoutInHours() {
        return getPropertyInt(SystemPropertyKey.StaleSessionTimeoutInHours, 24);
    }

    public int getCheckoutSessionCloseDelayInMinutes() {
        return getPropertyInt(SystemPropertyKey.CheckoutSessionCloseDelayInMinutes, 30);
    }

    public int getCloseSessionsOnCheckoutJobIntervalInSeconds() {
        return getPropertyInt(SystemPropertyKey.CloseSessionsOnCheckoutJobIntervalInSeconds, 300);
    }

    // ---------------------------------------------------------------- consumption records

    /**
     * Consumption above this many kWh per stay night marks a record as SUSPECT.
     */
    public BigDecimal getSuspectRecordKwhPerNightThreshold() {
        return getPropertyBigDecimal(SystemPropertyKey.SuspectRecordKwhPerNightThreshold, BigDecimal.valueOf(300));
    }

    // ---------------------------------------------------------------- local card lists

    /**
     * Fallback list capacity, used only when the pedestal type does not declare its own capacity.
     */
    public int getLocalCardListDefaultMaxEntries() {
        return getPropertyInt(SystemPropertyKey.LocalCardListDefaultMaxEntries, 100);
    }

    public int getLocalCardListDistributionConcurrentThreads() {
        return getPropertyInt(SystemPropertyKey.LocalCardListDistributionConcurrentThreads, 10);
    }

    public int getLocalCardListDistributionMaximumDurationInMinutes() {
        return getPropertyInt(SystemPropertyKey.LocalCardListDistributionMaximumDurationInMinutes, 120);
    }

    public long getLocalCardListDistributionWaitTimeAfterDeleteInMillis() {
        return 1000L * getPropertyInt(SystemPropertyKey.LocalCardListDistributionWaitTimeAfterDeleteInSeconds, 300);
    }

    public int getLocalCardListDistributionJobIntervalInSeconds() {
        return getPropertyInt(SystemPropertyKey.LocalCardListDistributionJobIntervalInSeconds, 3600);
    }

    // ---------------------------------------------------------------- materialized views

    public int getUserinfoSummaryRefreshJobIntervalInSeconds() {
        return getPropertyInt(SystemPropertyKey.UserinfoSummaryRefreshJobIntervalInSeconds, 120);
    }

    public int getSocketUtilizationRefreshJobIntervalInSeconds() {
        return getPropertyInt(SystemPropertyKey.SocketUtilizationRefreshJobIntervalInSeconds, 3600);
    }

    // ---------------------------------------------------------------- ERP

    public String getErpArticleTaxClass() {
        return getProperty(SystemPropertyKey.ErpArticleTaxClass, "A1");
    }

    public int getErpExportJobIntervalInSeconds() {
        return getPropertyInt(SystemPropertyKey.ErpExportJobIntervalInSeconds, 3600);
    }

    // ---------------------------------------------------------------- audit

    public int getAuditErrorEventsExpiryPeriodInDays() {
        return getPropertyInt(SystemPropertyKey.AuditErrorEventsExpiryPeriodInDays, 180);
    }

    public int getAuditSuccessEventsExpiryPeriodInDays() {
        return getPropertyInt(SystemPropertyKey.AuditSuccessEventsExpiryPeriodInDays, 60);
    }

    public int getAuditCleanupJobIntervalInSeconds() {
        return getPropertyInt(SystemPropertyKey.AuditCleanupJobIntervalInSeconds, 86400);
    }
}
