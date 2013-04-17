package io.pillopl.group.attribute;

import io.pillopl.group.Group;
import io.pillopl.persistence.dao.IssuerTenantDao;
import io.pillopl.persistence.dao.SiteTenantDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TenantAttributeIntegrationTest extends GroupAttributeIntegrationTestBase {

    @Autowired SiteTenantService siteTenantService;
    @Autowired IssuerTenantService issuerTenantService;
    @Autowired SiteTenantDao siteTenantDao;
    @Autowired IssuerTenantDao issuerTenantDao;

    @Test
    void nearestSiteTenantWinsOverTheTenantOfTheMarket() {
        siteTenantDao.saveAndFlush(on(hrSites, new SiteTenant("Network operator HR")));
        siteTenantDao.saveAndFlush(on(campsite, new SiteTenant("Bay Camping d.o.o.")));

        assertThat(siteTenantService.findSiteTenantNameFor(section)).isEqualTo("Bay Camping d.o.o.");
        assertThat(siteTenantService.findSiteTenantNameFor(hrSites)).isEqualTo("Network operator HR");
    }

    @Test
    void siteTenantNameFallsBackToTheDisplayNameOfTheGroup() {
        assertThat(siteTenantService.findSiteTenantNameFor(section)).isEqualTo("Bay beach");
        assertThat(siteTenantService.findSiteTenantNameFor((io.pillopl.group.Group) null)).isEqualTo("N/A");
        assertThat(siteTenantService.findFor(club)).isNull();
    }

    @Test
    void recursiveTenantSqlAgreesWithTheJavaResolution() {
        siteTenantDao.saveAndFlush(on(hrSites, new SiteTenant("Network operator HR")));
        siteTenantDao.saveAndFlush(on(campsite, new SiteTenant("Bay Camping d.o.o.")));

        for (Group group : List.of(section, campsite, hrSites)) {
            List<Long> line = groupService.fetchParentLineIds(group.getId(), true);
            SiteTenant byJava = GroupAttributeHelper.findNearest(line, siteTenantDao.findByGroupIds(line));
            assertThat(siteTenantService.findFor(group)).isEqualTo(byJava);
        }
    }

    @Test
    void nearestIssuerTenantWinsAndFallsBackToTheGroupName() {
        assertThat(issuerTenantService.findIssuerTenantNameFor(clubJunior)).isEqualTo("CARAVAN CLUB JUNIOR");

        issuerTenantDao.saveAndFlush(on(club, new IssuerTenant("Caravan Club Association")));

        assertThat(issuerTenantService.findIssuerTenantNameFor(clubJunior)).isEqualTo("Caravan Club Association");
        assertThat(issuerTenantService.findIssuerTenantNameFor(null)).isEqualTo("N/A");
        assertThat(issuerTenantService.findFor(section)).isNull();
    }
}
