package io.pillopl.webservice.console.pedestal;

import java.io.Serializable;

/**
 * The pedestal is deleted.
 */
public record DeletePedestalResponse(String pedestalCode, boolean deleted) implements Serializable {
}
