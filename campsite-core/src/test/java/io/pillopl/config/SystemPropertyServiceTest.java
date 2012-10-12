package io.pillopl.config;

import io.pillopl.persistence.dao.SystemPropertyDao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemPropertyServiceTest {

    private final List<SystemProperty> rows = new ArrayList<>();
    private final SystemPropertyDao dao = mock(SystemPropertyDao.class);
    private final SystemPropertyService service = new SystemPropertyService(dao);

    SystemPropertyServiceTest() {
        when(dao.findAll()).thenAnswer(invocation -> List.copyOf(rows));
    }

    private void stored(SystemPropertyKey key, String value) {
        rows.add(new SystemProperty(key, value, "test"));
    }

    @Test
    void missingPropertyFallsBackToTheCodedDefault() {
        stored(SystemPropertyKey.DeveloperEmail, "dev@campsite.local");

        assertThat(service.getStaleSessionTimeoutInHours()).isEqualTo(24);
        assertThat(service.getSuspectRecordKwhPerNightThreshold()).isEqualByComparingTo("300");
        assertThat(service.getErpArticleTaxClass()).isEqualTo("A1");
    }

    @Test
    void storedValueOverridesTheDefault() {
        stored(SystemPropertyKey.StaleSessionTimeoutInHours, "36");
        stored(SystemPropertyKey.SuspectRecordKwhPerNightThreshold, "250.5");
        stored(SystemPropertyKey.ErpArticleTaxClass, "B2");

        assertThat(service.getStaleSessionTimeoutInHours()).isEqualTo(36);
        assertThat(service.getSuspectRecordKwhPerNightThreshold()).isEqualByComparingTo(new BigDecimal("250.5"));
        assertThat(service.getErpArticleTaxClass()).isEqualTo("B2");
    }

    @Test
    void unparsableNumberFallsBackToTheDefault() {
        stored(SystemPropertyKey.LocalCardListDefaultMaxEntries, "a hundred");
        stored(SystemPropertyKey.SuspectRecordKwhPerNightThreshold, "lots");
        stored(SystemPropertyKey.DbAvailabilityAlarmThreshold, "");

        assertThat(service.getLocalCardListDefaultMaxEntries()).isEqualTo(100);
        assertThat(service.getSuspectRecordKwhPerNightThreshold()).isEqualByComparingTo("300");
        assertThat(service.getDbAvailabilityAlarmThreshold()).isEqualTo(20);
    }

    @Test
    void numbersAreReadIgnoringSurroundingWhitespace() {
        stored(SystemPropertyKey.PedestalHeartbeatTimeoutInMinutes, " 5 ");

        assertThat(service.getPedestalHeartbeatTimeoutInMinutes()).isEqualTo(5);
        assertThat(service.getPropertyLong(SystemPropertyKey.PedestalHeartbeatTimeoutInMinutes, 1L)).isEqualTo(5L);
        assertThat(service.getPropertyDouble(SystemPropertyKey.PedestalHeartbeatTimeoutInMinutes, 1.0)).isEqualTo(5.0);
    }

    @Test
    void booleanAcceptsOnlyTrueOrFalseOtherwiseDefault() {
        stored(SystemPropertyKey.MailSenderAddress, "TRUE");
        stored(SystemPropertyKey.DeveloperEmail, "yes");

        assertThat(service.getPropertyBoolean(SystemPropertyKey.MailSenderAddress, false)).isTrue();
        assertThat(service.getPropertyBoolean(SystemPropertyKey.DeveloperEmail, false)).isFalse();
        assertThat(service.getPropertyBoolean(SystemPropertyKey.DeveloperEmail, true)).isTrue();
        assertThat(service.getPropertyBoolean(SystemPropertyKey.ErpArticleTaxClass, true)).isTrue();
    }

    @Test
    void waitTimeAfterDeleteIsConfiguredInSecondsAndReturnedInMillis() {
        stored(SystemPropertyKey.LocalCardListDistributionWaitTimeAfterDeleteInSeconds, "7");

        assertThat(service.getLocalCardListDistributionWaitTimeAfterDeleteInMillis()).isEqualTo(7000L);
    }

    @Test
    void propertiesAreLoadedOnceAndCachedUntilReload() {
        stored(SystemPropertyKey.StaleSessionTimeoutInHours, "36");

        service.getStaleSessionTimeoutInHours();
        service.getCheckoutSessionCloseDelayInMinutes();
        rows.clear();
        stored(SystemPropertyKey.StaleSessionTimeoutInHours, "48");

        assertThat(service.getStaleSessionTimeoutInHours()).isEqualTo(36);
        verify(dao, times(1)).findAll();

        service.reloadSystemProperties();

        assertThat(service.getStaleSessionTimeoutInHours()).isEqualTo(48);
    }

    @Test
    void cachedPropertiesCannotBeModifiedByCallers() {
        stored(SystemPropertyKey.StaleSessionTimeoutInHours, "36");

        assertThat(service.getSystemProperties()).containsEntry(SystemPropertyKey.StaleSessionTimeoutInHours, "36");
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.getSystemProperties().put(SystemPropertyKey.DeveloperEmail, "x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
