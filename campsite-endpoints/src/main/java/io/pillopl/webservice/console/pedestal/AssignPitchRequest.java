package io.pillopl.webservice.console.pedestal;

import java.io.Serializable;

/**
 * Assigns a socket to a pitch; a blank pitch removes the assignment.
 */
public record AssignPitchRequest(String authUsername, Long socketId, String pitchNo) implements Serializable {
}
