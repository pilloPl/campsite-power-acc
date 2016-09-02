package io.pillopl.dto.admin;

import io.pillopl.dto.user.UserroleDTO;

import java.io.Serializable;
import java.util.List;

/**
 * Rights matrix of the administration tab: all rights (columns), every role with its rights (rows) and the
 * security groups bundling roles.
 */
public record RightsMatrixDTO(List<String> rights, List<UserroleDTO> roles, List<SecurityGroupDTO> securityGroups)
        implements Serializable {

    public RightsMatrixDTO {
        rights = rights != null ? List.copyOf(rights) : List.of();
        roles = roles != null ? List.copyOf(roles) : List.of();
        securityGroups = securityGroups != null ? List.copyOf(securityGroups) : List.of();
    }
}
