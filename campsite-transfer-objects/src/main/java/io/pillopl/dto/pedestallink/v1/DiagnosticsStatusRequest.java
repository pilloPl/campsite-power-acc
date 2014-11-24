package io.pillopl.dto.pedestallink.v1;

import java.io.Serializable;

/**
 * Link v1: result of a diagnostics upload requested from the controller (UPLOADED, UPLOAD_FAILED).
 */
public record DiagnosticsStatusRequest(String status) implements Serializable {
}
