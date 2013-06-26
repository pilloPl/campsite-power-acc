package io.pillopl.webservice.console.user;

import java.io.Serializable;

/**
 * Sets a user up as owner of an affiliate campsite (group and roles).
 */
public record SetupAffiliateOwnerRequest(String authUsername, String ownerUsername) implements Serializable {
}
