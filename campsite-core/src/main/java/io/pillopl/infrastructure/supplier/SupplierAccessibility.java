package io.pillopl.infrastructure.supplier;

/**
 * Accessibility column of the supplier's pedestal file.
 */
public enum SupplierAccessibility {
    PUBLIC_PAYING,
    PUBLIC_FREE,
    RESTRICTED,
    UNSPECIFIED;

    /**
     * @return UNSPECIFIED for blank or unknown values
     */
    public static SupplierAccessibility parse(String value) {
        if (value == null || value.isBlank()) {
            return UNSPECIFIED;
        }
        try {
            return valueOf(value.trim().toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return UNSPECIFIED;
        }
    }
}
