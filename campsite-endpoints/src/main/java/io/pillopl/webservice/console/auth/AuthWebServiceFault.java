package io.pillopl.webservice.console.auth;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A call of the AuthWebService failed.
 */
public class AuthWebServiceFault extends WebServiceFault {

    public AuthWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
