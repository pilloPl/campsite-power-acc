package io.pillopl.api.exception;

/**
 * A value of the call is missing or not acceptable.
 */
public class InvalidInputException extends ApiException {

    public InvalidInputException(String message) {
        super(message);
    }

    public InvalidInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
