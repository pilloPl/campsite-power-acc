package io.pillopl.webservice.console.issuer;

import io.pillopl.dto.admin.WhitelistFilterDTO;

import java.io.Serializable;
import java.util.List;

/**
 * Whitelist filters ordered by group name.
 */
public record FindWhitelistFiltersResponse(List<WhitelistFilterDTO> filters) implements Serializable {
}
