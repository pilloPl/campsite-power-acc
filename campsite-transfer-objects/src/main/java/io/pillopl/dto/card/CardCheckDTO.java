package io.pillopl.dto.card;

import java.io.Serializable;

/**
 * What a pedestal would answer to the card at the socket now: the validation status, whether a house card was
 * recognised, and the card and guest found.
 */
public record CardCheckDTO(String socket, String cardRfid, String validationStatus, boolean houseCard,
                           Long cardId, String guestName) implements Serializable {
}
