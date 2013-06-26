package io.pillopl.webservice.console.user;

import io.pillopl.dto.user.UserinfoDTO;

import java.io.Serializable;
import java.util.List;

/**
 * Users found.
 */
public record FindUsersByRolesResponse(List<UserinfoDTO> users) implements Serializable {

    public FindUsersByRolesResponse {
        users = users != null ? List.copyOf(users) : List.of();
    }
}
