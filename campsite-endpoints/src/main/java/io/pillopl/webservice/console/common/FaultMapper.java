package io.pillopl.webservice.console.common;

import io.pillopl.api.exception.AccessNotAllowedException;
import io.pillopl.api.exception.ConflictException;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.api.exception.ObjectNotFoundException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Error code of a fault from the exception of the call. Invalid request values (value objects, paging) are wrong
 * requests; everything unknown is a technical error.
 */
public final class FaultMapper {

    private FaultMapper() {
        throw new AssertionError();
    }

    public static WsErrorCode toErrorCode(Throwable e) {
        if (e instanceof ObjectNotFoundException) {
            return WsErrorCode.NOT_EXISTS;
        }
        if (e instanceof AccessNotAllowedException) {
            return WsErrorCode.NOT_AUTHORIZED;
        }
        if (e instanceof InvalidInputException || e instanceof IllegalArgumentException) {
            // includes the InvalidValueException of the value objects
            return WsErrorCode.WRONG_REQUEST;
        }
        if (e instanceof ConflictException) {
            return WsErrorCode.CONFLICT;
        }
        if (e instanceof DataIntegrityViolationException) {
            return WsErrorCode.ALREADY_EXISTS;
        }
        if (e instanceof UnsupportedOperationException) {
            return WsErrorCode.METHOD_NOT_AVAILABLE;
        }
        if (e instanceof DataAccessException) {
            return WsErrorCode.TECHNICAL_ERROR;
        }
        return WsErrorCode.TECHNICAL_ERROR;
    }

    /**
     * Technical errors do not show their message to the console user.
     */
    public static String toMessage(Throwable e, WsErrorCode errorCode) {
        if (errorCode == WsErrorCode.TECHNICAL_ERROR) {
            return e instanceof DataAccessException ? "Data access error" : "Technical error";
        }
        return e.getMessage();
    }
}
