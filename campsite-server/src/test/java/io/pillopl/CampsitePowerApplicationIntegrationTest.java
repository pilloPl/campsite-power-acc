package io.pillopl;

import io.pillopl.config.SystemPropertyService;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class CampsitePowerApplicationIntegrationTest extends IntegrationTest {

    @Autowired
    SystemPropertyService systemPropertyService;

    @Test
    void applicationStartsOnMigratedDatabaseAndReadsConfiguration() {
        assertThat(systemPropertyService.getSuspectRecordKwhPerNightThreshold()).isEqualByComparingTo("300");
    }
}
