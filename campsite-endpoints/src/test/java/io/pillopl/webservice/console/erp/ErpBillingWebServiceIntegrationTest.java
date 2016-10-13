package io.pillopl.webservice.console.erp;

import io.pillopl.user.UserroleName;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ErpBillingWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.ERP_BILLING;

    @BeforeEach
    void setUp() {
        console.user("office", UserroleName.BACK_OFFICE, console.staff("HR STAFF"));
        console.user("desk", UserroleName.RECEPTION, console.staff("HR RECEPTION"));
    }

    @Test
    void backOfficeReadsRunsLinesAndPreviewsAMonth() throws Exception {
        call("office", PATH + "/findErpBillingRuns", new FindErpBillingRunsRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runs").isArray());
        call("office", PATH + "/findErpBillingLines", new FindErpBillingLinesRequest("2019_01", null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines.length()").value(0));
        call("office", PATH + "/previewErpBilling", new ErpBillingMonthRequest(1, 2019))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines").isArray());
        call("office", PATH + "/retryErpExport", new RetryErpExportRequest("2019_01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resentLines").value(0));
        call("office", PATH + "/findRecordsWithoutExport", new FindRecordsWithoutExportRequest())
                .andExpect(status().isOk());
    }

    @Test
    void runNeedsTheBillingRightAndACompleteMonth() throws Exception {
        call("desk", PATH + "/runErpBilling", new ErpBillingMonthRequest(1, 2019)).andExpect(status().isForbidden());
        call("desk", PATH + "/findErpBillingRuns", new FindErpBillingRunsRequest()).andExpect(status().isForbidden());
        call("office", PATH + "/runErpBilling", new ErpBillingMonthRequest(null, 2019))
                .andExpect(status().isBadRequest());
        call("office", PATH + "/findErpBillingLines", new FindErpBillingLinesRequest("July", null))
                .andExpect(status().isBadRequest());
    }
}
