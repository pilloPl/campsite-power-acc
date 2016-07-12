package io.pillopl.webservice.console.user;

import java.io.Serializable;

/**
 * New password of a user (at least 8 characters); an invited user becomes active. Never written to the audit.
 */
public record SetPasswordRequest(String username, String password) implements Serializable {

    @Override
    public String toString() {
        return "SetPasswordRequest{username='" + username + "'}";
    }
}
