package io.pillopl.webservice.console.monitoring;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A call of the MonitoringWebService failed.
 */
public class MonitoringWebServiceFault extends WebServiceFault {

    public MonitoringWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
