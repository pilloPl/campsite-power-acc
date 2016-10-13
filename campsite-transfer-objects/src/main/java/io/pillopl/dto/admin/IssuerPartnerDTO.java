package io.pillopl.dto.admin;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * An issuer partner as the console edits it: the ISSUER group of its cards, the SITE group of its own campsites
 * (needed for the card export and the record import), the interface (V1 files, V2 REST) with its endpoint and the
 * four directions of the exchange. id null creates a partner.
 */
public record IssuerPartnerDTO(Long id, String name, String displayName, Long issuerGroupId, String issuerGroupName,
                               Long siteGroupId, String siteGroupName, String interfaceVersion, String endpoint,
                               boolean cardImport, boolean cardExport, boolean recordExport, boolean recordImport,
                               boolean active, LocalDateTime createDate) implements Serializable {
}
