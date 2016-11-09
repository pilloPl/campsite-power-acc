package io.pillopl.webservice.console.supply;

import io.pillopl.dto.infrastructure.PedestalCommandDTO;

import java.io.Serializable;

/**
 * The command the pedestal accepted (supply start/stop, status request, fault reset, fault mail); a refused command
 * is a CONFLICT fault.
 */
public record PedestalCommandResponse(PedestalCommandDTO command) implements Serializable {
}
