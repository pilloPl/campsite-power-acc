package io.pillopl.webservice.console.group;

import java.io.Serializable;

/**
 * New child group; the type is the type of the parent.
 */
public record CreateGroupRequest(String authUsername, Long parentGroupId, String name, String displayName)
        implements Serializable {
}
