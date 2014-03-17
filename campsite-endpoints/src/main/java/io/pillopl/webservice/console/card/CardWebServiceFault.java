package io.pillopl.webservice.console.card;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A call of the CardWebService failed.
 */
public class CardWebServiceFault extends WebServiceFault {

    public CardWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
