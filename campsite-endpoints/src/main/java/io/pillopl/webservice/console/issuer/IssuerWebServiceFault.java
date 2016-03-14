package io.pillopl.webservice.console.issuer;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A call of the IssuerWebService failed.
 */
public class IssuerWebServiceFault extends WebServiceFault {

    public IssuerWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
