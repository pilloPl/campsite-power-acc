package io.pillopl.webservice.console.auth;

import io.pillopl.dto.user.CurrentUserDTO;

import java.io.Serializable;

/**
 * The logged-in user with roles and rights (the console shows the tabs of its rights).
 */
public record GetCurrentUserResponse(CurrentUserDTO user) implements Serializable {
}
