package io.pillopl.webservice.console.administration;

import java.io.Serializable;

/**
 * The view is refreshed.
 */
public record RefreshUserSummaryResponse(boolean refreshed) implements Serializable {
}
