package io.pillopl.webservice.console.administration;

import io.pillopl.dto.user.UserroleDTO;

import java.io.Serializable;
import java.util.List;

/**
 * Roles in their sort order.
 */
public record FindRolesResponse(List<UserroleDTO> roles) implements Serializable {

    public FindRolesResponse {
        roles = roles != null ? List.copyOf(roles) : List.of();
    }
}
