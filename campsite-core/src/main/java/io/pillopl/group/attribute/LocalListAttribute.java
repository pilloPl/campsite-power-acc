package io.pillopl.group.attribute;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Whether pedestals of a SITE subtree keep a local card list in their controller, and what goes into it.
 */
@Entity
@Table(name = "GRP_ATTR_LOCAL_LIST")
@Getter
@Setter
public class LocalListAttribute extends GroupAttribute {

    @Column(name = "LOCAL_LIST_ALLOWED", nullable = false)
    private boolean localListAllowed;

    @Column(name = "INCLUDE_CARDS", nullable = false)
    private boolean includeCards;

    @Column(name = "INCLUDE_HOUSE_CARDS", nullable = false)
    private boolean includeHouseCards;

    public LocalListAttribute() {
    }

    public LocalListAttribute(boolean localListAllowed, boolean includeCards, boolean includeHouseCards) {
        this.localListAllowed = localListAllowed;
        this.includeCards = includeCards;
        this.includeHouseCards = includeHouseCards;
    }
}
