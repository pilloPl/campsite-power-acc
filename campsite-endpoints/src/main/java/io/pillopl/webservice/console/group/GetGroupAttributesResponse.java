package io.pillopl.webservice.console.group;

import io.pillopl.dto.group.GroupAttributesDTO;

import java.io.Serializable;

/**
 * Nearest-ancestor-wins values of the group.
 */
public record GetGroupAttributesResponse(GroupAttributesDTO attributes) implements Serializable {
}
