package io.pillopl.webservice.console.user;

import io.pillopl.dto.user.UserinfoDTO;

import java.io.Serializable;

/**
 * The user with groups and roles.
 */
public record FindUserResponse(UserinfoDTO user) implements Serializable {
}
