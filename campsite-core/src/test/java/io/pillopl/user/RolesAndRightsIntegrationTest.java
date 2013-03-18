package io.pillopl.user;

import io.pillopl.group.GroupType;
import io.pillopl.persistence.dao.SecurityGroupDao;
import io.pillopl.persistence.dao.UserroleDao;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class RolesAndRightsIntegrationTest extends IntegrationTest {

    @Autowired UserroleDao userroleDao;
    @Autowired SecurityGroupDao securityGroupDao;

    @Test
    void everyRoleTheCodeNamesIsSeeded() {
        assertThat(Arrays.stream(UserroleName.values()).map(n -> userroleDao.findByName(n.name())))
                .doesNotContainNull();
    }

    @Test
    void onlyAffiliateOwnersAndSeasonalResidentsAndTheirSupportAreSelfService() {
        assertThat(userroleDao.findAllSelfServiceRoles()).extracting(Userrole::getName).containsExactly(
                "AFFILIATE_OWNER", "SEASONAL_RESIDENT", "AFFILIATE_PRICE_REPORTING", "SELF_SERVICE_USER",
                "SELF_SERVICE_SUPPORT");
    }

    @Test
    void receptionChecksOutStaysButDoesNotCommissionPedestals() {
        Userrole reception = userroleDao.findByName("RECEPTION");

        assertThat(reception.hasRight("STAY_CHECKOUT")).isTrue();
        assertThat(reception.hasRight("PEDESTAL_COMMISSION")).isFalse();
        assertThat(userroleDao.findByName("SERVICE_TECHNICIAN").hasRight("PEDESTAL_COMMISSION")).isTrue();
    }

    @Test
    void seasonalResidentMayNotHangInTheSiteTree() {
        Userrole resident = userroleDao.findByName("SEASONAL_RESIDENT");

        assertThat(resident.allowsGroupType(GroupType.ISSUER)).isTrue();
        assertThat(resident.allowsGroupType(GroupType.SITE)).isFalse();
    }

    @Test
    void campsiteManagementBundlesReceptionAndManagerRoles() {
        assertThat(securityGroupDao.findByName("CAMPSITE MANAGEMENT").getRoles()).extracting(Userrole::getName)
                .containsExactlyInAnyOrder("RECEPTION", "CAMPSITE_MANAGER");
    }
}
