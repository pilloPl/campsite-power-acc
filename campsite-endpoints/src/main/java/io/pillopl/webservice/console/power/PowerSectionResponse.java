package io.pillopl.webservice.console.power;

import io.pillopl.dto.power.PowerSectionDTO;

import java.io.Serializable;

/**
 * The section after the change (null when a socket was taken off its section).
 */
public record PowerSectionResponse(PowerSectionDTO section) implements Serializable {
}
