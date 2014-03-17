package io.pillopl.webservice.console.card;

import java.io.Serializable;

/**
 * Blocks (DEACTIVATED) or unblocks (ACTIVATED) a card.
 */
public record ChangeCardStatusRequest(String authUsername, Long cardId, String activation) implements Serializable {
}
