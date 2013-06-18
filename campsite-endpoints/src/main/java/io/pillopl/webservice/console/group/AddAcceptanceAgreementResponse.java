package io.pillopl.webservice.console.group;

import io.pillopl.dto.group.AcceptanceAgreementDTO;

import java.io.Serializable;

/**
 * Agreements in force after the change.
 */
public record AddAcceptanceAgreementResponse(AcceptanceAgreementDTO agreement) implements Serializable {
}
