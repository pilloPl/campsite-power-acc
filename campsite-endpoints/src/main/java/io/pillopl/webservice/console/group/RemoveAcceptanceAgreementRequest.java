package io.pillopl.webservice.console.group;

import java.io.Serializable;

/**
 * Ends the agreement of the ISSUER group with the SITE group.
 */
public record RemoveAcceptanceAgreementRequest(String authUsername, Long issuerGroupId, Long siteGroupId)
        implements Serializable {
}
