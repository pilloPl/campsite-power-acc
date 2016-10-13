package io.pillopl.webservice.console.erp;

import java.io.Serializable;

/**
 * Number of lines that go to the ERP again with the next file.
 */
public record RetryErpExportResponse(int resentLines) implements Serializable {
}
