package io.pillopl.webservice.console.issuer;

import io.pillopl.dto.admin.IssuerPartnerDTO;
import io.pillopl.user.UserroleName;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IssuerPartnerWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.ISSUER;

    @BeforeEach
    void setUp() {
        console.hrAdmin("admin");
        console.user("office", UserroleName.BACK_OFFICE, console.staff("HR STAFF"), console.issuer("HR ISSUERS"));
        console.user("desk", UserroleName.RECEPTION, console.staff("HR RECEPTION"));
    }

    @Test
    void administratorSavesAPartnerAndBackOfficeSeesItInTheMatrix() throws Exception {
        Long issuerGroupId = console.issuer("HR ISSUERS").getId();
        call("admin", PATH + "/saveIssuerPartner", new SaveIssuerPartnerRequest(new IssuerPartnerDTO(null,
                "WSCLUB", "Web club", issuerGroupId, null, null, null, "V2", "https://wsclub.example.org", true,
                false, true, false, true, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partner.name").value("WSCLUB"));

        call("office", PATH + "/findIssuerPartners", new FindIssuerPartnersRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partners[?(@.name == 'WSCLUB')].endpoint").value("https://wsclub.example.org"));
        call("office", PATH + "/findIssuerSyncStatuses", new FindIssuerSyncStatusesRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncStatuses[?(@.partnerName == 'WSCLUB')].status").value(
                        org.hamcrest.Matchers.contains("NOT_RUN", "NOT_RUN")));
    }

    @Test
    void partnerEditingAndResyncNeedTheirRights() throws Exception {
        call("office", PATH + "/saveIssuerPartner", new SaveIssuerPartnerRequest(new IssuerPartnerDTO(null,
                "WSCLUB", null, console.issuer("HR ISSUERS").getId(), null, null, null, "V2", "x", true, false,
                false, false, true, null))).andExpect(status().isForbidden());
        call("desk", PATH + "/findIssuerPartners", new FindIssuerPartnersRequest()).andExpect(status().isForbidden());
        call("desk", PATH + "/resyncCard", new ResyncCardRequest(1L)).andExpect(status().isForbidden());
        call("office", PATH + "/resyncCard", new ResyncCardRequest(987_654_321L)).andExpect(status().isNotFound());
    }
}
