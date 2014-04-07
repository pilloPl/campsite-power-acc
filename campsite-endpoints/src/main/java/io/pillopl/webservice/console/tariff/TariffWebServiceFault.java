package io.pillopl.webservice.console.tariff;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A call of the TariffWebService failed.
 */
public class TariffWebServiceFault extends WebServiceFault {

    public TariffWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
