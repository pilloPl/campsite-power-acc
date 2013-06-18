package io.pillopl.webservice.console.group;

import io.pillopl.dto.group.AcceptanceAgreementDTO;

import java.io.Serializable;

/**
 * Agreements in force for the issuer group.
 */
public record FindAcceptanceAgreementsResponse(AcceptanceAgreementDTO agreement) implements Serializable {
}
