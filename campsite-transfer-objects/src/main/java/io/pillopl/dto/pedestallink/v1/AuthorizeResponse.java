package io.pillopl.dto.pedestallink.v1;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Link v1: answer to an authorize. Status ACCEPTED, BLOCKED, EXPIRED, INVALID or OCCUPIED; the controller may cache
 * the card until the expiry date and show the guest name.
 */
public record AuthorizeResponse(String status, LocalDate expiryDate, String guestName) implements Serializable {
}
