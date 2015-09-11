package io.pillopl.persistence.dao.impl;

import io.pillopl.persistence.dao.QrPayProcessDaoCustom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

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
}
