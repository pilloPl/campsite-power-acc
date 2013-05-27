package io.pillopl.api.exception;

/**
 * The call contradicts the current state: the object already exists, is already commissioned, a set-up is missing.
 */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
