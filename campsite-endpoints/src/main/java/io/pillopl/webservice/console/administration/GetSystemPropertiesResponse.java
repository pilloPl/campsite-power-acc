package io.pillopl.webservice.console.administration;

import io.pillopl.dto.admin.SystemPropertyDTO;

import java.io.Serializable;
import java.util.List;

/**
 * Keys in the order of the catalogue.
 */
public record GetSystemPropertiesResponse(List<SystemPropertyDTO> properties) implements Serializable {

    public GetSystemPropertiesResponse {
        properties = properties != null ? List.copyOf(properties) : List.of();
    }
}
