package io.pillopl.webservice.console.map;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A call of the MapWebService failed.
 */
public class MapWebServiceFault extends WebServiceFault {

    public MapWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
