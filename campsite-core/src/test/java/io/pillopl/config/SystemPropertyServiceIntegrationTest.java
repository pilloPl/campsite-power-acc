package io.pillopl.config;

import io.pillopl.persistence.dao.SystemPropertyDao;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class SystemPropertyServiceIntegrationTest extends IntegrationTest {

    @Autowired
    SystemPropertyService systemPropertyService;

    @Autowired
    SystemPropertyDao systemPropertyDao;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void removeRowsAddedByTest() {
        jdbcTemplate.update("DELETE FROM SYSTEM_PROPERTY WHERE ID >= 1000");
        jdbcTemplate.update("UPDATE SYSTEM_PROPERTY SET PROPERTY_VALUE = '24' WHERE PROPERTY_KEY = 'StaleSessionTimeoutInHours'");
        systemPropertyService.reloadSystemProperties();
    }

    @Test
    void seededValuesAreReadFromTheDatabase() {
        systemPropertyService.reloadSystemProperties();

        assertThat(systemPropertyService.getSuspectRecordKwhPerNightThreshold()).isEqualByComparingTo("300");
        assertThat(systemPropertyService.getPedestalHeartbeatTimeoutInMinutes()).isEqualTo(15);
    }

    @Test
    void settingANewPropertyInsertsARowAndRefreshesTheCache() {
        systemPropertyService.setProperty(SystemPropertyKey.LocalCardListDefaultMaxEntries, "250", "Fallback capacity");

        assertThat(systemPropertyService.getLocalCardListDefaultMaxEntries()).isEqualTo(250);
        assertThat(systemPropertyDao.findByKey(SystemPropertyKey.LocalCardListDefaultMaxEntries))
                .hasValueSatisfying(row -> {
                    assertThat(row.getValue()).isEqualTo("250");
                    assertThat(row.getDescription()).isEqualTo("Fallback capacity");
                    assertThat(row.getId()).isGreaterThanOrEqualTo(1000L);
                });
    }

    @Test
    void settingAnExistingPropertyUpdatesItsRowAndKeepsTheDescription() {
        systemPropertyService.setProperty(SystemPropertyKey.StaleSessionTimeoutInHours, "12", null);

        assertThat(systemPropertyService.getStaleSessionTimeoutInHours()).isEqualTo(12);
        assertThat(systemPropertyDao.findByKey(SystemPropertyKey.StaleSessionTimeoutInHours))
                .hasValueSatisfying(row -> assertThat(row.getDescription()).startsWith("Supply session without a stop"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM SYSTEM_PROPERTY WHERE PROPERTY_KEY = 'StaleSessionTimeoutInHours'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void newPropertyWithoutDescriptionUsesTheKeyName() {
        systemPropertyService.setProperty(SystemPropertyKey.ErpExportJobIntervalInSeconds, "600", null);

        assertThat(systemPropertyDao.findByKey(SystemPropertyKey.ErpExportJobIntervalInSeconds))
                .hasValueSatisfying(row -> assertThat(row.getDescription()).isEqualTo("ErpExportJobIntervalInSeconds"));
    }
}
