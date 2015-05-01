package io.pillopl.persistence.dao;

import io.pillopl.dto.report.ReportLineDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Hand-written report SQL over CONSUMPTION_RECORD. Every report sees the scope of its reader: the records of the
 * pedestals in the subtree of {@code siteGroupId} OR of the guests in the subtree of {@code issuerGroupId} (a null
 * id makes its side empty). Rejected records are never consumption. All filters are bound parameters.
 */
public interface ConsumptionReportDao {

    /**
     * A record as the in-memory reports need it.
     */
    record ReportRecord(String socketCode, String pedestalCode, LocalDateTime plugIn, LocalDateTime plugOut,
                        BigDecimal kwh) {
    }

    /**
     * @param powerSectionId only the sockets of this section, null for all
     * @return records with plug-out in [from, to], plug-in and kWh known, oldest plug-in first
     */
    List<ReportRecord> findReportRecords(LocalDateTime from, LocalDateTime to, Long siteGroupId,
                                         Long issuerGroupId, Long powerSectionId);

    /**
     * @param year  null for every year
     * @param month 1..12, null for every month
     * @return one line per month of plug-out ("2026_07"): kWh, records, supply hours; oldest month first
     */
    List<ReportLineDTO> getMonthlyConsumptions(Long siteGroupId, Long issuerGroupId, Long powerSectionId,
                                               Integer year, Integer month);

    /**
     * @param year null for every year
     * @return one line per quarter of plug-out ("2026_Q3"): kWh, records, supply hours; oldest quarter first
     */
    List<ReportLineDTO> getQuarterlyConsumptions(Long siteGroupId, Long issuerGroupId, Long powerSectionId,
                                                 Integer year);

    /**
     * @return one line per campsite code: summed kWh, count = summed stay nights (records without nights left out)
     */
    List<ReportLineDTO> getConsumptionPerStayNight(LocalDateTime from, LocalDateTime to, Long siteGroupId,
                                                   Long issuerGroupId, Long powerSectionId);

    /**
     * @return at most {@code maxRows} lines, most kWh first: period = pitch number, series = campsite code,
     * count = records
     */
    List<ReportLineDTO> getTopPitches(LocalDateTime from, LocalDateTime to, Long siteGroupId, Long issuerGroupId,
                                      Long powerSectionId, int maxRows);

    /**
     * @param to exclusive
     * @return rows {pedestal code, kWh sum, record count} of the pedestals having records in [from, to)
     */
    List<Object[]> sumKwhPerPedestal(LocalDateTime from, LocalDateTime to, Collection<String> pedestalCodes,
                                     Long siteGroupId, Long issuerGroupId);
}
