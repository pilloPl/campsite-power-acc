package io.pillopl.webservice.console.group;

import java.io.Serializable;

/**
 * Cards of the ISSUER group are accepted in the subtree of the SITE group from now on.
 */
public record AddAcceptanceAgreementRequest(String authUsername, Long issuerGroupId, Long siteGroupId)
        implements Serializable {
}
