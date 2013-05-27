package io.pillopl.api.exception;

/**
 * The object named in the call does not exist (or is deleted).
 */
public class ObjectNotFoundException extends ApiException {

    public ObjectNotFoundException(String message) {
        super(message);
    }

    public static ObjectNotFoundException of(String what, Object key) {
        return new ObjectNotFoundException(String.format("%s '%s' not found", what, key));
    }
}
