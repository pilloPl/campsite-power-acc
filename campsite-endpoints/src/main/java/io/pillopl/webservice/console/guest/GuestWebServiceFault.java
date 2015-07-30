package io.pillopl.webservice.console.guest;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A call of the GuestWebService failed.
 */
public class GuestWebServiceFault extends WebServiceFault {

    public GuestWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
