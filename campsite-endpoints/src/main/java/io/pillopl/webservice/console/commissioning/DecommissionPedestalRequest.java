package io.pillopl.webservice.console.commissioning;

import java.io.Serializable;

/**
 * The owner sends a pedestal back to the supplier.
 */
public record DecommissionPedestalRequest(Long pedestalId) implements Serializable {
}
