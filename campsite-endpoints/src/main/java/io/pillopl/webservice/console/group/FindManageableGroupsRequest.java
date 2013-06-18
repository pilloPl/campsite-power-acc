package io.pillopl.webservice.console.group;

import java.io.Serializable;

/**
 * Groups of one tree in the scope of the calling user, optionally filtered by display name.
 */
public record FindManageableGroupsRequest(String authUsername, String groupType, String search)
        implements Serializable {
}
