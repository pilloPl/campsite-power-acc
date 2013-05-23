package io.pillopl.dto.user;

import java.io.Serializable;
import java.util.List;

/**
 * A role with the group types it is allowed in and its rights (one row of the rights matrix).
 */
public record UserroleDTO(String name, List<String> groupTypes, boolean selfService, List<String> rights)
        implements Serializable {

    public UserroleDTO {
        groupTypes = groupTypes != null ? List.copyOf(groupTypes) : List.of();
        rights = rights != null ? List.copyOf(rights) : List.of();
    }
}
