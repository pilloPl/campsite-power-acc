package io.pillopl.api.exception;

/**
 * The calling user is unknown, lacks the right, or the object lies outside its scope.
 */
public class AccessNotAllowedException extends ApiException {

    public AccessNotAllowedException(String message) {
        super(message);
    }

    public AccessNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }
}
