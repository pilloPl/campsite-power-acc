package io.pillopl.webservice.console.user;

import java.io.Serializable;

/**
 * New email and/or name of a user; blank fields stay.
 */
public record UpdateUserRequest(String username, String email, String name)
        implements Serializable {
}
