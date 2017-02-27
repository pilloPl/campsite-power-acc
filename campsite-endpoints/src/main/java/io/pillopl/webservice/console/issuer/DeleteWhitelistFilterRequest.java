package io.pillopl.webservice.console.issuer;

import java.io.Serializable;

/**
 * The whitelist filter to remove.
 */
public record DeleteWhitelistFilterRequest(Long filterId) implements Serializable {
}
