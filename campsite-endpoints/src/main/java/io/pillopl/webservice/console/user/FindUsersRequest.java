package io.pillopl.webservice.console.user;

import java.io.Serializable;

/**
 * Users the caller administers, optionally matching a search in username, name or e-mail.
 */
public record FindUsersRequest(String search) implements Serializable {
}
