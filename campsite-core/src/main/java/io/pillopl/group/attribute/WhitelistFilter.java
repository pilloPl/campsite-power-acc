package io.pillopl.group.attribute;

import io.pillopl.issuer.IssuerPartner;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Whether cards of an ISSUER subtree are synchronised with card issuer partners, per direction.
 * A filter with an issuer partner applies to the exchange with that partner only, without one to every partner.
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

    @ManyToOne
    @JoinColumn(name = "ISSUER_PARTNER_ID")
    private IssuerPartner issuerPartner;

    public WhitelistFilter() {
    }

    public WhitelistFilter(boolean synchronizable, WhitelistFilterType whitelistFilterType) {
        this.synchronizable = synchronizable;
        this.whitelistFilterType = whitelistFilterType;
    }

    /**
     * Does the filter apply to the exchange with the partner? A filter without a partner applies to all.
     */
    public boolean isTargeting(IssuerPartner partner) {
        if (issuerPartner == null) {
            return true;
        }
        return partner != null && issuerPartner.getId() != null && issuerPartner.getId().equals(partner.getId());
    }
}
