package io.pillopl.webservice.console.user;

import io.pillopl.user.UserroleName;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.administration.RefreshUserSummaryRequest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.USER;

    @BeforeEach
    void setUp() {
        console.hrAdmin("admin");
        console.user("reception", UserroleName.RECEPTION, console.staff("HR RECEPTION"), console.staff("HR RECEPTION"));
    }

    @Test
    void newSelfServiceUserGetsItsUsernameFromTheLowerCaseEmail() throws Exception {
        call("admin", PATH + "/createUser", new CreateUserRequest("Luka.Horvat@example.org", "Luka", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("luka.horvat"))
                .andExpect(jsonPath("$.user.status").value("INVITED"))
                .andExpect(jsonPath("$.user.group.name").value("HR SELF SERVICE"))
                .andExpect(jsonPath("$.user.roles", contains("SELF_SERVICE_USER")))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
        call("admin", PATH + "/createUser", new CreateUserRequest("luka.horvat@example.org", "Luka", "HR"))
                .andExpect(status().isConflict());
        call("admin", PATH + "/createUser", new CreateUserRequest("luka", "Luka", "HR"))
                .andExpect(status().isBadRequest());
        call("reception", PATH + "/createUser", new CreateUserRequest("ana@example.org", "Ana", "HR"))
                .andExpect(status().isForbidden());
    }

    @Test
    void everyUserSeesAndRenamesItselfButNotOthers() throws Exception {
        call("reception", PATH + "/findUser", new FindUserRequest("reception")).andExpect(status().isOk());
        call("reception", PATH + "/findUser", new FindUserRequest("admin")).andExpect(status().isForbidden());
        call("reception", PATH + "/updateUser", new UpdateUserRequest("reception", null, " Front desk "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value("Front desk"))
                .andExpect(jsonPath("$.user.email").value("reception@example.org"));
        call("reception", PATH + "/updateUser", new UpdateUserRequest("admin", null, "Boss"))
                .andExpect(status().isForbidden());
        call("admin", PATH + "/updateUser", new UpdateUserRequest("reception", "admin@example.org", null))
                .andExpect(status().isConflict());
        call("admin", PATH + "/findUser", new FindUserRequest("ghost")).andExpect(status().isNotFound());
    }

    @Test
    void affiliateOwnerSetUpGivesTheOwnerItsCampsiteAndRoles() throws Exception {
        console.user("marko", null, console.staff("HR SELF SERVICE"));

        call("admin", PATH + "/setupAffiliateOwner", new SetupAffiliateOwnerRequest("marko"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.scopeGroups[0].name").value("AFFILIATE marko"))
                .andExpect(jsonPath("$.user.roles", hasItem("AFFILIATE_OWNER")))
                .andExpect(jsonPath("$.user.roles", not(hasItem("AFFILIATE_PRICE_REPORTING"))));
    }

    @Test
    void usersAreFoundByRolesInTheScope() throws Exception {
        call("admin", PATH + "/findUsersByRoles", new FindUsersByRolesRequest(List.of("RECEPTION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[*].username", contains("reception")));
        call("admin", PATH + "/findUsersByRoles", new FindUsersByRolesRequest(List.of("WIZARD")))
                .andExpect(status().isBadRequest());
        call("admin", PATH + "/findUsersByRoles", new FindUsersByRolesRequest(null))
                .andExpect(status().isBadRequest());
    }

    @Test
    void selfServiceListComesFromTheRefreshedView() throws Exception {
        console.user("marko", null, console.staff("HR SELF SERVICE"));
        call("admin", PATH + "/setupAffiliateOwner", new SetupAffiliateOwnerRequest("marko"));
        call("admin", WebServicePaths.ADMINISTRATION + "/refreshUserSummary", new RefreshUserSummaryRequest())
                .andExpect(status().isOk());

        call("admin", PATH + "/findSelfServiceUsers", new FindSelfServiceUsersRequest(true, null, null, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[*].username", contains("marko")))
                .andExpect(jsonPath("$.users[0].affiliateOwner").value(true))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }
}
