package io.pillopl.group.attribute;

import io.pillopl.persistence.dao.WhitelistFilterDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class WhitelistFilterIntegrationTest extends GroupAttributeIntegrationTestBase {

    @Autowired WhitelistFilterService whitelistFilterService;
    @Autowired WhitelistFilterDao whitelistFilterDao;

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
}
