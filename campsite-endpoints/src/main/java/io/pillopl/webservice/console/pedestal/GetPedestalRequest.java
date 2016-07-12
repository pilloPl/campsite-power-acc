package io.pillopl.webservice.console.pedestal;

import java.io.Serializable;

/**
 * Pedestal detail by its code.
 */
public record GetPedestalRequest(String pedestalCode) implements Serializable {
}
