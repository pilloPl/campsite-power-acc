package io.pillopl.group.attribute;

import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.persistence.dao.CommunicationLogAttributeDao;
import io.pillopl.persistence.dao.LocalListAttributeDao;
import io.pillopl.persistence.dao.SiteTenantDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Attributes asked for a pedestal are the attributes of the group the pedestal hangs in.
 */
class PedestalAttributeIntegrationTest extends GroupAttributeIntegrationTestBase {

    @Autowired InfrastructureFixture fixture;
    @Autowired SiteTenantService siteTenantService;
    @Autowired SiteTenantDao siteTenantDao;
    @Autowired CommunicationLogAttributeService communicationLogAttributeService;
    @Autowired CommunicationLogAttributeDao communicationLogAttributeDao;
    @Autowired LocalListAttributeService localListAttributeService;
    @Autowired LocalListAttributeDao localListAttributeDao;

    @Test
    void operatorOfThePedestalIsTheNearestSiteTenantOfItsSection() {
        siteTenantDao.saveAndFlush(on(campsite, new SiteTenant("Bay Camping d.o.o.")));
        Pedestal pedestal = fixture.pedestal("HR-BAY-P001", section, 1);

        assertThat(siteTenantService.findSiteTenantNameFor(pedestal)).isEqualTo("Bay Camping d.o.o.");
        assertThat(siteTenantService.findSiteTenantNameFor((Pedestal) null)).isEqualTo("N/A");
    }

    @Test
    void pedestalTrafficIsLoggedWhenItsGroupIsLoggable() {
        communicationLogAttributeDao.saveAndFlush(on(campsite, new CommunicationLogAttribute("bay", true)));
        Pedestal pedestal = fixture.pedestal("HR-BAY-P001", section, 1);

        assertThat(communicationLogAttributeService.isPedestalLoggable(pedestal.getId())).isTrue();
        assertThat(communicationLogAttributeService.isPedestalLoggable("HR-BAY-P001")).isTrue();
        assertThat(communicationLogAttributeService.isPedestalLoggable("UNKNOWN")).isFalse();
    }

    @Test
    void pedestalWithoutGroupIsNotLogged() {
        communicationLogAttributeDao.saveAndFlush(on(hrSites, new CommunicationLogAttribute("all HR", true)));
        Pedestal pedestal = fixture.pedestal("HR-BAY-P001", null, 1);

        assertThat(communicationLogAttributeService.isPedestalLoggable(pedestal.getId())).isFalse();
    }

    @Test
    void localCardListNeedsAControllerKeepingOneAndAGroupAllowingIt() {
        Pedestal pedestal = fixture.pedestal("HR-BAY-P001", section, 1);
        assertThat(localListAttributeService.isLocalListPedestal(pedestal)).isFalse();

        localListAttributeDao.saveAndFlush(on(campsite, new LocalListAttribute(true, true, true)));
        assertThat(localListAttributeService.isLocalListPedestal(pedestal)).isTrue();

        pedestal.getPedestalType().setLocalCardList(false);
        assertThat(localListAttributeService.isLocalListPedestal(pedestal)).isFalse();
    }
}
