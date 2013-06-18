package io.pillopl.webservice.console.common;

/**
 * Base of the faults of the web services; each web service has its own subclass.
 */
public abstract class WebServiceFault extends Exception {

    private final FaultInfo faultInfo;

    protected WebServiceFault(String message, WsErrorCode errorCode) {
        super(message);
        this.faultInfo = new FaultInfo(errorCode, message);
    }

    public FaultInfo getFaultInfo() {
        return faultInfo;
    }

    public WsErrorCode getErrorCode() {
        return faultInfo.errorCode();
    }
}
