package io.pillopl.webservice.console.report;

import java.io.Serializable;

/**
 * The utilization view is refreshed.
 */
public record RefreshUtilizationResponse(boolean refreshed) implements Serializable {
}
