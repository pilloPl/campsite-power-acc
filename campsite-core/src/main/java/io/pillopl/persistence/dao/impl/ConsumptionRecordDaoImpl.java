package io.pillopl.persistence.dao.impl;

import io.pillopl.consumption.ConsumptionRecord;
import io.pillopl.consumption.ExportStatus;
import io.pillopl.consumption.RecordValidationState;
import io.pillopl.persistence.dao.ConsumptionRecordDaoCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * The two-sided visibility of consumption records from the SQL map ({@code WITH RECURSIVE} subtrees of the user's
 * SITE and ISSUER groups, UNION). Every value is bound. The entity variants read the ordered ids first and then the
 * entities, keeping the order.
 */
public class ConsumptionRecordDaoImpl implements ConsumptionRecordDaoCustom {

    @Value("#{sqls['findConsumptionRecordIds']}")
    private String findConsumptionRecordIds;

    @Value("#{sqls['findConsumptionRecordIdsPage']}")
    private String findConsumptionRecordIdsPage;

    @Value("#{sqls['findConsumptionRecordIdsForExport']}")
    private String findConsumptionRecordIdsForExport;

    @Value("#{sqls['nextConsumptionRecordNumber']}")
    private String nextConsumptionRecordNumber;

    @Value("#{sqls['findErpConsumptionPerMonth']}")
    private String findErpConsumptionPerMonth;

    @Value("#{sqls['markRecordsForErpBilling']}")
    private String markRecordsForErpBilling;

    @Value("#{sqls['updateErpBilledRecords']}")
    private String updateErpBilledRecords;

    @PersistenceContext
    private EntityManager entityManager;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ConsumptionRecordDaoImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ConsumptionRecord> findConsumptionRecords(LocalDateTime from, LocalDateTime to, Long siteGroupId,
                                                          Long issuerGroupId) {
        return toRecords(jdbcTemplate.queryForList(findConsumptionRecordIds,
                visibility(from, to, siteGroupId, issuerGroupId), Long.class));
    }

    @Override
    public List<ConsumptionRecord> findConsumptionRecords(LocalDateTime from, LocalDateTime to, Long siteGroupId,
                                                          Long issuerGroupId, RecordValidationState validationState,
                                                          int pageNumber, int pageSize) {
        if (pageNumber < 1 || pageSize < 1) {
            throw new IllegalArgumentException("Page " + pageNumber + " of size " + pageSize + " does not exist");
        }
        MapSqlParameterSource params = visibility(from, to, siteGroupId, issuerGroupId)
                .addValue("validationState", validationState != null ? validationState.name() : null, Types.VARCHAR)
                .addValue("pageSize", pageSize)
                .addValue("offset", (long) (pageNumber - 1) * pageSize);
        return toRecords(jdbcTemplate.queryForList(findConsumptionRecordIdsPage, params, Long.class));
    }

    @Override
    public List<Long> findConsumptionRecordIdsForExport(LocalDateTime from, LocalDateTime to, Long siteGroupId,
                                                        Long issuerGroupId) {
        return jdbcTemplate.queryForList(findConsumptionRecordIdsForExport,
                visibility(from, to, siteGroupId, issuerGroupId), Long.class);
    }

    @Override
    public long nextRecordNumber() {
        Long next = jdbcTemplate.queryForObject(nextConsumptionRecordNumber, new MapSqlParameterSource(), Long.class);
        return Objects.requireNonNull(next, "CONSUMPTION_RECORD_NO_SEQ");
    }

    @Override
    public List<ErpConsumption> findErpConsumptionPerMonth(LocalDateTime from, LocalDateTime to) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", Objects.requireNonNull(from, "from"), Types.TIMESTAMP)
                .addValue("to", Objects.requireNonNull(to, "to"), Types.TIMESTAMP);
        return jdbcTemplate.query(findErpConsumptionPerMonth, params, (rs, rowNum) -> new ErpConsumption(
                rs.getString("BILLING_ACCOUNT_NO"), rs.getObject("TARIFF_ID", Long.class),
                rs.getBoolean("BILLED"), rs.getString("SUPPLY_PERIOD"), rs.getBigDecimal("KWH"),
                rs.getBigDecimal("COST"), rs.getLong("RECORDS")));
    }

    @Override
    public int markRecordsForErpBilling(Collection<String> billingAccountNos, LocalDateTime from, LocalDateTime to,
                                        String period) {
        if (billingAccountNos == null || billingAccountNos.isEmpty()) {
            return 0;
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("billingAccountNos", billingAccountNos)
                .addValue("from", Objects.requireNonNull(from, "from"), Types.TIMESTAMP)
                .addValue("to", Objects.requireNonNull(to, "to"), Types.TIMESTAMP)
                .addValue("period", Objects.requireNonNull(period, "period"));
        return jdbcTemplate.update(markRecordsForErpBilling, params);
    }

    @Override
    public int updateErpBilledRecords(String period, String billingAccountNo, Collection<ExportStatus> fromStatuses,
                                      ExportStatus exportStatus) {
        if (fromStatuses == null || fromStatuses.isEmpty()) {
            return 0;
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("period", Objects.requireNonNull(period, "period"))
                .addValue("billingAccountNo", billingAccountNo, Types.VARCHAR)
                .addValue("fromStatuses", fromStatuses.stream().map(ExportStatus::name).toList())
                .addValue("exportStatus", exportStatus.name());
        return jdbcTemplate.update(updateErpBilledRecords, params);
    }

    private static MapSqlParameterSource visibility(LocalDateTime from, LocalDateTime to, Long siteGroupId,
                                                    Long issuerGroupId) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        return new MapSqlParameterSource()
                .addValue("from", from, Types.TIMESTAMP)
                .addValue("to", to, Types.TIMESTAMP)
                .addValue("siteGroupId", siteGroupId, Types.BIGINT)
                .addValue("issuerGroupId", issuerGroupId, Types.BIGINT);
    }

    private List<ConsumptionRecord> toRecords(List<Long> ids) {
        List<ConsumptionRecord> records = new ArrayList<>(ids.size());
        for (Long id : ids) {
            records.add(entityManager.find(ConsumptionRecord.class, id));
        }
        return records;
    }
}
