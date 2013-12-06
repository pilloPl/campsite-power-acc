package io.pillopl.card;

import io.pillopl.group.AccessDeniedException;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.guest.Guest;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.test.IntegrationTest;
import io.pillopl.user.Userinfo;
import io.pillopl.value.SocketLinkId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class CampingCardServiceIntegrationTest extends IntegrationTest {

    static final LocalDate TODAY = LocalDate.now();

    @Autowired CampingCardService campingCardService;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired UserinfoDao userinfoDao;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired CardFixture cards;

    Group club, discount;
    Guest member, outsider;
    Userinfo clubReception;

    @BeforeEach
    void setUp() {
        Group hrIssuers = groupService.fetchIssuerGroup("HR ISSUERS");
        club = groupManager.createChildGroup(hrIssuers, "CARAVAN CLUB", "Club");
        discount = groupManager.createChildGroup(hrIssuers, "DISCOUNT CARDS", "Discount");
        Group bay = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "BAY CAMPSITE", "Bay");
        groupManager.addAcceptanceAgreement(club, bay);
        infrastructure.pedestal("HR-BAY-P001", bay, 2);
        member = cards.guest("Kovac", club);
        outsider = cards.guest("Novak", discount);
        clubReception = new Userinfo("reception", "Reception", "reception@example.org");
        clubReception.setGroup(groupService.fetchStaffGroup("HR RECEPTION"));
        clubReception.getGroups().add(club);
        userinfoDao.saveAndFlush(clubReception);
    }

    @Test
    void receptionIssuesACardForTheStayOfAGuest() {
        CampingCard card = campingCardService.issueCard(clubReception, member.getId(), "3283263748", "HR-501",
                TODAY, TODAY.plusDays(7), "400012345678");

        assertThat(card.getId()).isNotNull();
        assertThat(campingCardService.findByRfid("3283263748")).isEqualTo(card);
        assertThat(campingCardService.findCards(clubReception, "HR-501")).containsExactly(card);
        assertThat(campingCardService.findCardsOfGuest(clubReception, member.getId())).containsExactly(card);
    }

    @Test
    void cardIsIssuedOnlyOnceWithValidDataAndToManageableGuests() {
        campingCardService.issueCard(clubReception, member.getId(), "1111", "HR-502", null, null, "400012345678");

        assertThatThrownBy(() -> campingCardService.issueCard(clubReception, member.getId(), "1111", "HR-502", null,
                null, "400012345678")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> campingCardService.issueCard(clubReception, member.getId(), "04A1", "HR-503", null,
                null, "400012345678")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> campingCardService.issueCard(clubReception, member.getId(), "1112", "HR-504", null,
                null, "ACC")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> campingCardService.issueCard(clubReception, member.getId(), "1113", "HR-505",
                TODAY, TODAY.minusDays(1), "400012345678")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> campingCardService.issueCard(clubReception, outsider.getId(), "1114", "HR-506",
                null, null, "400012345678")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> campingCardService.issueCard(clubReception, 999999L, "1115", "HR-507", null, null,
                "400012345678")).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void cardsOfGuestsOutsideTheScopeAreNotFound() {
        cards.card("2222", "HR-510", outsider, null, null);

        assertThat(campingCardService.findCards(clubReception, "2222")).isEmpty();
        assertThat(campingCardService.findCards(clubReception, " ")).isEmpty();
    }

    @Test
    void blockedCardIsRefusedAtTheSocketAndCanBeUnblocked() {
        CampingCard card = cards.card("3333", "HR-511", member, TODAY, TODAY.plusDays(2));
        SocketLinkId socket = SocketLinkId.of("HR-BAY-P001", 1);
        assertThat(campingCardService.checkCardAtSocket(clubReception, "3333", socket).getStatus())
                .isEqualTo(ValidationStatus.ACCEPTED);

        campingCardService.changeActivation(clubReception, card.getId(), CardActivation.DEACTIVATED);
        assertThat(campingCardService.checkCardAtSocket(clubReception, "3333", socket).getStatus())
                .isEqualTo(ValidationStatus.BLOCKED);

        campingCardService.changeActivation(clubReception, card.getId(), CardActivation.ACTIVATED);
        assertThat(campingCardService.checkCardAtSocket(clubReception, "3333", socket).getStatus())
                .isEqualTo(ValidationStatus.ACCEPTED);
    }

    @Test
    void extendedStayKeepsTheCardValid() {
        CampingCard card = cards.card("4444", "HR-512", member, TODAY.minusDays(5), TODAY.minusDays(1));
        SocketLinkId socket = SocketLinkId.of("HR-BAY-P001", 2);
        assertThat(campingCardService.checkCardAtSocket(clubReception, "4444", socket).getStatus())
                .isEqualTo(ValidationStatus.EXPIRED);

        campingCardService.changeValidity(clubReception, card.getId(), TODAY.minusDays(5), TODAY.plusDays(3));

        assertThat(campingCardService.checkCardAtSocket(clubReception, "4444", socket).getStatus())
                .isEqualTo(ValidationStatus.ACCEPTED);
    }

    @Test
    void cardOutsideTheScopeCannotBeChanged() {
        CampingCard card = cards.card("5555", "HR-513", outsider, null, null);

        assertThatThrownBy(() -> campingCardService.changeActivation(clubReception, card.getId(),
                CardActivation.DEACTIVATED)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> campingCardService.changeValidity(clubReception, 999999L, null, null))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void issuerFileCardIsImportedThroughTheUpsert() {
        CampingCard card = campingCardService.importCard("04A1B2C3", "HR-514", null, TODAY.plusDays(30),
                "400012345678", member.getId());

        assertThat(card.getCardRfid()).isEqualTo("3283263748");
        assertThat(card.getGuest().getId()).isEqualTo(member.getId());
    }
}
