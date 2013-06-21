package io.pillopl.webservice.console.commissioning;

import io.pillopl.dto.infrastructure.SupplierStockEntryDTO;

import java.io.Serializable;
import java.util.List;

/**
 * Pedestals in the supplier stock.
 */
public record GetSupplierStockResponse(List<SupplierStockEntryDTO> pedestals) implements Serializable {

    public GetSupplierStockResponse {
        pedestals = pedestals != null ? List.copyOf(pedestals) : List.of();
    }
}
