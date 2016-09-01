package io.pillopl.webservice.console.guest;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Stays overlapping [from, to] at one campsite (SITE group id) or at all campsites of the caller;
 * state OPEN, CHECKED_OUT or empty for all.
 */
public record FindStaysRequest(Long campsiteGroupId, String state, LocalDate from, LocalDate to) implements Serializable {
}
