package io.pillopl.webservice.console.user;

import java.io.Serializable;

/**
 * A user in the calling user's STAFF scope (or the calling user itself).
 */
public record FindUserRequest(String authUsername, String username) implements Serializable {
}
