package io.pillopl.webservice.console.user;

import java.io.Serializable;

/**
 * Locks (LOCKED), unlocks (ACTIVE) or deactivates (DEACTIVATED) a user.
 */
public record ChangeUserStatusRequest(String username, String status) implements Serializable {
}
