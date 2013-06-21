package io.pillopl.webservice.console.pedestal;

import io.pillopl.dto.infrastructure.SocketDTO;

import java.io.Serializable;

/**
 * The socket after the change.
 */
public record SetFreeSocketResponse(SocketDTO socket) implements Serializable {
}
