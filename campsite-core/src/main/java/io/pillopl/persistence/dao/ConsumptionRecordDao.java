package io.pillopl.persistence.dao;

import io.pillopl.consumption.ConsumptionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Consumption records; the visibility queries of both sides are hand-written SQL in {@link ConsumptionRecordDaoCustom}.
 */
public interface ConsumptionRecordDao extends JpaRepository<ConsumptionRecord, Long>, ConsumptionRecordDaoCustom {

    ConsumptionRecord findBySupplySessionId(Long supplySessionId);

    ConsumptionRecord findByRecordNo(String recordNo);

    /**
     * A record imported from an issuer partner, by the partner's name and record number.
     */
    ConsumptionRecord findBySourceNameAndExternalRecordNo(String sourceName, String externalRecordNo);
}
