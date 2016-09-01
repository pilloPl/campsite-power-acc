package io.pillopl.webservice.console.common;

import com.fasterxml.jackson.databind.JsonNode;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiDescriptionIntegrationTest extends WebServiceIntegrationTest {

    @Test
    void apiDescriptionIsOpenAndDescribesTheConsoleOperationsWithTheirRequestTypes() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        JsonNode api = objectMapper.readTree(body);

        assertThat(api.path("info").path("title").asText()).isEqualTo("Campsite Power backend");
        assertThat(api.path("paths").has(WebServicePaths.AUTH + "/login")).isTrue();
        assertThat(api.path("paths").has(WebServicePaths.AUTH + "/currentUser")).isTrue();
        assertThat(api.path("paths").has(WebServicePaths.CONSUMPTION_RECORD + "/findConsumptionRecords")).isTrue();
        assertThat(api.path("paths").has(WebServicePaths.PEDESTAL_LINK_V1 + "/{pedestalCode}/authorize")).isTrue();
        JsonNode schemas = api.path("components").path("schemas");
        assertThat(schemas.has("FindConsumptionRecordsRequest")).isTrue();
        assertThat(schemas.path("FindConsumptionRecordsRequest").path("properties").has("authUsername")).isFalse();
        assertThat(schemas.has("CurrentUserDTO")).isTrue();
        assertThat(api.path("components").path("securitySchemes").has("csrfHeader")).isTrue();
    }
}
