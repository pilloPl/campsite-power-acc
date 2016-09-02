package io.pillopl.dto.admin;

import java.io.Serializable;
import java.util.List;

/**
 * A security group and the roles a user gets through it.
 */
public record SecurityGroupDTO(String name, List<String> roles) implements Serializable {

    public SecurityGroupDTO {
        roles = roles != null ? List.copyOf(roles) : List.of();
    }
}
