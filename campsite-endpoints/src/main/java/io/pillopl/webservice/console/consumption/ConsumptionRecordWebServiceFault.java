package io.pillopl.webservice.console.consumption;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A call of the ConsumptionRecordWebService failed.
 */
public class ConsumptionRecordWebServiceFault extends WebServiceFault {

    public ConsumptionRecordWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
