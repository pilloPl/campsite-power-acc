package io.pillopl.webservice.console.report;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A call of the ReportWebService failed.
 */
public class ReportWebServiceFault extends WebServiceFault {

    public ReportWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
