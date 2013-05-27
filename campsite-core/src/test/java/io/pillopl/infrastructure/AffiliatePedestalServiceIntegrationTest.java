package io.pillopl.infrastructure;

import io.pillopl.config.SystemPropertyKey;
import io.pillopl.config.SystemPropertyService;
import io.pillopl.event.Event;
import io.pillopl.event.EventService;
import io.pillopl.event.EventType;
import io.pillopl.event.PedestalCommissionedEvent;
import io.pillopl.event.PedestalDecommissionedEvent;
import io.pillopl.group.AccessDeniedException;
import io.pillopl.group.AccessMode;
import io.pillopl.group.AffiliateGroupService;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.Market;
import io.pillopl.group.attribute.SupplierMarkerService;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.test.IntegrationTest;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserinfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@Import(AffiliatePedestalServiceIntegrationTest.RecordingNotifierConfiguration.class)
class AffiliatePedestalServiceIntegrationTest extends IntegrationTest {

    static final List<String> NOTIFIED = new ArrayList<>();

    @TestConfiguration(proxyBeanMethods = false)
    static class RecordingNotifierConfiguration {
        @Bean
        @org.springframework.context.annotation.Primary
        DecommissioningNotifier recordingNotifier() {
            return (locale, pedestalCode, email) -> NOTIFIED.add(pedestalCode + " -> " + email);
        }
    }

    @Autowired AffiliatePedestalService affiliatePedestalService;
    @Autowired CommissioningService commissioningService;
    @Autowired SupplierMarkerService supplierMarkerService;
    @Autowired AffiliateGroupService affiliateGroupService;
    @Autowired UserinfoService userinfoService;
    @Autowired GroupService groupService;
    @Autowired UserinfoDao userinfoDao;
    @Autowired SystemPropertyService systemPropertyService;
    @Autowired InfrastructureFixture fixture;
    @Autowired EventService eventService;
    @Autowired io.pillopl.persistence.dao.AuditDao auditDao;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @org.springframework.test.context.transaction.AfterTransaction
    void removeAuditRows() {
        // audit rows are written in their own transaction and survive the rollback of the test
        jdbcTemplate.update("DELETE FROM AUDIT WHERE ACTION_TYPE LIKE 'PEDESTAL_%'");
    }

    Group stock;
    Userinfo installer, marko;

    @BeforeEach
    void setUp() {
        NOTIFIED.clear();
        stock = supplierMarkerService.createSupplierStock(InfrastructureFixture.MANUFACTURER, Market.HR);
        Group hrStaff = groupService.fetchStaffGroup("HR STAFF");
        installer = user("ivo", hrStaff);
        installer.getGroups().add(hrStaff);
        installer.getGroups().add(groupService.fetchSiteGroup("HR SITES"));
        marko = user("marko", groupService.fetchStaffGroup("HR SELF SERVICE"));
        userinfoService.setupAffiliateOwner(marko);
    }

    @Test
    void pedestalsInTheSupplierStockAreListedAndNotCommissioned() {
        Pedestal pedestal = fixture.pedestal("ACME-0001", stock, 2);

        assertThat(affiliatePedestalService.supplierStock("ivo")).extracting(SupplierStockEntry::pedestalCode)
                .containsExactly("ACME-0001");
        assertThat(affiliatePedestalService.isCommissioned(pedestal.getId())).isFalse();
        assertThat(affiliatePedestalService.findNotCommissioned()).containsExactly(pedestal);
        assertThat(affiliatePedestalService.isCommissioned(999_999L)).isNull();
    }

    @Test
    void commissioningMountsThePedestalsOnTheAffiliateCampsiteWithSocketCodesAndAccessMode() {
        Pedestal pedestal = fixture.pedestal("ACME-0001", stock, 2);
        Commissioning commissioning = commissioningService.create(commissioning(AccessMode.NETWORK_ACCESS, true));

        List<Pedestal> failed = affiliatePedestalService.commission(List.of(pedestal), "ivo", commissioning.getToken());

        Group affiliate = affiliateGroupService.findAffiliateSiteGroup("marko");
        assertThat(failed).isEmpty();
        assertThat(pedestal.getGroup()).isEqualTo(affiliate);
        assertThat(pedestal.isBillable()).isTrue();
        assertThat(pedestal.getLocation().getCampsiteCode()).isEqualTo("HRMARKO");
        assertThat(pedestal.getSockets()).extracting(Socket::getSocketCode)
                .containsExactly("HRMARKO*ACME-0001*1", "HRMARKO*ACME-0001*2");
        assertThat(affiliateGroupService.checkAccessMode(affiliate.getId())).isEqualTo(AccessMode.NETWORK_ACCESS);
        assertThat(pedestal.isListedInApp()).isTrue();
        assertThat(commissioning.isDeleted()).isTrue();
        assertThat(affiliatePedestalService.isCommissioned(pedestal.getId())).isTrue();
    }

    @Test
    void unknownTokenCommissionsNothing() {
        Pedestal pedestal = fixture.pedestal("ACME-0001", stock, 1);

        assertThat(affiliatePedestalService.commission(List.of(pedestal), "ivo", "nope")).containsExactly(pedestal);
        assertThat(pedestal.getGroup()).isEqualTo(stock);
    }

    @Test
    void directCommissioningRefusesAPedestalAlreadyCommissioned() {
        fixture.pedestal("ACME-0001", stock, 1);
        affiliatePedestalService.commission("ACME-0001", "ivo", "marko", location(), false,
                AccessMode.HOUSE_CARDS_ONLY);

        assertThatThrownBy(() -> affiliatePedestalService.commission("ACME-0001", "ivo", "marko", location(), false,
                AccessMode.HOUSE_CARDS_ONLY)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void installerOfAnotherMarketMayNotCommissionForTheOwner() {
        fixture.pedestal("ACME-0001", stock, 1);
        user("luca", groupService.fetchStaffGroup("IT STAFF")).getGroups()
                .add(groupService.fetchStaffGroup("IT STAFF"));

        assertThatThrownBy(() -> affiliatePedestalService.commission("ACME-0001", "luca", "marko", location(), false,
                AccessMode.HOUSE_CARDS_ONLY)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void decommissionedPedestalGoesBackToTheSupplierWithoutCodesAndTheOwnerIsTold() {
        Pedestal pedestal = fixture.pedestal("ACME-0001", stock, 2);
        affiliatePedestalService.commission("ACME-0001", "ivo", "marko", location(), true, AccessMode.NETWORK_ACCESS);
        pedestal.getSockets().get(0).setPitchNo("A-1");

        affiliatePedestalService.decommission(pedestal.getId(), marko.getId());

        assertThat(pedestal.getGroup()).isEqualTo(stock);
        assertThat(pedestal.getLocation().isUnknown()).isTrue();
        assertThat(pedestal.isBillable()).isFalse();
        assertThat(pedestal.getSockets()).allMatch(s -> s.getSocketCode() == null && s.getPitchNo() == null);
        assertThat(NOTIFIED).containsExactly("ACME-0001 -> marko@example.org");
        assertThat(jdbcTemplate.queryForList("SELECT ACTION_TYPE FROM AUDIT WHERE ACTION_NAME = 'ACME-0001'",
                String.class)).contains("PEDESTAL_COMMISSIONING", "PEDESTAL_DECOMMISSIONING");
    }

    @Test
    void commissioningAndDecommissioningAreAnnouncedAsEvents() {
        eventService.registerQueue("test-commissioned", EventType.PEDESTAL_COMMISSIONED);
        eventService.registerQueue("test-decommissioned", EventType.PEDESTAL_DECOMMISSIONED);
        try {
            Pedestal pedestal = fixture.pedestal("ACME-0001", stock, 1);
            affiliatePedestalService.commission("ACME-0001", "ivo", "marko", location(), true,
                    AccessMode.NETWORK_ACCESS);
            affiliatePedestalService.decommission(pedestal.getId(), marko.getId());

            Event commissioned = eventService.getQueueAndRegisterListener("test-commissioned").poll();
            Event decommissioned = eventService.getQueueAndRegisterListener("test-decommissioned").poll();
            assertThat(PedestalCommissionedEvent.getPedestalId(commissioned)).isEqualTo(pedestal.getId());
            assertThat(PedestalCommissionedEvent.getGroupId(commissioned))
                    .isEqualTo(affiliateGroupService.findAffiliateSiteGroup("marko").getId());
            assertThat(PedestalDecommissionedEvent.getPedestalId(decommissioned)).isEqualTo(pedestal.getId());
        } finally {
            eventService.unregisterQueue("test-commissioned", EventType.PEDESTAL_COMMISSIONED);
            eventService.unregisterQueue("test-decommissioned", EventType.PEDESTAL_DECOMMISSIONED);
        }
    }

    @Test
    void ownerOfAnExcludedMarketIsNotTold() {
        systemPropertyService.setProperty(SystemPropertyKey.DecommissioningMailExcludedMarkets, "IT, HR", null);
        Pedestal pedestal = fixture.pedestal("ACME-0001", stock, 1);
        affiliatePedestalService.commission("ACME-0001", "ivo", "marko", location(), true, AccessMode.NETWORK_ACCESS);

        try {
            affiliatePedestalService.decommission(pedestal.getId(), marko.getId());

            assertThat(NOTIFIED).isEmpty();
        } finally {
            systemPropertyService.setProperty(SystemPropertyKey.DecommissioningMailExcludedMarkets, "", null);
        }
    }

    @Test
    void userWithoutAccessMayNotDecommission() {
        Pedestal pedestal = fixture.pedestal("ACME-0001", stock, 1);
        affiliatePedestalService.commission("ACME-0001", "ivo", "marko", location(), true, AccessMode.NETWORK_ACCESS);
        Userinfo stranger = user("stranger", groupService.fetchStaffGroup("HR SELF SERVICE"));

        assertThatThrownBy(() -> affiliatePedestalService.decommission(pedestal.getId(), stranger.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Commissioning commissioning(AccessMode accessMode, boolean listedInApp) {
        Commissioning commissioning = new Commissioning();
        commissioning.setAffiliateOwner(marko);
        commissioning.setLocation(location());
        commissioning.setAccessMode(accessMode);
        commissioning.setListedInApp(listedInApp);
        return commissioning;
    }

    private static Location location() {
        Location location = new Location("Pula", "52100", "Lungomare", "3");
        location.setName("Marko's campsite");
        location.setCampsiteCode("HRMARKO");
        location.setCountryIso("hr");
        return location;
    }

    private Userinfo user(String username, Group ownGroup) {
        Userinfo user = new Userinfo(username, username, username + "@example.org");
        user.setGroup(ownGroup);
        return userinfoDao.saveAndFlush(user);
    }
}
