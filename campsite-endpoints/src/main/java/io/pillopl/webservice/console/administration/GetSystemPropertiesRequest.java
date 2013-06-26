package io.pillopl.webservice.console.administration;

import java.io.Serializable;

/**
 * All configuration keys with their stored values.
 */
public record GetSystemPropertiesRequest(String authUsername) implements Serializable {
}
