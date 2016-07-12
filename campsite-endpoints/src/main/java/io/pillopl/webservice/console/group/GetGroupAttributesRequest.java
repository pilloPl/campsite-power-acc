package io.pillopl.webservice.console.group;

import java.io.Serializable;

/**
 * Effective attribute values of a group.
 */
public record GetGroupAttributesRequest(Long groupId) implements Serializable {
}
