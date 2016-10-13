package io.pillopl.webservice.console.erp;

import java.io.Serializable;

/**
 * Lines of a billing period ("2026_07"), optionally of one export status (NEW, TRANSFERRED, PROCESSED, ERROR).
 */
public record FindErpBillingLinesRequest(String billingPeriod, String exportStatus) implements Serializable {
}
