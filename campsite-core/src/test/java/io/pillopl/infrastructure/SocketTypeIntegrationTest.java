package io.pillopl.infrastructure;

import io.pillopl.persistence.dao.SocketTypeDao;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class SocketTypeIntegrationTest extends IntegrationTest {

    @Autowired
    SocketTypeDao socketTypeDao;

    @Test
    void seededSocketTypesCoverCeeAndSchuko() {
        assertThat(socketTypeDao.findAllActive()).extracting(SocketType::getPlugStandard)
                .containsExactly(PlugStandard.CEE_BLUE_16A, PlugStandard.CEE_RED_32A, PlugStandard.SCHUKO);
    }

    @Test
    void ceeBlueSocketIsProtectedBySixteenAmperes() {
        SocketType ceeBlue = socketTypeDao.findByName("CEE blue 16A");

        assertThat(ceeBlue.getRating()).isEqualTo(BreakerRating.A16);
        assertThat(ceeBlue.getBreakerRating().getMaximumPowerKw()).isEqualByComparingTo(new BigDecimal("3.68"));
        assertThat(ceeBlue.sameAs(PlugStandard.CEE_BLUE_16A, 1, BreakerRating.A16)).isTrue();
        assertThat(ceeBlue.sameAs(PlugStandard.CEE_BLUE_16A, 1, BreakerRating.A10)).isFalse();
    }
}
