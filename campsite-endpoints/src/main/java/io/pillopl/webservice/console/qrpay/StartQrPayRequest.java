package io.pillopl.webservice.console.qrpay;

import java.io.Serializable;

/**
 * Start of a QR payment: the app's order, the code the guest scanned on the socket and the guest as the app knows
 * him.
 */
public record StartQrPayRequest(String authUsername, String orderReference, String socketCode, String appUserId)
        implements Serializable {
}
