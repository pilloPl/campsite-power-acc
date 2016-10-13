package io.pillopl.dto.erp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * A billing run (one per period "2026_07"): when it ran, how many lines of how many accounts for which amount, and
 * the lines per export state (NEW, TRANSFERRED, PROCESSED, ERROR).
 */
public record ErpBillingRunDTO(String billingPeriod, LocalDateTime runDate, int lines, int accounts,
                               BigDecimal amount, Map<String, Integer> linesPerExportStatus) implements Serializable {

    public ErpBillingRunDTO {
        linesPerExportStatus = linesPerExportStatus != null ? Map.copyOf(linesPerExportStatus) : Map.of();
    }
}
