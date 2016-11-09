package io.pillopl.webservice.console.supply;

import java.io.Serializable;

/**
 * A supply session to stop remotely (stopSupply) or to close as stale (closeStaleSession).
 */
public record SupplySessionRequest(Long sessionId) implements Serializable {
}
