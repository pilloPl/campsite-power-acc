package io.pillopl.webservice.console.issuer;

import io.pillopl.dto.admin.WhitelistFilterDTO;

import java.io.Serializable;

/**
 * The filter as stored.
 */
public record SaveWhitelistFilterResponse(WhitelistFilterDTO filter) implements Serializable {
}
