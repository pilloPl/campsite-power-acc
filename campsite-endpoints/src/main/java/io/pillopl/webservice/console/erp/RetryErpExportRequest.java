package io.pillopl.webservice.console.erp;

import java.io.Serializable;

/**
 * Send the refused lines of a billing period ("2026_07") again.
 */
public record RetryErpExportRequest(String billingPeriod) implements Serializable {
}
