package io.pillopl.webservice.console.supply;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A call of the SupplyWebService failed.
 */
public class SupplyWebServiceFault extends WebServiceFault {

    public SupplyWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
