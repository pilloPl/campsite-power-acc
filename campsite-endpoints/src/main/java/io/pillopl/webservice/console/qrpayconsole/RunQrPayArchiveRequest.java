package io.pillopl.webservice.console.qrpayconsole;

import java.io.Serializable;

/**
 * Run the archive of the finished QR payments now (the job runs it as well).
 */
public record RunQrPayArchiveRequest() implements Serializable {
}
