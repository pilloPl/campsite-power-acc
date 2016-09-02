package io.pillopl.webservice.console.administration;

import java.io.Serializable;

/**
 * Reads SYSTEM_PROPERTY again after changes made directly in the database.
 */
public record ReloadConfigurationRequest() implements Serializable {
}
