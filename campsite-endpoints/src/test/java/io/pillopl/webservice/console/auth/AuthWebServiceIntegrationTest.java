package io.pillopl.webservice.console.auth;

import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserinfoStatus;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.administration.SetSystemPropertyRequest;
import io.pillopl.webservice.console.common.WebServicePaths;
import io.pillopl.webservice.console.user.SetPasswordRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.AUTH;

    @Autowired PasswordEncoder passwordEncoder;
    @Autowired UserinfoDao userinfoDao;

    @BeforeEach
    void setUp() {
        Userinfo admin = console.hrAdmin("loginadmin");
        admin.setPasswordHash(passwordEncoder.encode("campsite-secret"));
        userinfoDao.saveAndFlush(admin);
    }

    @Test
    void consoleCallsWithoutLoginAreRefusedWithNotAuthenticated() throws Exception {
        call(WebServicePaths.ADMINISTRATION + "/getSystemProperties", Map.of())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("NOT_AUTHENTICATED"));
        mockMvc.perform(get(PATH + "/currentUser")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginAnswersTheUserWithRolesAndRightsAndTheSessionCarriesItUntilLogout() throws Exception {
        MvcResult login = login("loginadmin", "campsite-secret")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("loginadmin"))
                .andExpect(jsonPath("$.user.roles", hasItem("MARKET_ADMIN")))
                .andExpect(jsonPath("$.user.rights", hasItem("CONFIG_EDIT")))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(get(PATH + "/currentUser").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("loginadmin"));
        mockMvc.perform(post(WebServicePaths.ADMINISTRATION + "/getSystemProperties").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        assertThat(userinfoDao.findByUsername("loginadmin").getLastLoginDate()).isNotNull();

        mockMvc.perform(post(PATH + "/logout").session(session).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loggedOut").value(true));
        mockMvc.perform(get(PATH + "/currentUser").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithoutCsrfTokenIsRefused() throws Exception {
        mockMvc.perform(post(PATH + "/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("loginadmin", "campsite-secret"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_AUTHORIZED"));
    }

    @Test
    void wrongPasswordsAreCountedAuditedAndLockTheUser() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("loginadmin", "wrong-password")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Wrong username or password"));
        }
        assertThat(userinfoDao.findByUsername("loginadmin").getStatus()).isEqualTo(UserinfoStatus.LOCKED);
        login("loginadmin", "campsite-secret").andExpect(status().isUnauthorized());
        List<String> errors = jdbcTemplate.queryForList("SELECT ERROR_REASON FROM AUDIT WHERE ACTION_TYPE = 'LOGIN' "
                + "AND USERID = 'loginadmin' ORDER BY ID", String.class);
        assertThat(errors).hasSize(6).startsWith("wrong password").endsWith("user is LOCKED");
    }

    @Test
    void invitedUserWithoutPasswordAndUnknownUserCannotLogIn() throws Exception {
        console.hrAdmin("invited").setStatus(UserinfoStatus.INVITED);
        login("invited", "anything-at-all").andExpect(status().isUnauthorized());
        login("nobody", "anything-at-all").andExpect(status().isUnauthorized());
    }

    @Test
    void stateChangingConsoleCallsAreAuditedWithoutPasswordsAndReadsAreNot() throws Exception {
        console.hrAdmin("newcomer");
        call("loginadmin", WebServicePaths.USER + "/setPassword", new SetPasswordRequest("newcomer", "very-secret-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.status").value("ACTIVE"));
        call("loginadmin", WebServicePaths.ADMINISTRATION + "/setSystemProperty",
                new SetSystemPropertyRequest("UnknownKey", "1", null))
                .andExpect(status().isBadRequest());
        call("loginadmin", WebServicePaths.ADMINISTRATION + "/getSystemProperties", Map.of())
                .andExpect(status().isOk());

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT ACTION_NAME, MESSAGE, ERROR_REASON, STATUS "
                + "FROM AUDIT WHERE ACTION_TYPE = 'CONSOLE_ACTION' AND USERID = 'loginadmin' ORDER BY ID");
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsEntry("ACTION_NAME", "user/setPassword").containsEntry("STATUS", "SUCCESS");
        assertThat((String) rows.get(0).get("MESSAGE")).contains("newcomer").doesNotContain("very-secret-1");
        assertThat(rows.get(1)).containsEntry("ACTION_NAME", "administration/setSystemProperty")
                .containsEntry("STATUS", "ERROR");
        assertThat((String) rows.get(1).get("ERROR_REASON")).startsWith("HTTP 400").contains("UnknownKey");
    }

    @Test
    void viteDevServerMayCallWithCredentials() throws Exception {
        mockMvc.perform(options(PATH + "/login").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-xsrf-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mockMvc.perform(options(PATH + "/login").header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    private ResultActions login(String username, String password)
            throws Exception {
        return mockMvc.perform(post(PATH + "/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password))));
    }
}
