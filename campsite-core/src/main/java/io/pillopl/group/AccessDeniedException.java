package io.pillopl.group;

/**
 * Thrown when a group tries to manage a group outside of its subtree.
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
