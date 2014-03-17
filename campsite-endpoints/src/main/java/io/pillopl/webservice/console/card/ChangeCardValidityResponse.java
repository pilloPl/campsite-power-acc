package io.pillopl.webservice.console.card;

import io.pillopl.dto.card.CampingCardDTO;

import java.io.Serializable;

/**
 * The card after the change.
 */
public record ChangeCardValidityResponse(CampingCardDTO card) implements Serializable {
}
