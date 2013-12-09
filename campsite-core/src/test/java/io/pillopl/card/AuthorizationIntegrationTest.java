package io.pillopl.card;

import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.persistence.dao.HouseCardDao;
import io.pillopl.test.IntegrationTest;
import io.pillopl.value.SocketLinkId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The pedestal's authorization on the database, with the default session registry and section budget.
 */
@Transactional
class AuthorizationIntegrationTest extends IntegrationTest {

    @Autowired Authorization authorization;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired HouseCardDao houseCardDao;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired CardFixture cards;

    Group club, bay;
    final SocketLinkId socket = SocketLinkId.of("HR-BAY-P001", 1);

    @BeforeEach
    void setUp() {
        bay = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "BAY CAMPSITE", "Bay");
        club = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"), "CARAVAN CLUB", "Club");
        groupManager.addAcceptanceAgreement(club, bay);
        infrastructure.pedestal("HR-BAY-P001", bay, 2);
    }

    @Test
    void guestWithAValidStayCardGetsSupply() {
        cards.card("6101", "HR-601", cards.guest("Kovac", club), LocalDate.now(), LocalDate.now().plusDays(4));

        AuthorizationResult result = authorization.authenticate("6101", socket);

        assertThat(result.getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
        assertThat(result.getGuestName()).isEqualTo("Kovac");
        assertThat(result.getExpiryDate()).isEqualTo(LocalDate.now().plusDays(4));
    }

    @Test
    void cardOfAnotherIssuerIsNotAcceptedOnTheCampsite() {
        cards.card("6102", cards.guest("Novak", groupService.fetchNetworkCardIssuerGroup()));

        assertThat(authorization.authenticate("6102", socket).getStatus())
                .isEqualTo(ValidationStatus.CARD_NOT_ACCEPTED_AT_SITE);
        assertThat(authorization.authenticate("6102", "HR-BAY-P001").getStatus())
                .isEqualTo(ValidationStatus.CARD_NOT_ACCEPTED_AT_SITE);
    }

    @Test
    void houseCardOfTheCampsiteIsAcceptedAndItsUseRecorded() {
        HouseCard houseCard = new HouseCard(HouseCardType.RENTAL, "6103", "R1");
        houseCard.setGroup(bay);
        houseCardDao.saveAndFlush(houseCard);

        assertThat(authorization.authenticate("6103", socket).getStatus()).isEqualTo(ValidationStatus.ACCEPTED);
        assertThat(houseCardDao.findById(houseCard.getId()).orElseThrow().getLastUsed()).isNotNull();
    }

    @Test
    void unknownSocketAndUnknownCardAreRefused() {
        assertThat(authorization.authenticate("6104", SocketLinkId.of("HR-BAY-P001", 5)).getStatus())
                .isEqualTo(ValidationStatus.SOCKET_INVALID);
        assertThat(authorization.authenticate("6104", socket).getStatus()).isEqualTo(ValidationStatus.CARD_INVALID);
    }
}
