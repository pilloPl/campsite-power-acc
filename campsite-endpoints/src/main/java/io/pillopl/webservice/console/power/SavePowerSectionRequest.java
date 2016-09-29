package io.pillopl.webservice.console.power;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * A new section (sectionId null) or the new name, capacity and algorithm of a section.
 */
public record SavePowerSectionRequest(Long sectionId, String sectionName, BigDecimal maxCurrentA,
                                      String algorithmBeanName) implements Serializable {
}
