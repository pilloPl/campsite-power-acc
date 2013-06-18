package io.pillopl.webservice.console.common;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Answers the faults of the web services with the HTTP status of their error code and the fault info as body.
 */
@RestControllerAdvice(basePackages = "io.pillopl.webservice")
public class WebServiceFaultHandler {

    @ExceptionHandler(WebServiceFault.class)
    public ResponseEntity<FaultInfo> handleFault(WebServiceFault fault) {
        return ResponseEntity.status(fault.getErrorCode().getHttpStatus()).body(fault.getFaultInfo());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<FaultInfo> handleUnreadableRequest(HttpMessageNotReadableException e) {
        return ResponseEntity.status(WsErrorCode.WRONG_REQUEST.getHttpStatus())
                .body(new FaultInfo(WsErrorCode.WRONG_REQUEST, "Request could not be read"));
    }
}
