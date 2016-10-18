package io.pillopl.webservice.console.issuer;

import java.io.Serializable;

/**
 * Send a camping card to the issuer partners again with the next card export.
 */
public record ResyncCardRequest(Long cardId) implements Serializable {
}
