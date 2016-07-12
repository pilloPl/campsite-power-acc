package io.pillopl.webservice.console.auth;

import java.io.Serializable;

/**
 * The session is closed; loggedOut is false when there was no login.
 */
public record LogoutResponse(boolean loggedOut) implements Serializable {
}
