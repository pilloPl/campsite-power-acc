package io.pillopl.webservice.console.power;

import java.io.Serializable;

/**
 * Hangs a socket on a section; sectionId null takes it off (no section limit).
 */
public record AssignSocketToSectionRequest(Long socketId, Long sectionId) implements Serializable {
}
