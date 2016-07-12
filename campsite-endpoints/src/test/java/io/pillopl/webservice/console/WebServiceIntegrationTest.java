package io.pillopl.webservice.console;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pillopl.config.SystemPropertyService;
import io.pillopl.test.IntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Web service calls through MockMvc inside the test transaction (rolled back after each test). Audit rows are
 * written in their own transaction and removed afterwards; the configuration cache is reloaded.
 */
@Transactional
@AutoConfigureMockMvc
public abstract class WebServiceIntegrationTest extends IntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected ConsoleFixture console;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    SystemPropertyService systemPropertyService;

    @AfterTransaction
    void cleanUpCommittedRows() {
        jdbcTemplate.update("DELETE FROM AUDIT WHERE ACTION_TYPE IN ('PEDESTAL_IMPORT', 'PEDESTAL_COMMISSIONING', "
                + "'PEDESTAL_DECOMMISSIONING', 'CONFIG_CHANGE', 'EMAIL', 'LOGIN', 'CONSOLE_ACTION')");
        systemPropertyService.reloadSystemProperties();
    }

    /**
     * Call without a login (pedestal link, refused console calls).
     */
    protected ResultActions call(String path, Object request) throws Exception {
        return mockMvc.perform(post(path).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    /**
     * Call of the logged-in console user (the caller of the operation), with the CSRF token of the console.
     */
    protected ResultActions call(String caller, String path, Object request) throws Exception {
        return mockMvc.perform(post(path).with(user(caller)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    protected ResultActions callRaw(String caller, String path, String json) throws Exception {
        return mockMvc.perform(post(path).with(user(caller)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}
