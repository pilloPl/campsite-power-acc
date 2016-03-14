package io.pillopl.webservice.console.issuer;

import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupType;
import io.pillopl.issuer.IssuerPartner;
import io.pillopl.persistence.dao.IssuerPartnerDao;
import io.pillopl.user.UserroleName;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IssuerWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.ISSUER + "/runIssuerExchange";

    @TempDir
    Path exchangeDirectory;

    @Autowired GroupManager groupManager;
    @Autowired IssuerPartnerDao issuerPartnerDao;

    @Test
    void administratorRunsTheExchangeAndSeesTheStatusOfThePartners() throws Exception {
        Group club = groupManager.createChildGroup(console.issuer("HR ISSUERS"), "CONSOLE EXCHANGE CLUB", "Club",
                GroupType.ISSUER);
        IssuerPartner partner = new IssuerPartner("CONSOLE CLUB", club, IssuerPartner.InterfaceVersion.V1,
                exchangeDirectory.toString());
        partner.setCardImport(true);
        issuerPartnerDao.saveAndFlush(partner);
        Path cardsIn = Files.createDirectories(exchangeDirectory.resolve("cards-in"));
        Files.write(cardsIn.resolve("cards.csv"), List.of("CARD_UID;CARD_NUMBER;ACTION;ACTION_DATE;BILLING_ACCOUNT_NO;"
                + "VALID_TO", "04A1B2F1;PC-1;ADD;2026-07-01 09:30;770012345678;2026-12-31"), StandardCharsets.UTF_8);
        console.hrAdmin("admin");

        call(PATH, new RunIssuerExchangeRequest("admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncStatuses[?(@.partnerName == 'CONSOLE CLUB')].direction")
                        .value(contains("CARD_IMPORT")))
                .andExpect(jsonPath("$.syncStatuses[?(@.partnerName == 'CONSOLE CLUB')].status").value(contains("OK")))
                .andExpect(jsonPath("$.syncStatuses[?(@.partnerName == 'CONSOLE CLUB')].itemsAccepted")
                        .value(contains(1)));
    }

    @Test
    void exchangeNeedsTheConfigurationRight() throws Exception {
        console.user("technician", UserroleName.SERVICE_TECHNICIAN, console.staff("HR RECEPTION"),
                console.site("HR SITES"));

        call(PATH, new RunIssuerExchangeRequest("technician")).andExpect(status().isForbidden());
        call(PATH, new RunIssuerExchangeRequest("nobody")).andExpect(status().isForbidden());
    }
}
