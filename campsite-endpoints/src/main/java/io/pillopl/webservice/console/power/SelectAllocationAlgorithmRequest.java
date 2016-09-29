package io.pillopl.webservice.console.power;

import java.io.Serializable;

/**
 * The allocation algorithm bean (one of availableAlgorithms) a section checks new supplies with.
 */
public record SelectAllocationAlgorithmRequest(Long sectionId, String algorithmBeanName) implements Serializable {
}
