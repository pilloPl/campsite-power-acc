package io.pillopl.webservice.console.card;

import io.pillopl.dto.card.CardCheckDTO;

import java.io.Serializable;

/**
 * What the pedestal would answer.
 */
public record CheckCardAtSocketResponse(CardCheckDTO check) implements Serializable {
}
