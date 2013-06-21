package io.pillopl.webservice.console.pedestal;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A call of the PedestalWebService failed.
 */
public class PedestalWebServiceFault extends WebServiceFault {

    public PedestalWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
