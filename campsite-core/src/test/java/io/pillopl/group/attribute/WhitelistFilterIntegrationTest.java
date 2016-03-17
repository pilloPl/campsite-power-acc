package io.pillopl.group.attribute;

import io.pillopl.issuer.IssuerPartner;
import io.pillopl.persistence.dao.IssuerPartnerDao;
import io.pillopl.persistence.dao.WhitelistFilterDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class WhitelistFilterIntegrationTest extends GroupAttributeIntegrationTestBase {

    @Autowired WhitelistFilterService whitelistFilterService;
    @Autowired WhitelistFilterDao whitelistFilterDao;
    @Autowired IssuerPartnerDao issuerPartnerDao;

    @Test
    void whitelistFilterDecidesPerDirectionByTheNearestFilterCoveringIt() {
        whitelistFilterDao.saveAndFlush(on(club, new WhitelistFilter(false, WhitelistFilterType.IMPORT_EXPORT)));
        whitelistFilterDao.saveAndFlush(on(clubJunior, new WhitelistFilter(true, WhitelistFilterType.EXPORT)));

        assertThat(whitelistFilterService.isSynchronizableForExport(clubJunior.getId())).isTrue();
        assertThat(whitelistFilterService.isSynchronizableForImport(clubJunior.getId())).isFalse();
        assertThat(whitelistFilterService.isSynchronizableForImport(club.getId())).isFalse();
    }

    @Test
    void issuerGroupWithoutWhitelistFilterIsSynchronised() {
        assertThat(whitelistFilterService.isSynchronizableForImport(clubJunior.getId())).isTrue();
        assertThat(whitelistFilterService.isSynchronizableForExport(clubJunior.getId())).isTrue();
    }

    @Test
    void filterWithATargetPartnerAppliesToTheExchangeWithThatPartnerOnly() {
        IssuerPartner federation = partner("FILTER FEDERATION");
        IssuerPartner alliance = partner("FILTER ALLIANCE");
        WhitelistFilter filter = new WhitelistFilter(false, WhitelistFilterType.IMPORT_EXPORT);
        filter.setIssuerPartner(federation);
        whitelistFilterDao.saveAndFlush(on(club, filter));

        assertThat(whitelistFilterService.isSynchronizableForImport(clubJunior.getId(), federation)).isFalse();
        assertThat(whitelistFilterService.isSynchronizableForExport(clubJunior.getId(), federation)).isFalse();
        assertThat(whitelistFilterService.isSynchronizableForImport(clubJunior.getId(), alliance)).isTrue();
        assertThat(whitelistFilterService.isSynchronizableForExport(clubJunior.getId(), alliance)).isTrue();
    }

    @Test
    void nearestFilterForThePartnerWinsOverAFilterForEveryPartnerAbove() {
        IssuerPartner federation = partner("FILTER FEDERATION");
        whitelistFilterDao.saveAndFlush(on(club, new WhitelistFilter(false, WhitelistFilterType.IMPORT)));
        WhitelistFilter forFederation = new WhitelistFilter(true, WhitelistFilterType.IMPORT);
        forFederation.setIssuerPartner(federation);
        whitelistFilterDao.saveAndFlush(on(clubJunior, forFederation));

        assertThat(whitelistFilterService.isSynchronizableForImport(clubJunior.getId(), federation)).isTrue();
        assertThat(whitelistFilterService.isSynchronizableForImport(club.getId(), federation)).isFalse();
    }

    private IssuerPartner partner(String name) {
        return issuerPartnerDao.saveAndFlush(new IssuerPartner(name, club, IssuerPartner.InterfaceVersion.V2,
                "http://issuer.test/api"));
    }
}
