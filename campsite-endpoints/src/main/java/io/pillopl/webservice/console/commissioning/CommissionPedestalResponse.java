package io.pillopl.webservice.console.commissioning;

import java.io.Serializable;

/**
 * The pedestal is commissioned.
 */
public record CommissionPedestalResponse(String pedestalCode, boolean commissioned) implements Serializable {
}
