package io.pillopl.webservice.console.administration;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A call of the AdministrationWebService failed.
 */
public class AdministrationWebServiceFault extends WebServiceFault {

    public AdministrationWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
