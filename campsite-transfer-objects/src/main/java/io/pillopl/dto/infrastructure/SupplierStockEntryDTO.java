package io.pillopl.dto.infrastructure;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A pedestal waiting in the supplier stock, with its last boot and last consumption record (null when not logged).
 */
public record SupplierStockEntryDTO(Long id, String pedestalCode, String pedestalType, int numberOfSockets,
                                    String modemIccid, String serialNumber, LocalDateTime lastBoot,
                                    LocalDateTime lastConsumptionRecord) implements Serializable {
}
