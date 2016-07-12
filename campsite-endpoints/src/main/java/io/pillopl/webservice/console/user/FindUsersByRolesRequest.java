package io.pillopl.webservice.console.user;

import java.io.Serializable;
import java.util.List;

/**
 * Users with one of the roles in the calling user's STAFF scope.
 */
public record FindUsersByRolesRequest(List<String> roles) implements Serializable {

    public FindUsersByRolesRequest {
        roles = roles != null ? List.copyOf(roles) : List.of();
    }
}
