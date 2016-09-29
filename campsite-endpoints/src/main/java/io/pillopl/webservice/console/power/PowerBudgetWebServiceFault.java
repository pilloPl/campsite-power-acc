package io.pillopl.webservice.console.power;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A call of the PowerBudgetWebService failed.
 */
public class PowerBudgetWebServiceFault extends WebServiceFault {

    public PowerBudgetWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
