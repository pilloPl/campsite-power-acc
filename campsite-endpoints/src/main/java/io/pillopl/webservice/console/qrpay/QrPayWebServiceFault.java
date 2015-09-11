package io.pillopl.webservice.console.qrpay;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A call of the QrPayWebService failed.
 */
public class QrPayWebServiceFault extends WebServiceFault {

    public QrPayWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
