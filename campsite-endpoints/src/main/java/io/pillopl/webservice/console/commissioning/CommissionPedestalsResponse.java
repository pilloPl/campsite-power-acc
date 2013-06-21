package io.pillopl.webservice.console.commissioning;

import java.io.Serializable;
import java.util.List;

/**
 * Pedestals that could not be commissioned; empty when all were.
 */
public record CommissionPedestalsResponse(List<Long> failedPedestalIds) implements Serializable {

    public CommissionPedestalsResponse {
        failedPedestalIds = failedPedestalIds != null ? List.copyOf(failedPedestalIds) : List.of();
    }
}
