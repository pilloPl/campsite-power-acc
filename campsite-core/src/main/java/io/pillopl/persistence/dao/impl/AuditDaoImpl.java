package io.pillopl.persistence.dao.impl;

import io.pillopl.persistence.dao.AuditDaoCustom;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AuditDaoImpl implements AuditDaoCustom {

    private static final String CLEAN_AUDIT_TABLE = "DELETE FROM AUDIT"
            + " WHERE "
            + "(CATEGORY = 'TECHNICAL' AND STATUS = 'SUCCESS' AND ACTION_DATE <= :technicalSuccessExpiryDate)"
            + " OR "
            + "(CATEGORY = 'TECHNICAL' AND STATUS <> 'SUCCESS' AND ACTION_DATE <= :technicalErrorExpiryDate)"
            + " OR "
            + "(CATEGORY = 'FUNCTIONAL' AND STATUS = 'SUCCESS' AND ACTION_DATE <= :functionalSuccessExpiryDate)"
            + " OR "
            + "(CATEGORY = 'FUNCTIONAL' AND STATUS <> 'SUCCESS' AND ACTION_DATE <= :functionalErrorExpiryDate)";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AuditDaoImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int cleanAuditTable(int technicalSuccessExpiryInDays, int technicalErrorExpiryInDays,
                               int functionalSuccessExpiryInDays, int functionalErrorExpiryInDays) {
        LocalDate today = LocalDate.now();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("technicalSuccessExpiryDate", endOfDay(today, technicalSuccessExpiryInDays))
                .addValue("technicalErrorExpiryDate", endOfDay(today, technicalErrorExpiryInDays))
                .addValue("functionalSuccessExpiryDate", endOfDay(today, functionalSuccessExpiryInDays))
                .addValue("functionalErrorExpiryDate", endOfDay(today, functionalErrorExpiryInDays));
        return jdbcTemplate.update(CLEAN_AUDIT_TABLE, params);
    }

    /**
     * End of the day {@code days} days ago (23:59:59).
     */
    static LocalDateTime endOfDay(LocalDate today, int days) {
        return LocalDateTime.of(today.minusDays(days), LocalTime.of(23, 59, 59));
    }
}
