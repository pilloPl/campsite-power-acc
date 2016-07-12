package io.pillopl.webservice.console.card;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * New validity of a card (stay extended or shortened).
 */
public record ChangeCardValidityRequest(Long cardId, LocalDate validFrom, LocalDate validTo) implements Serializable {
}
