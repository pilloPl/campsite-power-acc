package io.pillopl.webservice.console.commissioning;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A call of the CommissioningWebService failed.
 */
public class CommissioningWebServiceFault extends WebServiceFault {

    public CommissioningWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
