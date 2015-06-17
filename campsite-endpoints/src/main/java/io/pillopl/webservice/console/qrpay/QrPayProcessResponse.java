package io.pillopl.webservice.console.qrpay;

import io.pillopl.dto.qrpay.QrPayProcessDTO;

import java.io.Serializable;

/**
 * The QR payment after the call: state, supply and cost.
 */
public record QrPayProcessResponse(QrPayProcessDTO qrPayProcess) implements Serializable {
}
