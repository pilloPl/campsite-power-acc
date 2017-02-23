package io.pillopl.webservice.console.card;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * A card of the welcome card pool handed to a guest for the validity (usually the stay; from today when the first
 * day is missing).
 */
public record AssignWelcomeCardRequest(Long cardId, Long guestId, LocalDate validFrom, LocalDate validTo)
        implements Serializable {
}
