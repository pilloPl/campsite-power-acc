package io.pillopl.dto.infrastructure;

import java.io.Serializable;

/**
 * A pedestal waiting in the supplier stock.
 */
public record SupplierStockEntryDTO(Long id, String pedestalCode, String pedestalType, int numberOfSockets,
                                    String modemIccid, String serialNumber) implements Serializable {
}
