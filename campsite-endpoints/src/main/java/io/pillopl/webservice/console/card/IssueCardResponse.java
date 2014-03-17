package io.pillopl.webservice.console.card;

import io.pillopl.dto.card.CampingCardDTO;

import java.io.Serializable;

/**
 * The issued card.
 */
public record IssueCardResponse(CampingCardDTO card) implements Serializable {
}
