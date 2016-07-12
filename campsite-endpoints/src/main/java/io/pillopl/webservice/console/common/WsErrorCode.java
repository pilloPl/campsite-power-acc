package io.pillopl.webservice.console.common;

/**
 * Error codes of the web service faults, each with the HTTP status the fault is answered with.
 */
public enum WsErrorCode {
    OK(200),
    WRONG_REQUEST(400),
    NOT_AUTHENTICATED(401),
    NOT_AUTHORIZED(403),
    NOT_EXISTS(404),
    ALREADY_EXISTS(409),
    CONFLICT(409),
    TECHNICAL_ERROR(500),
    METHOD_NOT_AVAILABLE(501);

    private final int httpStatus;

    WsErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
