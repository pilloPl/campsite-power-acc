package io.pillopl.webservice.console.user;

import io.pillopl.dto.user.UserinfoDTO;

import java.io.Serializable;

/**
 * The owner after the set-up.
 */
public record SetupAffiliateOwnerResponse(UserinfoDTO user) implements Serializable {
}
