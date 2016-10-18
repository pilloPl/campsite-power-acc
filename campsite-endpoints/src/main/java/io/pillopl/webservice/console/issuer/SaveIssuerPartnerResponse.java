package io.pillopl.webservice.console.issuer;

import io.pillopl.dto.admin.IssuerPartnerDTO;

import java.io.Serializable;

/**
 * The partner as saved.
 */
public record SaveIssuerPartnerResponse(IssuerPartnerDTO partner) implements Serializable {
}
