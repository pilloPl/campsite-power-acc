package io.pillopl.webservice.console.power;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * New capacity of a section in A; running supplies keep running, new ones must fit.
 */
public record ChangeSectionCapacityRequest(Long sectionId, BigDecimal maxCurrentA) implements Serializable {
}
