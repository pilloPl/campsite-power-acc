package io.pillopl.dto.pedestallink.v1;

import java.io.Serializable;

/**
 * Link v1: a card was held to the reader of the pedestal. With a socket number the guest wants supply at that
 * socket; without one the controller only asks whether the card is known.
 */
public record AuthorizeRequest(String authTag, Integer socketNo) implements Serializable {
}
