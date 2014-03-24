package io.pillopl.webservice.console.housecard;

import io.pillopl.dto.card.HouseCardDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupType;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.user.UserroleName;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HouseCardWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.HOUSE_CARD;

    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;

    @BeforeEach
    void setUp() {
        Group affiliate = groupManager.createChildGroup(console.site("HOUSE CARDS ONLY HR"), "AFFILIATE marko",
                "Marko's campsite", GroupType.SITE);
        Group lake = groupManager.createChildGroup(console.site("HR SITES"), "LAKE CAMPSITE", "Lake", GroupType.SITE);
        console.user("marko", UserroleName.AFFILIATE_OWNER, console.staff("HR SELF SERVICE"), affiliate);
        console.user("ivana", UserroleName.AFFILIATE_OWNER, console.staff("HR SELF SERVICE"), lake);
        console.user("technician", UserroleName.SERVICE_TECHNICIAN, console.staff("HR RECEPTION"), affiliate);
        console.hrAdmin("admin");
        infrastructure.pedestal("HR-MARKO-P001", affiliate, 2);
    }

    @Test
    void ownerAdministersTheHouseCardsOfItsCampsite() throws Exception {
        String json = call(PATH + "/saveHouseCard", new SaveHouseCardRequest("marko", "marko",
                new HouseCardDTO(null, "rental", "3001", "M-R1", true, null, null, null, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.houseCard.houseCardType").value("RENTAL"))
                .andExpect(jsonPath("$.houseCard.groupName").value("Marko's campsite"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(json).at("/houseCard/id").asLong();

        call(PATH + "/changeHouseCardStatus", new ChangeHouseCardStatusRequest("marko", "marko", id, false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        call(PATH + "/findHouseCards", new FindHouseCardsRequest("marko", "marko"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.houseCards", hasSize(1)))
                .andExpect(jsonPath("$.houseCards[0].active").value(false));
        call(PATH + "/deleteHouseCard", new DeleteHouseCardRequest("marko", "marko", id))
                .andExpect(status().isOk());
        call(PATH + "/deleteHouseCard", new DeleteHouseCardRequest("marko", "marko", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void staffManagingTheOwnerSeesItsHouseCards() throws Exception {
        call(PATH + "/saveHouseCard", new SaveHouseCardRequest("admin", "marko",
                new HouseCardDTO(null, "STAFF", "3002", "M-S1", true, null, null, null, null)))
                .andExpect(status().isOk());

        call(PATH + "/findHouseCards", new FindHouseCardsRequest("admin", "marko"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.houseCards", hasSize(1)));
    }

    @Test
    void anotherOwnerAndCallersWithoutTheRightAreRefused() throws Exception {
        call(PATH + "/findHouseCards", new FindHouseCardsRequest("ivana", "marko")).andExpect(status().isForbidden());
        call(PATH + "/findHouseCards", new FindHouseCardsRequest("technician", "marko"))
                .andExpect(status().isForbidden());
        call(PATH + "/findHouseCards", new FindHouseCardsRequest("marko", "nobody")).andExpect(status().isNotFound());
    }

    @Test
    void houseCardNeedsAKnownTypeAndADecimalUid() throws Exception {
        call(PATH + "/saveHouseCard", new SaveHouseCardRequest("marko", "marko",
                new HouseCardDTO(null, "GUEST", "3003", null, true, null, null, null, null)))
                .andExpect(status().isBadRequest());
        call(PATH + "/saveHouseCard", new SaveHouseCardRequest("marko", "marko",
                new HouseCardDTO(null, "STAFF", "04A1", null, true, null, null, null, null)))
                .andExpect(status().isBadRequest());
        call(PATH + "/saveHouseCard", new SaveHouseCardRequest("marko", "marko",
                new HouseCardDTO(999999L, "STAFF", "3004", null, true, null, null, null, null)))
                .andExpect(status().isNotFound());
        call(PATH + "/changeHouseCardStatus", new ChangeHouseCardStatusRequest("marko", "marko", null, true))
                .andExpect(status().isBadRequest());
    }
}
