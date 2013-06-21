package io.pillopl.webservice.console.commissioning;

import io.pillopl.dto.infrastructure.CommissioningDTO;

import java.io.Serializable;
import java.util.List;

/**
 * Open commissionings.
 */
public record FindCommissioningsResponse(List<CommissioningDTO> commissionings) implements Serializable {

    public FindCommissioningsResponse {
        commissionings = commissionings != null ? List.copyOf(commissionings) : List.of();
    }
}
