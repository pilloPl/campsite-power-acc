package io.pillopl.webservice.console.dashboard;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A call of the DashboardWebService failed.
 */
public class DashboardWebServiceFault extends WebServiceFault {

    public DashboardWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
