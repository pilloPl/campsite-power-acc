package io.pillopl.dto.admin;

import java.io.Serializable;

/**
 * Whitelist filter of an ISSUER group: whether the cards of its subtree take part in the exchange with the issuer
 * partners, for the direction (IMPORT_EXPORT, IMPORT, EXPORT) and one partner or all (issuerPartnerId null). One
 * filter per group; id null creates it.
 */
public record WhitelistFilterDTO(Long id, Long groupId, String groupName, String groupDisplayName,
                                 boolean synchronizable, String filterType, Long issuerPartnerId,
                                 String issuerPartnerName) implements Serializable {
}
