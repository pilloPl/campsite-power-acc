package io.pillopl.webservice.console.power;

import java.io.Serializable;

/**
 * Hourly load of a section in [from, to] (dates or date-times; without both: the last 24 hours; at most 31 days).
 */
public record GetSectionLoadRequest(Long sectionId, String from, String to) implements Serializable {
}
