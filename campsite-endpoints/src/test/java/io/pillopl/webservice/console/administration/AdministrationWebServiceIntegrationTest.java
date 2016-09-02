package io.pillopl.webservice.console.administration;

import io.pillopl.config.SystemPropertyKey;
import io.pillopl.user.UserroleName;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdministrationWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.ADMINISTRATION;

    @org.springframework.beans.factory.annotation.Autowired
    io.pillopl.config.SystemPropertyService configuration;

    @BeforeEach
    void setUp() {
        console.hrAdmin("admin");
        console.user("reception", UserroleName.RECEPTION, console.staff("HR RECEPTION"), console.staff("HR RECEPTION"));
    }

    @Test
    void configurationChangeIsStoredAndAudited() throws Exception {
        call("admin", PATH + "/setSystemProperty", new SetSystemPropertyRequest("StaleSessionTimeoutInHours", "36",
                "close stale sessions after"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.property.value").value("36"));

        call("admin", PATH + "/getSystemProperties", new GetSystemPropertiesRequest())
                .andExpect(jsonPath("$.properties", hasSize(SystemPropertyKey.values().length)))
                .andExpect(jsonPath("$.properties[?(@.key == 'StaleSessionTimeoutInHours')].value", hasItem("36")));
        call("admin", PATH + "/findAuditEntries", new FindAuditEntriesRequest("StaleSessionTimeoutInHours",
                "CONFIG_CHANGE", null, "functional", null, null, null, "actionDate", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries", hasSize(1)))
                .andExpect(jsonPath("$.entries[0].userId").value("admin"));
    }

    @Test
    void unknownKeysAndUnreadableDatesAreWrongRequests() throws Exception {
        call("admin", PATH + "/setSystemProperty", new SetSystemPropertyRequest("stalesessiontimeoutinhours", "36",
                null))
                .andExpect(status().isBadRequest());
        call("admin", PATH + "/findAuditEntries", new FindAuditEntriesRequest(null, null, null, null, "01.05.2016",
                null, null, null, null))
                .andExpect(status().isBadRequest());
        call("admin", PATH + "/findAuditEntries", new FindAuditEntriesRequest(null, null, null, null, "2016-05-02",
                "2016-05-01", null, null, null))
                .andExpect(status().isBadRequest());
    }

    @Test
    void receptionSeesNeitherAuditNorConfigurationNorRoles() throws Exception {
        call("reception", PATH + "/findAuditEntries", new FindAuditEntriesRequest(null, null, null, null, null, null,
                null, null, null))
                .andExpect(status().isForbidden());
        call("reception", PATH + "/getSystemProperties", new GetSystemPropertiesRequest())
                .andExpect(status().isForbidden());
        call("reception", PATH + "/findRoles", new FindRolesRequest()).andExpect(status().isForbidden());
        call("reception", PATH + "/refreshUserSummary", new RefreshUserSummaryRequest())
                .andExpect(status().isForbidden());
    }

    @Test
    void rightsMatrixListsTheRolesWithTheirRights() throws Exception {
        call("admin", PATH + "/findRoles", new FindRolesRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0].name").value("RECEPTION"))
                .andExpect(jsonPath("$.roles[0].rights", hasItem("STAY_CHECKOUT")))
                .andExpect(jsonPath("$.roles[?(@.name == 'AFFILIATE_OWNER')].selfService", hasItem(true)));
    }

    @Test
    void fullRightsMatrixHasAllRightsRolesAndSecurityGroups() throws Exception {
        call("admin", PATH + "/getRightsMatrix", new GetRightsMatrixRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rightsMatrix.rights[0]").value("PEDESTAL_VIEW"))
                .andExpect(jsonPath("$.rightsMatrix.rights", hasItem("BILLING_EDIT")))
                .andExpect(jsonPath("$.rightsMatrix.roles[?(@.name == 'BACK_OFFICE')].rights[*]",
                        hasItem("BILLING_EDIT")))
                .andExpect(jsonPath("$.rightsMatrix.securityGroups[?(@.name == 'CAMPSITE MANAGEMENT')].roles[*]",
                        hasItem("CAMPSITE_MANAGER")));
        call("reception", PATH + "/getRightsMatrix", new GetRightsMatrixRequest()).andExpect(status().isForbidden());
    }

    @Test
    void configurationChangedInTheDatabaseTakesEffectAfterTheReload() throws Exception {
        jdbcTemplate.update("UPDATE SYSTEM_PROPERTY SET PROPERTY_VALUE = '48' "
                + "WHERE PROPERTY_KEY = 'StaleSessionTimeoutInHours'");

        call("admin", PATH + "/reloadConfiguration", new ReloadConfigurationRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.properties[?(@.key == 'StaleSessionTimeoutInHours')].value", hasItem("48")));
        org.assertj.core.api.Assertions.assertThat(configuration.getStaleSessionTimeoutInHours())
                .isEqualTo(48);
        call("reception", PATH + "/reloadConfiguration", new ReloadConfigurationRequest())
                .andExpect(status().isForbidden());
    }
}
