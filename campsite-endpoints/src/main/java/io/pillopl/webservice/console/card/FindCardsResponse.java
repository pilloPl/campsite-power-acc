package io.pillopl.webservice.console.card;

import io.pillopl.dto.card.CampingCardDTO;

import java.io.Serializable;
import java.util.List;

/**
 * The cards found in the scope of the caller.
 */
public record FindCardsResponse(List<CampingCardDTO> cards) implements Serializable {
}
