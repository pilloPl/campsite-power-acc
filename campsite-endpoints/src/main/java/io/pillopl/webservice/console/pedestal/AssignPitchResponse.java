package io.pillopl.webservice.console.pedestal;

import io.pillopl.dto.infrastructure.SocketDTO;

import java.io.Serializable;

/**
 * The socket after the assignment.
 */
public record AssignPitchResponse(SocketDTO socket) implements Serializable {
}
