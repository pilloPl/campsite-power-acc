package io.pillopl.webservice.console.qrpayconsole;

import java.io.Serializable;

/**
 * Number of process rows moved to the archive.
 */
public record RunQrPayArchiveResponse(int archivedRows) implements Serializable {
}
