package io.pillopl.webservice.console.pedestal;

import java.io.Serializable;

/**
 * A free socket supplies without card and is never billed.
 */
public record SetFreeSocketRequest(String authUsername, Long socketId, Boolean freeSocket) implements Serializable {
}
