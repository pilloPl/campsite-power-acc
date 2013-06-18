package io.pillopl.webservice.console.group;

import java.io.Serializable;

/**
 * Where the cards of an ISSUER group are accepted.
 */
public record FindAcceptanceAgreementsRequest(String authUsername, Long issuerGroupId) implements Serializable {
}
