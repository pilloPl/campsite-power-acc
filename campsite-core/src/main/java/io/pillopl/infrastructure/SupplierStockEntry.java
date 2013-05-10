package io.pillopl.infrastructure;

/**
 * A pedestal waiting in the supplier stock, as listed for the supplier and the installers.
 */
public record SupplierStockEntry(Long id, String pedestalCode, String pedestalType, int numberOfSockets,
                                 String modemIccid, String serialNumber) {
}
