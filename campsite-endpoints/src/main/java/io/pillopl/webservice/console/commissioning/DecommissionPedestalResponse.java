package io.pillopl.webservice.console.commissioning;

import java.io.Serializable;

/**
 * The pedestal is back in the supplier stock.
 */
public record DecommissionPedestalResponse(Long pedestalId, boolean decommissioned) implements Serializable {
}
