package io.pillopl.webservice.console.commissioning;

import java.io.Serializable;
import java.util.List;

/**
 * The installer mounts pedestals from the supplier stock for an open commissioning.
 */
public record CommissionPedestalsRequest(String authUsername, String token, List<Long> pedestalIds)
        implements Serializable {

    public CommissionPedestalsRequest {
        pedestalIds = pedestalIds != null ? List.copyOf(pedestalIds) : List.of();
    }
}
