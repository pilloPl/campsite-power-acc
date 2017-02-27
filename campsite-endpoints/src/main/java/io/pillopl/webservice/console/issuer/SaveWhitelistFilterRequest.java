package io.pillopl.webservice.console.issuer;

import io.pillopl.dto.admin.WhitelistFilterDTO;

import java.io.Serializable;

/**
 * Sets the whitelist filter of an ISSUER group (id null for a group without one).
 */
public record SaveWhitelistFilterRequest(WhitelistFilterDTO filter) implements Serializable {
}
