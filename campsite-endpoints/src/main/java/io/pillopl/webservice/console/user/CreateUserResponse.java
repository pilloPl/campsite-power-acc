package io.pillopl.webservice.console.user;

import io.pillopl.dto.user.UserinfoDTO;

import java.io.Serializable;

/**
 * The created user.
 */
public record CreateUserResponse(UserinfoDTO user) implements Serializable {
}
