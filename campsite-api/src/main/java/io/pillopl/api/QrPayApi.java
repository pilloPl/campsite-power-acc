package io.pillopl.api;

import io.pillopl.dto.qrpay.QrPayProcessDTO;
import io.pillopl.value.Username;

/**
 * QR payment at the socket, called by the payment app (right QRPAY_APP): the guest without a card scans the code
 * of the socket, the app starts the supply, follows it and stops it. Only the client and the app user that started
 * a payment may follow and stop it.
 */
public interface QrPayApi {

    /**
     * Starts the supply of the socket remotely.
     *
     * @param orderReference order id of the app, unique
     * @param socketCode     code of the socket's sticker, e.g. "HRBAY*P012*2"
     * @param appUserId      the guest as the app knows him
     * @return the payment; state ERROR with the error code when the socket cannot supply
     */
    QrPayProcessDTO startQrPay(Username authUsername, String orderReference, String socketCode, String appUserId);

    /**
     * Stops the supply of the payment remotely; the price follows with the pedestal's stop.
     */
    QrPayProcessDTO stopQrPay(Username authUsername, String orderReference, String appUserId);

    QrPayProcessDTO getQrPayProcess(Username authUsername, String orderReference, String appUserId);
}
