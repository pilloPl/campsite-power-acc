package io.pillopl.webservice.console.dashboard;

import java.io.Serializable;

/**
 * Acknowledges the alert with the key from findAlerts, with an optional comment.
 */
public record AcknowledgeAlertRequest(String alertKey, String comment) implements Serializable {
}
