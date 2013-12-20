package io.pillopl.webservice.console.tariff;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Tells which tariff prices supplies on a socket at a time (now when no time is given) and why.
 */
public record FindSocketTariffRequest(String authUsername, Long socketId, LocalDateTime at) implements Serializable {
}
