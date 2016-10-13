package io.pillopl.webservice.console.erp;

import java.io.Serializable;

/**
 * The month of a billing run (preview or run).
 */
public record ErpBillingMonthRequest(Integer month, Integer year) implements Serializable {
}
