package io.pillopl.group;

import io.pillopl.card.CampingCard;
import io.pillopl.guest.Guest;
import io.pillopl.guest.GuestType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IssuerCodeServiceTest {

    private final GroupService groupService = mock(GroupService.class);
    private final IssuerCodeService issuerCodeService = new IssuerCodeService(groupService);

    @Test
    void everyMarketHasItsOwnIssuerCode() {
        assertThat(issuerCodeService.defineIssuer(Market.HR)).isEqualTo("CPN-HR");
        assertThat(issuerCodeService.defineIssuer(Market.IT)).isEqualTo("CPN-IT");
        assertThat(issuerCodeService.defineIssuer(Market.FR)).isEqualTo("CPN-FR");
        assertThat(issuerCodeService.defineIssuer(Market.ES)).isEqualTo("CPN-ES");
    }

    @Test
    void cardOfAGuestWithoutGroupGetsTheUnknownIssuer() {
        CampingCard card = new CampingCard.Builder("3", "3").guest(new Guest(null, GuestType.TRANSIT, "Novak")).build();

        assertThat(issuerCodeService.defineIssuer(card)).isEqualTo(IssuerCodeService.UNKNOWN_ISSUER);
        assertThat(issuerCodeService.defineIssuer((Market) null)).isEqualTo(IssuerCodeService.UNKNOWN_ISSUER);
    }

    @Test
    void groupWithoutMarketGetsTheUnknownIssuer() {
        Group group = new Group(GroupType.ISSUER, "CARAVAN CLUB");
        group.setId(5L);
        when(groupService.determineMarketByGroupId(5L)).thenReturn(null);

        assertThat(issuerCodeService.defineIssuer(group)).isEqualTo(IssuerCodeService.UNKNOWN_ISSUER);
        assertThat(issuerCodeService.defineIssuer((Group) null)).isEqualTo(IssuerCodeService.UNKNOWN_ISSUER);
        assertThat(issuerCodeService.defineIssuer(Market.UNKNOWN)).isEqualTo(IssuerCodeService.UNKNOWN_ISSUER);
    }

    @Test
    void issuerCodeFollowsTheMarketOfTheGroup() {
        Group group = new Group(GroupType.ISSUER, "CARAVAN CLUB");
        group.setId(6L);
        when(groupService.determineMarketByGroupId(6L)).thenReturn(Market.FR);

        assertThat(issuerCodeService.defineIssuer(group)).isEqualTo("CPN-FR");
    }

    @Test
    void issuerCodeOfACardFollowsTheGroupOfItsGuest() {
        Group group = new Group(GroupType.ISSUER, "NETWORK CARD");
        group.setId(7L);
        when(groupService.determineMarketByGroupId(7L)).thenReturn(Market.IT);
        CampingCard card = new CampingCard.Builder("1", "1").guest(new Guest(group, GuestType.TRANSIT, "Rossi")).build();

        assertThat(issuerCodeService.defineIssuer(card)).isEqualTo("CPN-IT");
        assertThat(issuerCodeService.defineIssuer(new CampingCard.Builder("2", "2").build()))
                .isEqualTo(IssuerCodeService.UNKNOWN_ISSUER);
        assertThat(issuerCodeService.defineIssuer((CampingCard) null)).isEqualTo(IssuerCodeService.UNKNOWN_ISSUER);
    }
}
