package io.pillopl.dto.infrastructure;

import java.io.Serializable;

/**
 * Answer to a command the console sent to a pedestal (supply start or stop, status request, fault reset, fault mail):
 * the pedestal accepted it, the socket's status after the call and what happened. A refused command is a CONFLICT
 * fault, not an answer.
 */
public record PedestalCommandDTO(String command, String pedestalCode, Long socketId, Integer socketNo,
                                 boolean accepted, String socketStatus, String message) implements Serializable {
}
