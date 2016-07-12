package io.pillopl.webservice.console.user;

import java.io.Serializable;

/**
 * New self-service user; the username is made from the email.
 */
public record CreateUserRequest(String email, String name, String market) implements Serializable {
}
