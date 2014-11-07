package io.pillopl.persistence.dao.impl;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AuditDaoImplTest {

    @Test
    void expiryDateIsTheLastSecondOfTheDayThatManyDaysAgo() {
        LocalDate today = LocalDate.of(2016, 8, 31);

        assertThat(AuditDaoImpl.endOfDay(today, 30)).isEqualTo(LocalDateTime.of(2016, 8, 1, 23, 59, 59));
        assertThat(AuditDaoImpl.endOfDay(today, 0)).isEqualTo(LocalDateTime.of(2016, 8, 31, 23, 59, 59));
    }
}
