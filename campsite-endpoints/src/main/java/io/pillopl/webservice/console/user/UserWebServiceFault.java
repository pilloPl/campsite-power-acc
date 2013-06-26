package io.pillopl.webservice.console.user;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A call of the UserWebService failed.
 */
public class UserWebServiceFault extends WebServiceFault {

    public UserWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
