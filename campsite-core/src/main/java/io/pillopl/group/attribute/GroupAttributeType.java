package io.pillopl.group.attribute;

/**
 * Catalogue of group attributes and their tables. All of them resolve by "nearest ancestor-or-self wins".
 * Attributes whose model belongs to another module are listed with the module that implements them.
 */
public enum GroupAttributeType {

    SITE_TENANT("GRP_ATTR_SITE_TENANT", true, "organisation"),
    ISSUER_TENANT("GRP_ATTR_ISSUER_TENANT", true, "organisation"),
    LOCAL_LIST("GRP_ATTR_LOCAL_LIST", true, "organisation"),
    NOTIFICATION("GRP_ATTR_NOTIFICATION", true, "organisation"),
    COMMUNICATION_LOG("GRP_ATTR_COMMLOG", true, "organisation"),
    WELCOME_CARD_ADOPTION("GRP_ATTR_WELCOME_CARD_ADOPTION", true, "organisation"),
    EXPORT_CAMPSITE_NAME("GRP_ATTR_EXPORT_CAMPSITE_NAME", true, "organisation"),
    SUPPLIER_MARKER("GRP_ATTR_SUPPLIER", true, "organisation"),
    WHITELIST_FILTER("GRP_ATTR_WHITELIST_FILTER", true, "organisation"),
    /**
     * House cards (STAFF / RENTAL) of a campsite: implemented with the cards module.
     */
    HOUSE_CARD("HOUSECARD", false, "cards"),
    /**
     * Default tariff of a SITE subtree (replaces the "market to tariff" fallback): implemented with pricing.
     */
    DEFAULT_TARIFF("GRP_ATTR_DEFAULT_TARIFF", false, "pricing");

    private final String tableName;

    private final boolean implemented;

    private final String owningModule;

    GroupAttributeType(String tableName, boolean implemented, String owningModule) {
        this.tableName = tableName;
        this.implemented = implemented;
        this.owningModule = owningModule;
    }

    public String getTableName() {
        return tableName;
    }

    public boolean isImplemented() {
        return implemented;
    }

    public String getOwningModule() {
        return owningModule;
    }
}
