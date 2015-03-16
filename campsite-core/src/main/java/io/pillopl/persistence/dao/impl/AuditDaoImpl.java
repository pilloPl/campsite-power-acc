package io.pillopl.persistence.dao.impl;

import io.pillopl.persistence.dao.AuditDaoCustom;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AuditDaoImpl implements AuditDaoCustom {

    /**
     * The leading range on ACTION_DATE (the latest of the four expiry dates) lets the planner use
     * IDX_AUDIT_ACTION_DATE instead of scanning the whole table for the OR of the four categories.
     */
    private static final String CLEAN_AUDIT_TABLE = "DELETE FROM AUDIT"
            + " WHERE ACTION_DATE <= :latestExpiryDate AND ("
            + "(CATEGORY = 'TECHNICAL' AND STATUS = 'SUCCESS' AND ACTION_DATE <= :technicalSuccessExpiryDate)"
            + " OR "
            + "(CATEGORY = 'TECHNICAL' AND STATUS <> 'SUCCESS' AND ACTION_DATE <= :technicalErrorExpiryDate)"
            + " OR "
            + "(CATEGORY = 'FUNCTIONAL' AND STATUS = 'SUCCESS' AND ACTION_DATE <= :functionalSuccessExpiryDate)"
            + " OR "
            + "(CATEGORY = 'FUNCTIONAL' AND STATUS <> 'SUCCESS' AND ACTION_DATE <= :functionalErrorExpiryDate))";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AuditDaoImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int cleanAuditTable(int technicalSuccessExpiryInDays, int technicalErrorExpiryInDays,
                               int functionalSuccessExpiryInDays, int functionalErrorExpiryInDays) {
        LocalDate today = LocalDate.now();
        int shortestExpiryInDays = Math.min(Math.min(technicalSuccessExpiryInDays, technicalErrorExpiryInDays),
                Math.min(functionalSuccessExpiryInDays, functionalErrorExpiryInDays));
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("latestExpiryDate", endOfDay(today, shortestExpiryInDays))
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
