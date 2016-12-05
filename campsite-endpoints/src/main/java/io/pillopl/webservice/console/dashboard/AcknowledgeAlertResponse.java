package io.pillopl.webservice.console.dashboard;

import io.pillopl.dto.monitoring.AlertDTO;

import java.io.Serializable;

/**
 * The acknowledged alert.
 */
public record AcknowledgeAlertResponse(AlertDTO alert) implements Serializable {
}
