package io.pillopl.webservice.console.job;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A call of the JobWebService failed.
 */
public class JobWebServiceFault extends WebServiceFault {

    public JobWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
