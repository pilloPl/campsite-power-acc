package io.pillopl.api.exception;

/**
 * Base of the exceptions the service interfaces throw to the web layer, which turns them into faults.
 */
public abstract class ApiException extends RuntimeException {

    protected ApiException(String message) {
        super(message);
    }

    protected ApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
