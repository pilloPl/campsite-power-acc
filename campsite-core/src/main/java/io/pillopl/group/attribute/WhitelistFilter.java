package io.pillopl.group.attribute;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Whether cards of an ISSUER subtree are synchronised with card issuer partners, per direction.
 * The partner the filter targets is added with the issuer exchange.
 */
@Entity
@Table(name = "GRP_ATTR_WHITELIST_FILTER")
@Getter
@Setter
public class WhitelistFilter extends GroupAttribute {

    @Column(name = "SYNCHRONIZABLE", nullable = false)
    private boolean synchronizable;

    @Enumerated(EnumType.STRING)
    @Column(name = "WHITELIST_FILTER_TYPE", nullable = false, length = 32)
    private WhitelistFilterType whitelistFilterType = WhitelistFilterType.IMPORT_EXPORT;

    public WhitelistFilter() {
    }

    public WhitelistFilter(boolean synchronizable, WhitelistFilterType whitelistFilterType) {
        this.synchronizable = synchronizable;
        this.whitelistFilterType = whitelistFilterType;
    }
}
