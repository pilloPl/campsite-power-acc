package io.pillopl.webservice.console.card;

import io.pillopl.dto.card.LocalCardListDTO;

import java.io.Serializable;

/**
 * The list with capacity and the cards left out.
 */
public record GetLocalCardListResponse(LocalCardListDTO localCardList) implements Serializable {
}
