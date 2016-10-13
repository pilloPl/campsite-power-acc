package io.pillopl.webservice.console.erp;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A call of the ErpBillingWebService failed.
 */
public class ErpBillingWebServiceFault extends WebServiceFault {

    public ErpBillingWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
