package io.pillopl.webservice.console.group;

import java.io.Serializable;

/**
 * Effective attribute values of a group.
 */
public record GetGroupAttributesRequest(String authUsername, Long groupId) implements Serializable {
}
