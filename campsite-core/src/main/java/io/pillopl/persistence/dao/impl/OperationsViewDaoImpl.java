package io.pillopl.persistence.dao.impl;

import io.pillopl.commons.ListPage;
import io.pillopl.commons.Pagination;
import io.pillopl.dto.map.SectionLoadDTO;
import io.pillopl.dto.supply.SupplyIncidentDTO;
import io.pillopl.dto.supply.SupplySessionDTO;
import io.pillopl.persistence.dao.OperationsViewDao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * SQL of the service tabs from the SQL map on {@link NamedParameterJdbcTemplate}. The page total comes with every
 * row (COUNT(*) OVER ()), an empty page has total 0.
 */
@Repository
public class OperationsViewDaoImpl implements OperationsViewDao {

    @Value("#{sqls['findSupplySessionsPage']}")
    private String findSupplySessionsPage;

    @Value("#{sqls['findSupplyIncidentsPage']}")
    private String findSupplyIncidentsPage;

    @Value("#{sqls['getDashboardCounts']}")
    private String getDashboardCounts;

    @Value("#{sqls['findAlertSources']}")
    private String findAlertSources;

    @Value("#{sqls['getSocketUtilization']}")
    private String getSocketUtilization;

    @Value("#{sqls['findMapSockets']}")
    private String findMapSockets;

    @Value("#{sqls['findSectionLoads']}")
    private String findSectionLoads;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OperationsViewDaoImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ListPage<SupplySessionDTO> findSupplySessions(Long siteGroupId, String pedestalCode, String socketCode,
                                                         boolean activeOnly, LocalDateTime from, LocalDateTime to,
                                                         LocalDateTime silentSince, Pagination pagination) {
        Objects.requireNonNull(silentSince, "silentSince");
        MapSqlParameterSource params = scope(siteGroupId, from, to, pagination)
                .addValue("pedestalCode", blankToNull(pedestalCode), Types.VARCHAR)
                .addValue("socketCode", blankToNull(socketCode), Types.VARCHAR)
                .addValue("activeOnly", activeOnly, Types.BOOLEAN)
                .addValue("silentSince", Timestamp.valueOf(silentSince), Types.TIMESTAMP);
        long[] total = {0};
        List<SupplySessionDTO> sessions = jdbcTemplate.query(findSupplySessionsPage, params, (rs, rowNum) -> {
            total[0] = rs.getLong("TOTAL");
            return toSession(rs);
        });
        return ListPage.of(sessions, total[0], pagination);
    }

    @Override
    public ListPage<SupplyIncidentDTO> findSupplyIncidents(Long siteGroupId, String pedestalCode,
                                                           boolean unreviewedOnly, LocalDateTime from,
                                                           LocalDateTime to, Pagination pagination) {
        MapSqlParameterSource params = scope(siteGroupId, from, to, pagination)
                .addValue("pedestalCode", blankToNull(pedestalCode), Types.VARCHAR)
                .addValue("unreviewedOnly", unreviewedOnly, Types.BOOLEAN);
        long[] total = {0};
        List<SupplyIncidentDTO> incidents = jdbcTemplate.query(findSupplyIncidentsPage, params, (rs, rowNum) -> {
            total[0] = rs.getLong("TOTAL");
            return new SupplyIncidentDTO(rs.getLong("ID"), rs.getString("INCIDENT_TYPE"),
                    rs.getString("PEDESTAL_CODE"), rs.getLong("CAMPSITE_GROUP_ID"), integer(rs, "TRANSACTION_ID"),
                    rs.getString("CARD_RFID"), longOrNull(rs, "SUPPLY_SESSION_ID"), rs.getBigDecimal("METER_KWH"),
                    toLocal(rs.getTimestamp("REMOTE_DATE")), rs.getString("MESSAGE"),
                    toLocal(rs.getTimestamp("CREATE_DATE")), rs.getString("REVIEWED_BY"),
                    toLocal(rs.getTimestamp("REVIEW_DATE")), rs.getString("REVIEW_COMMENT"));
        });
        return ListPage.of(incidents, total[0], pagination);
    }

    @Override
    public DashboardCounts getDashboardCounts(Long siteGroupId, LocalDateTime dayStart, LocalDateTime silentSince,
                                              boolean withEvents, boolean withErp) {
        Objects.requireNonNull(siteGroupId, "siteGroupId");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("siteGroupId", siteGroupId, Types.BIGINT)
                .addValue("dayStart", Timestamp.valueOf(dayStart), Types.TIMESTAMP)
                .addValue("silentSince", Timestamp.valueOf(silentSince), Types.TIMESTAMP)
                .addValue("withEvents", withEvents, Types.BOOLEAN)
                .addValue("withErp", withErp, Types.BOOLEAN);
        return jdbcTemplate.queryForObject(getDashboardCounts, params, (rs, rowNum) -> new DashboardCounts(
                rs.getLong("ACTIVE_SESSIONS"), rs.getBigDecimal("KWH_TODAY"), rs.getLong("FAULTED_SOCKETS"),
                rs.getLong("BREAKER_TRIPPED_SOCKETS"), rs.getLong("TIMED_OUT_PEDESTALS"), rs.getLong("REVIEW_QUEUE"),
                rs.getLong("STALE_SESSIONS"), longOrNull(rs, "FAILED_EVENTS"), longOrNull(rs, "ERP_LINES_IN_ERROR")));
    }

    @Override
    public List<MapSocketRow> findMapSockets(Long siteGroupId) {
        Objects.requireNonNull(siteGroupId, "siteGroupId");
        MapSqlParameterSource params = new MapSqlParameterSource("siteGroupId", siteGroupId);
        return jdbcTemplate.query(findMapSockets, params, (rs, rowNum) -> new MapSocketRow(rs.getLong("GROUP_ID"),
                rs.getString("GROUP_NAME"), rs.getString("GROUP_DISPLAY_NAME"), rs.getLong("PEDESTAL_ID"),
                rs.getString("PEDESTAL_CODE"), rs.getBigDecimal("LATITUDE"), rs.getBigDecimal("LONGITUDE"),
                rs.getString("PEDESTAL_TYPE"), rs.getString("LINK_VERSION"), rs.getBoolean("REMOTE_CONTROL"),
                rs.getBoolean("TIMEOUT"), toLocal(rs.getTimestamp("APPROX_LAST_INCOMING")),
                longOrNull(rs, "SOCKET_ID"), integer(rs, "SOCKET_NO"), rs.getString("SOCKET_NAME"),
                rs.getString("SOCKET_CODE"), rs.getString("PITCH_NO"), rs.getString("STATUS"),
                longOrNull(rs, "POWER_SECTION_ID"), longOrNull(rs, "SESSION_ID"), integer(rs, "MAX_CURRENT_A")));
    }

    /**
     * The load share is the allowed current of the active sessions divided by the capacity (3 decimals).
     */
    @Override
    public List<SectionLoadDTO> findSectionLoads(Long siteGroupId) {
        Objects.requireNonNull(siteGroupId, "siteGroupId");
        MapSqlParameterSource params = new MapSqlParameterSource("siteGroupId", siteGroupId);
        return jdbcTemplate.query(findSectionLoads, params, (rs, rowNum) -> {
            BigDecimal capacity = rs.getBigDecimal("MAX_CURRENT_A");
            BigDecimal allocated = rs.getBigDecimal("ALLOCATED_CURRENT_A");
            BigDecimal share = capacity != null && capacity.signum() > 0
                    ? allocated.divide(capacity, 3, java.math.RoundingMode.HALF_UP) : null;
            return new SectionLoadDTO(rs.getLong("ID"), rs.getString("SECTION_NAME"), capacity,
                    rs.getString("ALGORITHM_BEAN_NAME"), rs.getLong("SOCKETS"), rs.getLong("ACTIVE_SESSIONS"),
                    allocated, share);
        });
    }

    @Override
    public List<UtilizationCell> getSocketUtilization(Long siteGroupId, LocalDateTime from, LocalDateTime to) {
        Objects.requireNonNull(siteGroupId, "siteGroupId");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("siteGroupId", siteGroupId, Types.BIGINT)
                .addValue("from", Timestamp.valueOf(from), Types.TIMESTAMP)
                .addValue("to", Timestamp.valueOf(to), Types.TIMESTAMP);
        return jdbcTemplate.query(getSocketUtilization, params, (rs, rowNum) -> new UtilizationCell(
                rs.getInt("DAY_OF_WEEK"), rs.getInt("HOUR"), rs.getLong("MONITORED_SECONDS"),
                rs.getLong("SUPPLYING_SECONDS"), rs.getLong("SOCKETS")));
    }

    @Override
    public List<AlertSource> findAlertSources(Long siteGroupId, LocalDateTime silentSince, boolean withEvents,
                                              boolean withErp, int maxRows) {
        Objects.requireNonNull(siteGroupId, "siteGroupId");
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("siteGroupId", siteGroupId, Types.BIGINT)
                .addValue("silentSince", Timestamp.valueOf(silentSince), Types.TIMESTAMP)
                .addValue("withEvents", withEvents, Types.BOOLEAN)
                .addValue("withErp", withErp, Types.BOOLEAN)
                .addValue("maxRows", maxRows, Types.INTEGER);
        return jdbcTemplate.query(findAlertSources, params, (rs, rowNum) -> new AlertSource(
                rs.getString("ALERT_TYPE"), rs.getLong("OBJECT_ID"), toLocal(rs.getTimestamp("ALERT_TIME")),
                rs.getString("PEDESTAL_CODE"), longOrNull(rs, "SOCKET_ID"), rs.getString("SOCKET_NAME"),
                longOrNull(rs, "SESSION_ID"), longOrNull(rs, "RECORD_ID"), rs.getString("DETAIL")));
    }

    /**
     * An active session shows the socket's last reading as its end and the consumption so far.
     */
    private static SupplySessionDTO toSession(ResultSet rs) throws SQLException {
        boolean active = rs.getBoolean("ACTIVE");
        BigDecimal startMeter = rs.getBigDecimal("START_METER_KWH");
        BigDecimal stopMeter = active ? rs.getBigDecimal("SOCKET_METER_KWH") : rs.getBigDecimal("STOP_METER_KWH");
        BigDecimal kwh = active
                ? (startMeter != null && stopMeter != null ? stopMeter.subtract(startMeter).max(BigDecimal.ZERO) : null)
                : rs.getBigDecimal("KWH");
        LocalDateTime created = toLocal(rs.getTimestamp("CREATE_DATE"));
        LocalDateTime remote = toLocal(rs.getTimestamp("REMOTE_DATE"));
        String guestName = rs.getString("GUEST_NAME");
        return new SupplySessionDTO(rs.getLong("ID"), integer(rs, "TRANSACTION_ID"), active, rs.getBoolean("STALE"),
                rs.getLong("SOCKET_ID"), rs.getString("SOCKET_CODE"), rs.getString("SOCKET_NAME"),
                rs.getString("PITCH_NO"), rs.getString("PEDESTAL_CODE"), rs.getLong("CAMPSITE_GROUP_ID"),
                rs.getString("CAMPSITE_NAME"), rs.getString("CARD_NUMBER"), rs.getString("CARD_RFID"),
                rs.getString("HOUSE_CARD_TYPE"), longOrNull(rs, "QRPAY_PROCESS_ID"), longOrNull(rs, "STAY_ID"),
                guestName != null && !guestName.isBlank() ? guestName : null, rs.getString("UNIT_PLATE"),
                rs.getInt("MAX_CURRENT_A"), remote != null ? remote : created, toLocal(rs.getTimestamp("STOP_DATE")),
                rs.getString("STOP_REASON"), startMeter, stopMeter, kwh, longOrNull(rs, "RECORD_ID"),
                toLocal(rs.getTimestamp("SOCKET_MOD_DATE")));
    }

    static MapSqlParameterSource scope(Long siteGroupId, LocalDateTime from, LocalDateTime to,
                                       Pagination pagination) {
        Objects.requireNonNull(siteGroupId, "siteGroupId");
        Pagination page = pagination != null ? pagination : Pagination.FIRST_PAGE;
        return new MapSqlParameterSource()
                .addValue("siteGroupId", siteGroupId, Types.BIGINT)
                .addValue("from", from != null ? Timestamp.valueOf(from) : null, Types.TIMESTAMP)
                .addValue("to", to != null ? Timestamp.valueOf(to) : null, Types.TIMESTAMP)
                .addValue("limit", page.rowsPerPage(), Types.INTEGER)
                .addValue("offset", page.offset(), Types.BIGINT);
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static LocalDateTime toLocal(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    static Long longOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    static Integer integer(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
