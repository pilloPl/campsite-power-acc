package io.pillopl.dto.admin;

import java.io.Serializable;

/**
 * A configuration key with its stored value (null when the coded default applies).
 */
public record SystemPropertyDTO(String key, String value, String description) implements Serializable {
}
