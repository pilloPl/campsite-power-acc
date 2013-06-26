package io.pillopl.webservice.console.user;

import io.pillopl.dto.user.UserinfoDTO;

import java.io.Serializable;

/**
 * The user after the change.
 */
public record UpdateUserResponse(UserinfoDTO user) implements Serializable {
}
