package io.pillopl.dto.user;

import io.pillopl.dto.group.GroupDTO;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A console user: own STAFF group (who manages the user), scope groups (one per tree) and role names.
 */
public record UserinfoDTO(Long id, String username, String name, String email, String status, String emailLocale,
                          GroupDTO group, List<GroupDTO> scopeGroups, List<String> roles, LocalDateTime createDate)
        implements Serializable {

    public UserinfoDTO {
        scopeGroups = scopeGroups != null ? List.copyOf(scopeGroups) : List.of();
        roles = roles != null ? List.copyOf(roles) : List.of();
    }
}
