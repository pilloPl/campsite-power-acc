package io.pillopl.webservice.console.issuer;

import java.io.Serializable;

/**
 * The removed filter; the group follows the nearest filter above it again.
 */
public record DeleteWhitelistFilterResponse(Long deletedFilterId) implements Serializable {
}
