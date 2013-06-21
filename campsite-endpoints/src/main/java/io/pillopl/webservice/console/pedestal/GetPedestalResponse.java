package io.pillopl.webservice.console.pedestal;

import io.pillopl.dto.infrastructure.PedestalDTO;

import java.io.Serializable;

/**
 * The pedestal with its sockets.
 */
public record GetPedestalResponse(PedestalDTO pedestal) implements Serializable {
}
