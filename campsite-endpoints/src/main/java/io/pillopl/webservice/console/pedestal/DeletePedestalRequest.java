package io.pillopl.webservice.console.pedestal;

import java.io.Serializable;

/**
 * Soft delete of a pedestal of an affiliate owner with all its sockets.
 */
public record DeletePedestalRequest(String ownerUsername, String pedestalCode)
        implements Serializable {
}
