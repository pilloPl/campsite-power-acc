package io.pillopl.persistence.dao;

import io.pillopl.consumption.ExportStatus;
import io.pillopl.erp.ErpBillingLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErpBillingLineDao extends JpaRepository<ErpBillingLine, Long> {

    boolean existsByBillingPeriod(String billingPeriod);

    List<ErpBillingLine> findByBillingPeriodOrderByBillingAccountNoAscArticleNoAsc(String billingPeriod);

    List<ErpBillingLine> findByExportStatusOrderByBillingPeriodAscBillingAccountNoAscArticleNoAsc(
            ExportStatus exportStatus);

    List<ErpBillingLine> findByBillingPeriodAndBillingAccountNoOrderByArticleNo(String billingPeriod,
                                                                                String billingAccountNo);
}
