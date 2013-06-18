package io.pillopl.webservice.console.group;

import io.pillopl.dto.group.GroupDTO;

import java.io.Serializable;
import java.util.List;

/**
 * Groups found, the scope group first.
 */
public record FindManageableGroupsResponse(List<GroupDTO> groups) implements Serializable {

    public FindManageableGroupsResponse {
        groups = groups != null ? List.copyOf(groups) : List.of();
    }
}
