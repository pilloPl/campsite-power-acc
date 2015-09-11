package io.pillopl.infrastructure;

import java.time.LocalDateTime;

/**
 * A pedestal waiting in the supplier stock, as listed for the supplier and the installers, with its last boot and
 * last consumption record from the communication log (null when not logged).
 */
public record SupplierStockEntry(Long id, String pedestalCode, String pedestalType, int numberOfSockets,
                                 String modemIccid, String serialNumber, LocalDateTime lastBoot,
                                 LocalDateTime lastConsumptionRecord) {
}
