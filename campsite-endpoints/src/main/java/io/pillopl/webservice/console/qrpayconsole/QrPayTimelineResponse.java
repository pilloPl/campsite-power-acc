package io.pillopl.webservice.console.qrpayconsole;

import io.pillopl.dto.qrpay.QrPayTimelineDTO;

import java.io.Serializable;

/**
 * The timeline of the QR payment (after a cancel: with the states the cancel went through).
 */
public record QrPayTimelineResponse(QrPayTimelineDTO timeline) implements Serializable {
}
