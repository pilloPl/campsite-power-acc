package io.pillopl.persistence.dao.impl;

import io.pillopl.persistence.dao.PowerSectionDaoCustom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Section load with the statement of the SQL map; every value is bound.
 */
public class PowerSectionDaoImpl implements PowerSectionDaoCustom {

    @Value("#{sqls['findPowerSectionLoadPerHour']}")
    private String findPowerSectionLoadPerHour;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PowerSectionDaoImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<SectionLoadRow> findSectionLoadPerHour(Long sectionId, LocalDateTime from, LocalDateTime to) {
        return jdbcTemplate.query(findPowerSectionLoadPerHour, new MapSqlParameterSource("sectionId", sectionId)
                        .addValue("from", from).addValue("to", to),
                (rs, rowNum) -> new SectionLoadRow(rs.getTimestamp("HOUR_START").toLocalDateTime(),
                        rs.getInt("ALLOCATED_A"), rs.getBigDecimal("MEASURED_A"), rs.getInt("SESSIONS")));
    }
}
