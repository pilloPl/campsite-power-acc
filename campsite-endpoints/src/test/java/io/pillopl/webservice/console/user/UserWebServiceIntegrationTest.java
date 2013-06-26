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
        call(PATH + "/createUser", new CreateUserRequest("admin", "Luka.Horvat@example.org", "Luka", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("luka.horvat"))
                .andExpect(jsonPath("$.user.status").value("INVITED"))
                .andExpect(jsonPath("$.user.group.name").value("HR SELF SERVICE"))
                .andExpect(jsonPath("$.user.roles", contains("SELF_SERVICE_USER")))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
        call(PATH + "/createUser", new CreateUserRequest("admin", "luka.horvat@example.org", "Luka", "HR"))
                .andExpect(status().isConflict());
        call(PATH + "/createUser", new CreateUserRequest("admin", "luka", "Luka", "HR"))
                .andExpect(status().isBadRequest());
        call(PATH + "/createUser", new CreateUserRequest("reception", "ana@example.org", "Ana", "HR"))
                .andExpect(status().isForbidden());
    }

    @Test
    void everyUserSeesAndRenamesItselfButNotOthers() throws Exception {
        call(PATH + "/findUser", new FindUserRequest("reception", "reception")).andExpect(status().isOk());
        call(PATH + "/findUser", new FindUserRequest("reception", "admin")).andExpect(status().isForbidden());
        call(PATH + "/updateUser", new UpdateUserRequest("reception", "reception", null, " Front desk "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.name").value("Front desk"))
                .andExpect(jsonPath("$.user.email").value("reception@example.org"));
        call(PATH + "/updateUser", new UpdateUserRequest("reception", "admin", null, "Boss"))
                .andExpect(status().isForbidden());
        call(PATH + "/updateUser", new UpdateUserRequest("admin", "reception", "admin@example.org", null))
                .andExpect(status().isConflict());
        call(PATH + "/findUser", new FindUserRequest("admin", "ghost")).andExpect(status().isNotFound());
    }

    @Test
    void affiliateOwnerSetUpGivesTheOwnerItsCampsiteAndRoles() throws Exception {
        console.user("marko", null, console.staff("HR SELF SERVICE"));

        call(PATH + "/setupAffiliateOwner", new SetupAffiliateOwnerRequest("admin", "marko"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.scopeGroups[0].name").value("AFFILIATE marko"))
                .andExpect(jsonPath("$.user.roles", hasItem("AFFILIATE_OWNER")))
                .andExpect(jsonPath("$.user.roles", not(hasItem("AFFILIATE_PRICE_REPORTING"))));
    }

    @Test
    void usersAreFoundByRolesInTheScope() throws Exception {
        call(PATH + "/findUsersByRoles", new FindUsersByRolesRequest("admin", List.of("RECEPTION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[*].username", contains("reception")));
        call(PATH + "/findUsersByRoles", new FindUsersByRolesRequest("admin", List.of("WIZARD")))
                .andExpect(status().isBadRequest());
        call(PATH + "/findUsersByRoles", new FindUsersByRolesRequest("admin", null))
                .andExpect(status().isBadRequest());
    }

    @Test
    void selfServiceListComesFromTheRefreshedView() throws Exception {
        console.user("marko", null, console.staff("HR SELF SERVICE"));
        call(PATH + "/setupAffiliateOwner", new SetupAffiliateOwnerRequest("admin", "marko"));
        call(WebServicePaths.ADMINISTRATION + "/refreshUserSummary", new RefreshUserSummaryRequest("admin"))
                .andExpect(status().isOk());

        call(PATH + "/findSelfServiceUsers", new FindSelfServiceUsersRequest("admin", true, null, null, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[*].username", contains("marko")))
                .andExpect(jsonPath("$.users[0].affiliateOwner").value(true))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }
}
