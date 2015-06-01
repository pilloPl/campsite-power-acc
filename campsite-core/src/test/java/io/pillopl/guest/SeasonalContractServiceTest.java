package io.pillopl.guest;

import io.pillopl.card.CampingCard;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.group.Market;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.persistence.dao.SeasonalContractDao;
import io.pillopl.pricing.Tariff;
import io.pillopl.util.DateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SeasonalContractServiceTest {

    static final LocalDateTime AT = LocalDateTime.of(2026, 7, 6, 14, 0);
    static final String ACCOUNT = "400012345678";

    SeasonalContractDao contractDao = mock(SeasonalContractDao.class);
    CampingCardDao cardDao = mock(CampingCardDao.class);
    GroupService groupService = mock(GroupService.class);

    SeasonalContractService service = new SeasonalContractService(contractDao, cardDao, groupService,
            new DateService());

    final Tariff seasonal = new Tariff("HR SEASONAL", "Seasonal", Market.HR);
    final Group bay = group(500L, GroupType.SITE, "BAY");
    final Guest guest = new Guest(new Group(GroupType.ISSUER, "NETWORK CARD"), GuestType.SEASONAL, "Horvat");
    BillingAccount account;
    CampingCard card;
    Socket socket;

    @BeforeEach
    void setUp() {
        guest.setId(70L);
        account = guest.openBillingAccount(ACCOUNT, "Season");
        card = new CampingCard.Builder("1", "HR-1").guest(guest).billingAccountNo(ACCOUNT).build();
        card.setId(80L);
        when(cardDao.findById(80L)).thenReturn(Optional.of(card));
        Pedestal pedestal = new Pedestal();
        pedestal.setGroup(group(501L, GroupType.SITE, "BAY NORTH"));
        socket = new Socket();
        socket.setPitchNo("A12");
        pedestal.addSocket(socket);
        when(groupService.findManageable(bay)).thenReturn(List.of(500L, 501L));
    }

    SeasonalContract contract(Group siteGroup, String pitchNo) {
        SeasonalContract contract = new SeasonalContract(account, seasonal, siteGroup, pitchNo,
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 10, 31));
        when(contractDao.findStartedContractsOfGuest(70L, AT.toLocalDate())).thenReturn(List.of(contract));
        return contract;
    }

    @Test
    void contractTariffAppliesOnTheContractsPitch() {
        contract(bay, "a12");

        assertThat(service.findContractTariff(socket, 80L, AT)).isSameAs(seasonal);
    }

    @Test
    void supplyWithoutCardHasNoContract() {
        assertThat(service.findContractTariff(socket, null, AT)).isNull();
        verifyNoInteractions(contractDao);
    }

    @Test
    void unknownCardOrCardWithoutGuestHasNoContract() {
        when(cardDao.findById(anyLong())).thenReturn(Optional.empty());
        assertThat(service.findContractTariff(socket, 81L, AT)).isNull();

        card.setGuest(null);
        when(cardDao.findById(80L)).thenReturn(Optional.of(card));
        assertThat(service.findContractTariff(socket, 80L, AT)).isNull();
    }

    @Test
    void contractOverOnTheDayGivesNoTariff() {
        contract(bay, "A12").requestTermination(LocalDate.of(2026, 7, 5), AT.minusDays(10));

        assertThat(service.findContractTariff(socket, 80L, AT)).isNull();
    }

    @Test
    void cardBilledToAnotherAccountGetsNoContractTariff() {
        contract(bay, "A12");
        card.setBillingAccountNo("400099999999");

        assertThat(service.findContractTariff(socket, 80L, AT)).isNull();
    }

    @Test
    void socketOnAnotherPitchOrCampsiteGetsNoContractTariff() {
        contract(bay, "A13");
        assertThat(service.findContractTariff(socket, 80L, AT)).isNull();

        contract(group(600L, GroupType.SITE, "LAKE"), null);
        when(groupService.findManageable(any())).thenReturn(List.of(600L));
        assertThat(service.findContractTariff(socket, 80L, AT)).isNull();
    }

    @Test
    void contractWithoutPitchAppliesOnTheWholeCampsiteAndWithoutCampsiteEverywhere() {
        contract(bay, null);
        assertThat(service.findContractTariff(socket, 80L, AT)).isSameAs(seasonal);

        contract(null, null);
        assertThat(service.findContractTariff(new Socket(), 80L, AT)).isSameAs(seasonal);
    }

    private static Group group(Long id, GroupType type, String name) {
        Group group = new Group(type, name);
        group.setId(id);
        return group;
    }
}
