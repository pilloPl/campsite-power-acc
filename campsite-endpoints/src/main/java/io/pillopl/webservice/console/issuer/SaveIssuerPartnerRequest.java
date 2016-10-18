package io.pillopl.webservice.console.issuer;

import io.pillopl.dto.admin.IssuerPartnerDTO;

import java.io.Serializable;

/**
 * A new partner (id null) or the changed partner; names of groups and the creation date are ignored.
 */
public record SaveIssuerPartnerRequest(IssuerPartnerDTO partner) implements Serializable {
}
