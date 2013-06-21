package io.pillopl.webservice.console.commissioning;

import java.io.Serializable;
import java.util.List;

/**
 * Codes of the imported pedestals.
 */
public record ImportSupplierPedestalsResponse(List<String> pedestalCodes) implements Serializable {

    public ImportSupplierPedestalsResponse {
        pedestalCodes = pedestalCodes != null ? List.copyOf(pedestalCodes) : List.of();
    }
}
