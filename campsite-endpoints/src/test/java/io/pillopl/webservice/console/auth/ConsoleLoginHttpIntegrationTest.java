package io.pillopl.webservice.console.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.test.IntegrationTest;
import io.pillopl.user.Userinfo;
import io.pillopl.webservice.console.ConsoleFixture;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The login as the console does it over HTTP: the first GET gives the XSRF-TOKEN cookie, the login with the token in
 * the X-XSRF-TOKEN header gives the session cookie, later calls send both.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConsoleLoginHttpIntegrationTest extends IntegrationTest {

    @LocalServerPort int port;
    @Autowired ConsoleFixture console;
    @Autowired UserinfoDao userinfoDao;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ObjectMapper objectMapper;

    final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build();

    @BeforeEach
    void setUp() {
        Userinfo user = console.hrAdmin("httplogin");
        user.setPasswordHash(passwordEncoder.encode("campsite-secret"));
        userinfoDao.saveAndFlush(user);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM AUDIT WHERE USERID = 'httplogin'");
        jdbcTemplate.update("DELETE FROM USERINFO_GROUP WHERE USERINFO_ID IN "
                + "(SELECT ID FROM USERINFO WHERE USERNAME = 'httplogin')");
        jdbcTemplate.update("DELETE FROM USERINFO_USERROLE WHERE USERINFO_ID IN "
                + "(SELECT ID FROM USERINFO WHERE USERNAME = 'httplogin')");
        jdbcTemplate.update("DELETE FROM USERINFO WHERE USERNAME = 'httplogin'");
    }

    @Test
    void consoleLogsInWithTheCsrfCookieAndCallsWithTheSessionCookie() throws Exception {
        HttpResponse<String> anonymous = get(WebServicePaths.AUTH + "/currentUser");
        assertThat(anonymous.statusCode()).isEqualTo(401);
        String token = cookie("XSRF-TOKEN");
        assertThat(token).isNotBlank();

        assertThat(post(WebServicePaths.AUTH + "/login", "{\"username\":\"httplogin\",\"password\":\"campsite-secret\"}",
                null).statusCode()).as("login without the CSRF header").isEqualTo(403);
        HttpResponse<String> login = post(WebServicePaths.AUTH + "/login",
                "{\"username\":\"httplogin\",\"password\":\"campsite-secret\"}", token);
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(cookie("JSESSIONID")).isNotBlank();
        token = cookie("XSRF-TOKEN");

        HttpResponse<String> me = get(WebServicePaths.AUTH + "/currentUser");
        assertThat(me.statusCode()).isEqualTo(200);
        JsonNode user = objectMapper.readTree(me.body()).path("user");
        assertThat(user.path("username").asText()).isEqualTo("httplogin");

        assertThat(post(WebServicePaths.ADMINISTRATION + "/getSystemProperties", "{}", token).statusCode())
                .isEqualTo(200);
        assertThat(post(WebServicePaths.ADMINISTRATION + "/getSystemProperties", "{}", null).statusCode())
                .as("state-changing method without the CSRF header").isEqualTo(403);

        assertThat(post(WebServicePaths.AUTH + "/logout", "{}", token).statusCode()).isEqualTo(200);
        assertThat(get(WebServicePaths.AUTH + "/currentUser").statusCode()).isEqualTo(401);
    }

    private String cookie(String name) {
        for (HttpCookie cookie : cookies.getCookieStore().getCookies()) {
            if (cookie.getName().equals(name)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String json, String csrfToken) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json));
        if (csrfToken != null) {
            request.header("X-XSRF-TOKEN", csrfToken);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
