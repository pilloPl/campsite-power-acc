package io.pillopl.webservice.pedestallink.v1;

import io.pillopl.webservice.console.common.WebServiceFault;
import io.pillopl.webservice.console.common.WsErrorCode;

/**
 * A message of a pedestal on link v1 could not be processed (unknown socket, unknown status, technical error).
 */
public class PedestalLinkV1WebServiceFault extends WebServiceFault {

    public PedestalLinkV1WebServiceFault(String message, WsErrorCode errorCode) {
        super(message, errorCode);
    }
}
