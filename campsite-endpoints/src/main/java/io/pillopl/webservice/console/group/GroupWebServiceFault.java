package io.pillopl.webservice.console.group;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A call of the GroupWebService failed.
 */
public class GroupWebServiceFault extends WebServiceFault {

    public GroupWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
