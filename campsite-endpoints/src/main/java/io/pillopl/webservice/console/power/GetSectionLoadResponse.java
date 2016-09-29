package io.pillopl.webservice.console.power;

import io.pillopl.dto.power.SectionLoadDTO;

import java.io.Serializable;

/**
 * Load of the section per hour against its capacity.
 */
public record GetSectionLoadResponse(SectionLoadDTO load) implements Serializable {
}
