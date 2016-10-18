package io.pillopl.webservice.console.issuer;

import io.pillopl.dto.card.CampingCardDTO;

import java.io.Serializable;

/**
 * The card with the sync state the next export sends.
 */
public record ResyncCardResponse(CampingCardDTO card) implements Serializable {
}
