package io.pillopl.group.attribute;

/**
 * Direction of the issuer exchange a {@link WhitelistFilter} applies to.
 */
public enum WhitelistFilterType {
    IMPORT_EXPORT, IMPORT, EXPORT;

    public static boolean isImportFilter(WhitelistFilterType whitelistFilterType) {
        if (whitelistFilterType == null) {
            return false;
        }
        return IMPORT_EXPORT == whitelistFilterType || IMPORT == whitelistFilterType;
    }

    public static boolean isExportFilter(WhitelistFilterType whitelistFilterType) {
        if (whitelistFilterType == null) {
            return false;
        }
        return IMPORT_EXPORT == whitelistFilterType || EXPORT == whitelistFilterType;
    }

    public static WhitelistFilterType fromString(String whitelistFilterType) {
        if (whitelistFilterType == null) {
            return IMPORT_EXPORT;
        }
        try {
            return WhitelistFilterType.valueOf(whitelistFilterType.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown whitelist filter type '" + whitelistFilterType + "'", e);
        }
    }
}
