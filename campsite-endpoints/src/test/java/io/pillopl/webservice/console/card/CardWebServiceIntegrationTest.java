package io.pillopl.webservice.console.card;

import io.pillopl.card.CampingCard;
import io.pillopl.card.CardFixture;
import io.pillopl.dto.card.IssueCardDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupType;
import io.pillopl.group.attribute.LocalListAttribute;
import io.pillopl.guest.BillingAccount;
import io.pillopl.guest.Guest;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.persistence.dao.BillingAccountDao;
import io.pillopl.persistence.dao.LocalListAttributeDao;
import io.pillopl.user.UserroleName;
import io.pillopl.webservice.console.WebServiceIntegrationTest;
import io.pillopl.webservice.console.common.WebServicePaths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CardWebServiceIntegrationTest extends WebServiceIntegrationTest {

    static final String PATH = WebServicePaths.CARD;
    static final LocalDate TODAY = LocalDate.now();

    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired CardFixture cards;
    @Autowired LocalListAttributeDao localListAttributeDao;
    @Autowired BillingAccountDao billingAccountDao;

    Group bay, club, discount;
    Guest member, outsider;

    @BeforeEach
    void setUp() {
        bay = groupManager.createChildGroup(console.site("HR SITES"), "BAY CAMPSITE", "Bay", GroupType.SITE);
        Group cove = groupManager.createChildGroup(console.site("HR SITES"), "COVE CAMPSITE", "Cove", GroupType.SITE);
        club = groupManager.createChildGroup(console.issuer("HR ISSUERS"), "CARAVAN CLUB", "Club",
                GroupType.ISSUER);
        discount = groupManager.createChildGroup(console.issuer("HR ISSUERS"), "DISCOUNT CARDS",
                "Discount", GroupType.ISSUER);
        groupManager.addAcceptanceAgreement(club, bay);
        infrastructure.pedestal("HR-BAY-P001", bay, 2);
        infrastructure.pedestal("HR-COVE-P001", cove, 1);
        member = cards.guest("Kovac", club);
        outsider = cards.guest("Novak", discount);
        console.user("reception", UserroleName.RECEPTION, console.staff("HR RECEPTION"), console.staff("HR RECEPTION"),
                bay, club);
        console.user("technician", UserroleName.SERVICE_TECHNICIAN, console.staff("HR RECEPTION"),
                console.staff("HR RECEPTION"), bay);
        console.hrAdmin("admin");
    }

    @Test
    void receptionIssuesAndFindsACard() throws Exception {
        call("reception", PATH + "/issueCard", new IssueCardRequest(new IssueCardDTO(member.getId(), "3283263748",
                "HR-900", TODAY, TODAY.plusDays(6), "400012345678")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.card.guestName").value("Kovac"))
                .andExpect(jsonPath("$.card.cardStatus").value("VALID"));

        call("reception", PATH + "/findCards", new FindCardsRequest("HR-900"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards", hasSize(1)))
                .andExpect(jsonPath("$.cards[0].cardRfid").value("3283263748"));
    }

    @Test
    void issueRefusesWrongDataGuestsOutsideTheScopeAndCallersWithoutTheRight() throws Exception {
        call("reception", PATH + "/issueCard", new IssueCardRequest(new IssueCardDTO(member.getId(), "04A1",
                "HR-901", null, null, "400012345678"))).andExpect(status().isBadRequest());
        call("reception", PATH + "/issueCard", new IssueCardRequest(null)).andExpect(status().isBadRequest());
        call("reception", PATH + "/issueCard", new IssueCardRequest(new IssueCardDTO(outsider.getId(), "1001",
                "HR-902", null, null, "400012345678"))).andExpect(status().isForbidden());
        call("technician", PATH + "/issueCard", new IssueCardRequest(new IssueCardDTO(member.getId(), "1002",
                "HR-903", null, null, "400012345678"))).andExpect(status().isForbidden());
        call("reception", PATH + "/issueCard", new IssueCardRequest(new IssueCardDTO(999999L, "1003", "HR-904",
                null, null, "400012345678"))).andExpect(status().isNotFound());
    }

    @Test
    void cardIsBlockedAndItsStayExtended() throws Exception {
        CampingCard card = cards.card("2001", "HR-910", member, TODAY.minusDays(3), TODAY.minusDays(1));

        call("reception", PATH + "/changeCardValidity", new ChangeCardValidityRequest(card.getId(),
                TODAY.minusDays(3), TODAY.plusDays(2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.card.cardStatus").value("VALID"));
        call("reception", PATH + "/changeCardStatus", new ChangeCardStatusRequest(card.getId(), "deactivated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.card.cardStatus").value("CARD_DEACTIVATED"));
        call("reception", PATH + "/changeCardStatus", new ChangeCardStatusRequest(card.getId(), "LOST"))
                .andExpect(status().isBadRequest());
        call("reception", PATH + "/changeCardStatus", new ChangeCardStatusRequest(999999L, "ACTIVATED"))
                .andExpect(status().isNotFound());
    }

    @Test
    void checkAtTheSocketAnswersLikeThePedestal() throws Exception {
        cards.card("2002", "HR-911", member, TODAY, TODAY.plusDays(1));

        call("reception", PATH + "/checkCardAtSocket", new CheckCardAtSocketRequest("2002", "HR-BAY-P001", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.check.validationStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$.check.guestName").value("Kovac"));
        call("reception", PATH + "/checkCardAtSocket", new CheckCardAtSocketRequest("2999", "HR-BAY-P001", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.check.validationStatus").value("CARD_INVALID"));
        call("reception", PATH + "/checkCardAtSocket", new CheckCardAtSocketRequest("2002", "HR-COVE-P001", 1))
                .andExpect(status().isNotFound());
        call("reception", PATH + "/checkCardAtSocket", new CheckCardAtSocketRequest("2002", "HR-BAY-P001", 9))
                .andExpect(status().isBadRequest());
    }

    @Test
    void localCardListOfAPedestalIsShownAndSendingWithoutLinkIsAConflict() throws Exception {
        LocalListAttribute attribute = new LocalListAttribute(true, true, true);
        attribute.setGroup(bay);
        localListAttributeDao.saveAndFlush(attribute);
        cards.card("2003", "HR-912", member, TODAY, TODAY.plusDays(1));

        call("reception", PATH + "/getLocalCardList", new GetLocalCardListRequest("HR-BAY-P001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.localCardList.entries", hasSize(1)))
                .andExpect(jsonPath("$.localCardList.capacity").value(250));
        call("reception", PATH + "/getLocalCardList", new GetLocalCardListRequest("HR-COVE-P001"))
                .andExpect(status().isNotFound());
        call("reception", PATH + "/sendLocalCardList", new SendLocalCardListRequest("HR-BAY-P001"))
                .andExpect(status().isForbidden());
        call("admin", PATH + "/sendLocalCardList", new SendLocalCardListRequest("HR-BAY-P001"))
                .andExpect(status().isConflict());
        call("admin", PATH + "/sendLocalCardList", new SendLocalCardListRequest("HR-COVE-P001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(false));
    }

    @Test
    void receptionHandsAWelcomePoolCardToAGuestForTheStayAndTheCardLeavesThePool() throws Exception {
        console.user("desk", UserroleName.RECEPTION, console.staff("HR RECEPTION"), console.staff("HR RECEPTION"),
                bay, console.issuer("HR ISSUERS"));
        Guest poolGuest = cards.guest("Welcome pool", console.issuer("HR WELCOME_CARD_POOL"));
        CampingCard welcome = cards.card("6001", "HR-W-001", poolGuest, null, null);
        BillingAccount account = billingAccountDao.saveAndFlush(new BillingAccount(member, "400099887766", "Stay"));
        member.getBillingAccounts().add(account);

        call("desk", PATH + "/findWelcomePoolCards", new FindWelcomePoolCardsRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards", hasSize(1)))
                .andExpect(jsonPath("$.cards[0].cardNumber").value("HR-W-001"));
        call("desk", PATH + "/assignWelcomeCard", new AssignWelcomeCardRequest(welcome.getId(), member.getId(),
                TODAY, TODAY.plusDays(5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.card.guestName").value("Kovac"))
                .andExpect(jsonPath("$.card.validTo").value(TODAY.plusDays(5).toString()))
                .andExpect(jsonPath("$.card.billingAccountNo").value("400099887766"))
                .andExpect(jsonPath("$.card.cardStatus").value("VALID"));

        call("desk", PATH + "/findWelcomePoolCards", new FindWelcomePoolCardsRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards", hasSize(0)));
        assertThat(jdbcTemplate.queryForList("SELECT STATUS FROM AUDIT WHERE ACTION_TYPE = 'CONSOLE_ACTION' "
                + "AND USERID = 'desk' AND ACTION_NAME = 'card/assignWelcomeCard'", String.class))
                .containsExactly("SUCCESS");
    }

    @Test
    void onlyFreeWelcomePoolCardsAreHandedOutWithAValidityToCallersWithTheRight() throws Exception {
        console.user("desk", UserroleName.RECEPTION, console.staff("HR RECEPTION"), console.staff("HR RECEPTION"),
                bay, console.issuer("HR ISSUERS"));
        Guest poolGuest = cards.guest("Welcome pool", console.issuer("HR WELCOME_CARD_POOL"));
        CampingCard welcome = cards.card("6002", "HR-W-002", poolGuest, null, null);
        CampingCard own = cards.card("6003", "HR-920", member, TODAY, TODAY.plusDays(1));

        call("desk", PATH + "/assignWelcomeCard", new AssignWelcomeCardRequest(own.getId(), outsider.getId(),
                TODAY, TODAY.plusDays(2))).andExpect(status().isConflict());
        call("desk", PATH + "/assignWelcomeCard", new AssignWelcomeCardRequest(welcome.getId(), member.getId(),
                TODAY, null)).andExpect(status().isBadRequest());
        call("desk", PATH + "/assignWelcomeCard", new AssignWelcomeCardRequest(welcome.getId(), poolGuest.getId(),
                TODAY, TODAY.plusDays(2))).andExpect(status().isConflict());
        call("desk", PATH + "/assignWelcomeCard", new AssignWelcomeCardRequest(welcome.getId(), null, TODAY,
                TODAY.plusDays(2))).andExpect(status().isBadRequest());
        call("technician", PATH + "/assignWelcomeCard", new AssignWelcomeCardRequest(welcome.getId(),
                member.getId(), TODAY, TODAY.plusDays(2))).andExpect(status().isForbidden());
        call("reception", PATH + "/findWelcomePoolCards", new FindWelcomePoolCardsRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards", hasSize(0)));
    }
}
