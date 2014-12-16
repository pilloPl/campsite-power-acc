package io.pillopl.persistence.dao.impl;

import io.pillopl.dto.report.ReportLineDTO;
import io.pillopl.persistence.dao.ConsumptionReportDao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Report SQL from the SQL map on {@link NamedParameterJdbcTemplate}. Dates, groups, section, year and month are bound
 * parameters; no value is ever put into the SQL text.
 */
@Repository
public class ConsumptionReportDaoImpl implements ConsumptionReportDao {

    private static final int HOURS_SCALE = 2;

    @Value("#{sqls['findConsumptionReportRecords']}")
    private String findConsumptionReportRecords;

    @Value("#{sqls['getMonthlyConsumptions']}")
    private String getMonthlyConsumptions;

    @Value("#{sqls['getQuarterlyConsumptions']}")
    private String getQuarterlyConsumptions;

    @Value("#{sqls['getConsumptionPerStayNight']}")
    private String getConsumptionPerStayNight;

    @Value("#{sqls['sumKwhPerPedestal']}")
    private String sumKwhPerPedestal;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ConsumptionReportDaoImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ReportRecord> findReportRecords(LocalDateTime from, LocalDateTime to, Long siteGroupId,
                                                Long issuerGroupId, Long powerSectionId) {
        MapSqlParameterSource params = period(from, to)
                .addValue("siteGroupId", siteGroupId, Types.BIGINT)
                .addValue("issuerGroupId", issuerGroupId, Types.BIGINT)
                .addValue("powerSectionId", powerSectionId, Types.BIGINT);
        return jdbcTemplate.query(findConsumptionReportRecords, params, (rs, rowNum) -> new ReportRecord(
                rs.getString("SOCKET_CODE"), rs.getString("PEDESTAL_CODE"), toLocal(rs.getTimestamp("PLUG_IN")),
                toLocal(rs.getTimestamp("PLUG_OUT")), rs.getBigDecimal("KWH")));
    }

    @Override
    public List<ReportLineDTO> getMonthlyConsumptions(Long siteGroupId, Long issuerGroupId, Long powerSectionId,
                                                      Integer year, Integer month) {
        MapSqlParameterSource params = scope(siteGroupId, issuerGroupId, powerSectionId)
                .addValue("year", year, Types.INTEGER)
                .addValue("month", month, Types.INTEGER);
        return jdbcTemplate.query(getMonthlyConsumptions, params, CONSUMPTION_PER_TIME);
    }

    @Override
    public List<ReportLineDTO> getQuarterlyConsumptions(Long siteGroupId, Long issuerGroupId, Long powerSectionId,
                                                        Integer year) {
        MapSqlParameterSource params = scope(siteGroupId, issuerGroupId, powerSectionId)
                .addValue("year", year, Types.INTEGER);
        return jdbcTemplate.query(getQuarterlyConsumptions, params, CONSUMPTION_PER_TIME);
    }

    @Override
    public List<ReportLineDTO> getConsumptionPerStayNight(LocalDateTime from, LocalDateTime to, Long siteGroupId,
                                                          Long issuerGroupId, Long powerSectionId) {
        MapSqlParameterSource params = period(from, to)
                .addValue("siteGroupId", siteGroupId, Types.BIGINT)
                .addValue("issuerGroupId", issuerGroupId, Types.BIGINT)
                .addValue("powerSectionId", powerSectionId, Types.BIGINT);
        return jdbcTemplate.query(getConsumptionPerStayNight, params, (rs, rowNum) -> new ReportLineDTO(
                rs.getString("CAMPSITE_CODE"), null, rs.getBigDecimal("KWH"), rs.getLong("NIGHTS"), null));
    }

    @Override
    public List<Object[]> sumKwhPerPedestal(LocalDateTime from, LocalDateTime to, Collection<String> pedestalCodes,
                                            Long siteGroupId, Long issuerGroupId) {
        if (pedestalCodes == null || pedestalCodes.isEmpty()) {
            return new ArrayList<>();
        }
        MapSqlParameterSource params = period(from, to)
                .addValue("pedestalCodes", pedestalCodes)
                .addValue("siteGroupId", siteGroupId, Types.BIGINT)
                .addValue("issuerGroupId", issuerGroupId, Types.BIGINT);
        return jdbcTemplate.query(sumKwhPerPedestal, params, (rs, rowNum) -> new Object[]{
                rs.getString("PEDESTAL_CODE"), rs.getBigDecimal("KWH"), rs.getLong("RECORDS")});
    }

    private static final RowMapper<ReportLineDTO> CONSUMPTION_PER_TIME = (rs, rowNum) -> {
        BigDecimal hours = rs.getBigDecimal("HOURS");
        return new ReportLineDTO(rs.getString("TIME_PERIOD"), null, rs.getBigDecimal("KWH"), rs.getLong("RECORDS"),
                hours != null ? hours.setScale(HOURS_SCALE, RoundingMode.HALF_UP) : null);
    };

    private static MapSqlParameterSource period(LocalDateTime from, LocalDateTime to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        return new MapSqlParameterSource()
                .addValue("from", from, Types.TIMESTAMP)
                .addValue("to", to, Types.TIMESTAMP);
    }

    private static MapSqlParameterSource scope(Long siteGroupId, Long issuerGroupId, Long powerSectionId) {
        return new MapSqlParameterSource()
                .addValue("siteGroupId", siteGroupId, Types.BIGINT)
                .addValue("issuerGroupId", issuerGroupId, Types.BIGINT)
                .addValue("powerSectionId", powerSectionId, Types.BIGINT);
    }

    private static LocalDateTime toLocal(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}
