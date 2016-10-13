package io.pillopl.group.attribute;

import io.pillopl.issuer.IssuerPartner;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WhitelistFilterTest {

    @Test
    void filterWithoutPartnerTargetsEveryPartner() {
        WhitelistFilter filter = new WhitelistFilter(false, WhitelistFilterType.IMPORT);

        assertThat(filter.isTargeting(partner(1L, "SEA CLUB"))).isTrue();
        assertThat(filter.isTargeting(null)).isTrue();
    }

    @Test
    void filterWithPartnerTargetsThatPartnerOnly() {
        WhitelistFilter filter = new WhitelistFilter(false, WhitelistFilterType.EXPORT);
        filter.setIssuerPartner(partner(1L, "SEA CLUB"));

        assertThat(filter.isTargeting(partner(1L, "SEA CLUB"))).isTrue();
        assertThat(filter.isTargeting(partner(2L, "MOUNTAIN CLUB"))).isFalse();
        assertThat(filter.isTargeting(null)).isFalse();
    }

    @Test
    void filterForAnUnsavedPartnerTargetsNobody() {
        WhitelistFilter filter = new WhitelistFilter(true, WhitelistFilterType.IMPORT_EXPORT);
        filter.setIssuerPartner(partner(null, "SEA CLUB"));

        assertThat(filter.isTargeting(partner(null, "SEA CLUB"))).isFalse();
    }

    private static IssuerPartner partner(Long id, String name) {
        IssuerPartner partner = new IssuerPartner(name, null, IssuerPartner.InterfaceVersion.V1, "/exchange");
        partner.setId(id);
        return partner;
    }
}
