package io.pillopl.webservice.console.guest;

import io.pillopl.dto.guest.SeasonalContractDTO;

import java.io.Serializable;
import java.util.List;

/**
 * The contracts, latest start first.
 */
public record FindSeasonalContractsResponse(List<SeasonalContractDTO> seasonalContracts) implements Serializable {

    public FindSeasonalContractsResponse {
        seasonalContracts = seasonalContracts != null ? List.copyOf(seasonalContracts) : List.of();
    }
}
