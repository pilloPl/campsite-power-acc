package io.pillopl.webservice.console.group;

import java.io.Serializable;

/**
 * False when the check found errors (they are logged).
 */
public record CheckGroupValidityResponse(boolean valid) implements Serializable {
}
