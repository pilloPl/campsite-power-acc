package io.pillopl.webservice.console.card;

import io.pillopl.dto.card.CampingCardDTO;

import java.io.Serializable;

/**
 * The card as it belongs to the guest now.
 */
public record AssignWelcomeCardResponse(CampingCardDTO card) implements Serializable {
}
