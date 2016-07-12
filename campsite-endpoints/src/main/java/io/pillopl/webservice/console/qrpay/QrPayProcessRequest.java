package io.pillopl.webservice.console.qrpay;

import java.io.Serializable;

/**
 * Stop or query of a QR payment by its order reference; the app user must be the one that started it.
 */
public record QrPayProcessRequest(String orderReference, String appUserId)
        implements Serializable {
}
