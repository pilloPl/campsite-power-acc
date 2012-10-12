package io.pillopl.migration;

import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationIntegrationTest extends IntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void allMigrationsAreAppliedSuccessfullyOnStartup() {
        Integer failed = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = false", Integer.class);
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = ? AND success = true", Integer.class, "1");

        assertThat(failed).isZero();
        assertThat(applied).isEqualTo(1);
    }

    @Test
    void systemPropertyTableIsSeededWithTheSuspectThreshold() {
        String value = jdbcTemplate.queryForObject(
                "SELECT PROPERTY_VALUE FROM SYSTEM_PROPERTY WHERE PROPERTY_KEY = ?", String.class,
                "SuspectRecordKwhPerNightThreshold");

        assertThat(value).isEqualTo("300");
    }

    @Test
    void systemPropertyKeyIsUnique() {
        Integer uniqueConstraints = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM information_schema.table_constraints
                 WHERE table_name = 'system_property' AND constraint_type = 'UNIQUE'
                   AND constraint_name = 'unique_system_property_key'""", Integer.class);

        assertThat(uniqueConstraints).isEqualTo(1);
    }
}
