package io.pillopl.webservice.console.auth;

import io.pillopl.dto.user.CurrentUserDTO;

import java.io.Serializable;

/**
 * The logged-in user; the session cookie of the answer identifies it in the following calls.
 */
public record LoginResponse(CurrentUserDTO user) implements Serializable {
}
