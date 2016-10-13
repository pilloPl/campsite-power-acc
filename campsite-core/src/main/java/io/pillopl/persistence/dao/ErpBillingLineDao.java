package io.pillopl.persistence.dao;

import io.pillopl.consumption.ExportStatus;
import io.pillopl.erp.ErpBillingLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ErpBillingLineDao extends JpaRepository<ErpBillingLine, Long> {

    boolean existsByBillingPeriod(String billingPeriod);

    List<ErpBillingLine> findByBillingPeriodOrderByBillingAccountNoAscArticleNoAsc(String billingPeriod);

    List<ErpBillingLine> findByExportStatusOrderByBillingPeriodAscBillingAccountNoAscArticleNoAsc(
            ExportStatus exportStatus);

    List<ErpBillingLine> findByBillingPeriodAndBillingAccountNoOrderByArticleNo(String billingPeriod,
                                                                                String billingAccountNo);

    /**
     * Per billing period (newest first) and export status: lines, amount and the first creation of the run.
     */
    @Query("select l.billingPeriod, l.exportStatus, count(l), sum(l.amount), min(l.createDate) "
            + "from ErpBillingLine l group by l.billingPeriod, l.exportStatus "
            + "order by l.billingPeriod desc, l.exportStatus")
    List<Object[]> findRunSummaries();

    /**
     * Billed accounts per billing period.
     */
    @Query("select l.billingPeriod, count(distinct l.billingAccountNo) from ErpBillingLine l group by l.billingPeriod")
    List<Object[]> findRunAccountCounts();
}
