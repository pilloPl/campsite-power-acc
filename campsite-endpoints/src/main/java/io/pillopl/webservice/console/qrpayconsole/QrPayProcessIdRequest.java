package io.pillopl.webservice.console.qrpayconsole;

import java.io.Serializable;

/**
 * A QR payment by its process id (timeline, cancel).
 */
public record QrPayProcessIdRequest(Long processId) implements Serializable {
}
