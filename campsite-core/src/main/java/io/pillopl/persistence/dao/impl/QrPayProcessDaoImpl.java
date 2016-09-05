package io.pillopl.persistence.dao.impl;

import io.pillopl.commons.ListPage;
import io.pillopl.commons.Pagination;
import io.pillopl.dto.qrpay.QrPayProcessOverviewDTO;
import io.pillopl.persistence.dao.QrPayProcessDaoCustom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Archive of the QR payment process chains with the statements of the SQL map; every value is bound.
 */
public class QrPayProcessDaoImpl implements QrPayProcessDaoCustom {

    @Value("#{sqls['findFinishedQrPayProcessRowIds']}")
    private String findFinishedQrPayProcessRowIds;

    @Value("#{sqls['archiveQrPayProcessRows']}")
    private String archiveQrPayProcessRows;

    @Value("#{sqls['deleteQrPayProcessRows']}")
    private String deleteQrPayProcessRows;

    @Value("#{sqls['findQrPayProcessOverviewPage']}")
    private String findQrPayProcessOverviewPage;

    @Value("#{sqls['findQrPayProcessChain']}")
    private String findQrPayProcessChain;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public QrPayProcessDaoImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Long> findFinishedProcessRowIds(LocalDateTime finishedBefore) {
        return jdbcTemplate.queryForList(findFinishedQrPayProcessRowIds,
                new MapSqlParameterSource("finishedBefore", finishedBefore), Long.class);
    }

    @Override
    public int archiveRows(List<Long> rowIds, LocalDateTime archiveDate) {
        if (rowIds.isEmpty()) {
            return 0;
        }
        return jdbcTemplate.update(archiveQrPayProcessRows, new MapSqlParameterSource("ids", rowIds)
                .addValue("archiveDate", archiveDate));
    }

    @Override
    public int deleteRows(List<Long> rowIds) {
        if (rowIds.isEmpty()) {
            return 0;
        }
        return jdbcTemplate.update(deleteQrPayProcessRows, new MapSqlParameterSource("ids", rowIds));
    }

    @Override
    public ListPage<QrPayProcessOverviewDTO> findProcessOverviewPage(Long userSiteGroupId, Long campsiteGroupId,
                                                                     String state, LocalDateTime from,
                                                                     LocalDateTime to, LocalDateTime stuckBefore,
                                                                     Pagination pagination) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("userSiteGroupId", userSiteGroupId)
                .addValue("campsiteGroupId", campsiteGroupId)
                .addValue("state", state)
                .addValue("from", from)
                .addValue("to", to)
                .addValue("stuckBefore", stuckBefore)
                .addValue("limit", pagination.rowsPerPage())
                .addValue("offset", pagination.offset());
        long[] total = {0};
        List<QrPayProcessOverviewDTO> rows = jdbcTemplate.query(findQrPayProcessOverviewPage, parameters,
                (rs, rowNum) -> {
                    total[0] = rs.getLong("TOTAL");
                    return toOverview(rs);
                });
        return ListPage.of(rows, total[0], pagination);
    }

    @Override
    public List<QrPayChainRow> findChain(Long processId) {
        return jdbcTemplate.query(findQrPayProcessChain, new MapSqlParameterSource("processId", processId),
                (rs, rowNum) -> new QrPayChainRow(rs.getLong("ID"), rs.getLong("PROCESS_ID"),
                        rs.getString("ORDER_REFERENCE"), rs.getLong("SOCKET_ID"), rs.getString("STATE"),
                        time(rs, "START_TIME"), time(rs, "STOP_TIME"), (Integer) rs.getObject("TRANSACTION_ID"),
                        (Long) rs.getObject("SUPPLY_SESSION_ID"), rs.getString("ERROR_CODE"),
                        rs.getString("ERROR_MESSAGE"), rs.getBoolean("ARCHIVED")));
    }

    private static QrPayProcessOverviewDTO toOverview(ResultSet rs) throws SQLException {
        return new QrPayProcessOverviewDTO(rs.getLong("PROCESS_ID"), rs.getString("ORDER_REFERENCE"),
                rs.getString("STATE"), rs.getString("SOCKET_CODE"), rs.getString("PEDESTAL_CODE"),
                rs.getLong("CAMPSITE_GROUP_ID"), rs.getString("CAMPSITE_NAME"), rs.getString("CLIENT_USERNAME"),
                rs.getString("APP_USER_ID"), time(rs, "REQUEST_DATE"), time(rs, "START_TIME"),
                (Integer) rs.getObject("TRANSACTION_ID"), rs.getBigDecimal("KWH"), rs.getBigDecimal("COST"),
                rs.getString("CURRENCY"), rs.getString("ERROR_CODE"), rs.getString("ERROR_MESSAGE"),
                rs.getBoolean("ARCHIVED"), rs.getBoolean("STUCK"));
    }

    private static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}
