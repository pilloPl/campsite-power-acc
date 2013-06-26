package io.pillopl.webservice.console.administration;

import io.pillopl.dto.admin.SystemPropertyDTO;

import java.io.Serializable;

/**
 * The stored value.
 */
public record SetSystemPropertyResponse(SystemPropertyDTO property) implements Serializable {
}
