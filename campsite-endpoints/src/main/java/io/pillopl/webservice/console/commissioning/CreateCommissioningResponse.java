package io.pillopl.webservice.console.commissioning;

import io.pillopl.dto.infrastructure.CommissioningDTO;

import java.io.Serializable;

/**
 * The commissioning with the token for the installer.
 */
public record CreateCommissioningResponse(CommissioningDTO commissioning) implements Serializable {
}
