package io.pillopl.webservice.console.administration;

import java.io.Serializable;

/**
 * Stores a configuration value (audited).
 */
public record SetSystemPropertyRequest(String key, String value, String description)
        implements Serializable {
}
