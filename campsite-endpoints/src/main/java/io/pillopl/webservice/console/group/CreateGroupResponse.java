package io.pillopl.webservice.console.group;

import io.pillopl.dto.group.GroupDTO;

import java.io.Serializable;

/**
 * The created group.
 */
public record CreateGroupResponse(GroupDTO group) implements Serializable {
}
