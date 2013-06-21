package io.pillopl.webservice.console.commissioning;

import io.pillopl.dto.infrastructure.PedestalImportDTO;

import java.io.Serializable;
import java.util.List;

/**
 * Pedestals of a supplier delivery (or of the network) entered in the console.
 */
public record ImportSupplierPedestalsRequest(String authUsername, Boolean networkOwned,
                                             List<PedestalImportDTO> pedestals)
        implements Serializable {

    public ImportSupplierPedestalsRequest {
        pedestals = pedestals != null ? List.copyOf(pedestals) : List.of();
    }
}
