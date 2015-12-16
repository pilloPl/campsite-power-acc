package io.pillopl.webservice.pedestallink;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A message of a pedestal on link v2 or v3 could not be processed (unknown pedestal or socket, unknown status,
 * technical error).
 */
public class PedestalLinkWebServiceFault extends WebServiceFault {

    public PedestalLinkWebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
