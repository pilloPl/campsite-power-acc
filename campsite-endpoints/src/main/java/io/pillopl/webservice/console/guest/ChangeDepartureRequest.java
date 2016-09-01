package io.pillopl.webservice.console.guest;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * The stay ends on another day (extended or shortened); the card handed out for it follows.
 */
public record ChangeDepartureRequest(Long stayId, LocalDate departure) implements Serializable {
}
