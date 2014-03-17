package io.pillopl.webservice.console.housecard;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A call of the HouseCardWebService failed.
 */
public class HouseCardWebServiceFault extends WebServiceFault {

    public HouseCardWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
