package io.pillopl.webservice.console.commissioning;

import io.pillopl.dto.infrastructure.CommissioningDTO;

import java.io.Serializable;

/**
 * The open commissioning.
 */
public record FindCommissioningByTokenResponse(CommissioningDTO commissioning) implements Serializable {
}
