package io.pillopl.webservice.console.card;

import io.pillopl.dto.card.CampingCardDTO;

import java.io.Serializable;

/**
 * The card after the change.
 */
public record ChangeCardStatusResponse(CampingCardDTO card) implements Serializable {
}
