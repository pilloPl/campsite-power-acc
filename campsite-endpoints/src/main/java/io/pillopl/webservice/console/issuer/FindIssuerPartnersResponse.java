package io.pillopl.webservice.console.issuer;

import io.pillopl.dto.admin.IssuerPartnerDTO;

import java.io.Serializable;
import java.util.List;

/**
 * The partners by name.
 */
public record FindIssuerPartnersResponse(List<IssuerPartnerDTO> partners) implements Serializable {

    public FindIssuerPartnersResponse {
        partners = partners != null ? List.copyOf(partners) : List.of();
    }
}
