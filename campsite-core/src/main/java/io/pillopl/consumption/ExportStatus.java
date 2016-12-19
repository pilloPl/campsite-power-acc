package io.pillopl.consumption;

/**
 * Where a consumption record stands in the export to the ERP and to the card issuers. NEW records are exported;
 * REVIEW records wait for a person (suspect values, failed pricing) and are not exported until confirmed.
 */
public enum ExportStatus {
    NEW,
    TRANSFERRED,
    PROCESSED,
    WORK,
    ERROR,
    IGNORED,
    DUPLICATE,
    REVIEW;

    public boolean isExportable() {
        return this == NEW;
    }

    /**
     * The price of the record may still change: its pricing failed (REVIEW) or it waits for the export (NEW).
     */
    public boolean mayBePricedAgain() {
        return this == REVIEW || this == NEW;
    }
}
