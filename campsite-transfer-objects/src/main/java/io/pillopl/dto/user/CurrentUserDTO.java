package io.pillopl.dto.user;

import io.pillopl.dto.group.GroupDTO;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The logged-in console user: effective roles (direct and through security groups) and rights decide which tabs the
 * console shows; the scope groups say which campsites, issuers and staff the user sees.
 */
public record CurrentUserDTO(String username, String name, String email, String status, List<String> roles,
                             List<String> rights, List<GroupDTO> scopeGroups, LocalDateTime lastLoginDate)
        implements Serializable {

    public CurrentUserDTO {
        roles = roles != null ? List.copyOf(roles) : List.of();
        rights = rights != null ? List.copyOf(rights) : List.of();
        scopeGroups = scopeGroups != null ? List.copyOf(scopeGroups) : List.of();
    }
}
