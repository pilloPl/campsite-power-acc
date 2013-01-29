package io.pillopl.group.attribute;

import io.pillopl.persistence.dao.ExportCampsiteNameDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class ExportCampsiteNameIntegrationTest extends GroupAttributeIntegrationTestBase {

    @Autowired ExportCampsiteNameService exportCampsiteNameService;
    @Autowired ExportCampsiteNameDao exportCampsiteNameDao;

    @Test
    void exportNameOfTheNearestGroupIsUsed() {
        exportCampsiteNameDao.saveAndFlush(on(hrSites, new ExportCampsiteName("Network")));
        exportCampsiteNameDao.saveAndFlush(on(campsite, new ExportCampsiteName("Bay")));

        assertThat(exportCampsiteNameService.getExportCampsiteName(section.getId()).getName()).isEqualTo("Bay");
        assertThat(exportCampsiteNameService.getExportCampsiteName(hrSites.getId()).getName()).isEqualTo("Network");
    }

    @Test
    void groupWithoutExportNameOnItsLineHasNone() {
        assertThat(exportCampsiteNameService.getExportCampsiteName(groupService.fetchSiteGroup("IT SITES").getId()))
                .isNull();
    }
}
