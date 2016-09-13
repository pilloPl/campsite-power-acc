package io.pillopl.webservice.console.qrpayconsole;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A call of the QrPayConsoleWebService failed.
 */
public class QrPayConsoleWebServiceFault extends WebServiceFault {

    public QrPayConsoleWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
