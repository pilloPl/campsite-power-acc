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

    @BeforeEach
    void setUp() {
        console.hrAdmin("admin");
        console.user("reception", UserroleName.RECEPTION, console.staff("HR RECEPTION"), console.staff("HR RECEPTION"));
    }

    @Test
    void configurationChangeIsStoredAndAudited() throws Exception {
        call(PATH + "/setSystemProperty", new SetSystemPropertyRequest("admin", "StaleSessionTimeoutInHours", "36",
                "close stale sessions after"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.property.value").value("36"));

        call(PATH + "/getSystemProperties", new GetSystemPropertiesRequest("admin"))
                .andExpect(jsonPath("$.properties", hasSize(SystemPropertyKey.values().length)))
                .andExpect(jsonPath("$.properties[?(@.key == 'StaleSessionTimeoutInHours')].value", hasItem("36")));
        call(PATH + "/findAuditEntries", new FindAuditEntriesRequest("admin", "StaleSessionTimeoutInHours",
                "CONFIG_CHANGE", null, "functional", null, null, null, "actionDate", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries", hasSize(1)))
                .andExpect(jsonPath("$.entries[0].userId").value("admin"));
    }

    @Test
    void unknownKeysAndUnreadableDatesAreWrongRequests() throws Exception {
        call(PATH + "/setSystemProperty", new SetSystemPropertyRequest("admin", "stalesessiontimeoutinhours", "36",
                null))
                .andExpect(status().isBadRequest());
        call(PATH + "/findAuditEntries", new FindAuditEntriesRequest("admin", null, null, null, null, "01.05.2016",
                null, null, null, null))
                .andExpect(status().isBadRequest());
        call(PATH + "/findAuditEntries", new FindAuditEntriesRequest("admin", null, null, null, null, "2016-05-02",
                "2016-05-01", null, null, null))
                .andExpect(status().isBadRequest());
    }

    @Test
    void receptionSeesNeitherAuditNorConfigurationNorRoles() throws Exception {
        call(PATH + "/findAuditEntries", new FindAuditEntriesRequest("reception", null, null, null, null, null, null,
                null, null, null))
                .andExpect(status().isForbidden());
        call(PATH + "/getSystemProperties", new GetSystemPropertiesRequest("reception"))
                .andExpect(status().isForbidden());
        call(PATH + "/findRoles", new FindRolesRequest("reception")).andExpect(status().isForbidden());
        call(PATH + "/refreshUserSummary", new RefreshUserSummaryRequest("reception"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rightsMatrixListsTheRolesWithTheirRights() throws Exception {
        call(PATH + "/findRoles", new FindRolesRequest("admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0].name").value("RECEPTION"))
                .andExpect(jsonPath("$.roles[0].rights", hasItem("STAY_CHECKOUT")))
                .andExpect(jsonPath("$.roles[?(@.name == 'AFFILIATE_OWNER')].selfService", hasItem(true)));
    }
}
