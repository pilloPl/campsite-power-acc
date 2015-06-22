package io.pillopl.order;

import io.pillopl.card.CampingCard;
import io.pillopl.card.CardFixture;
import io.pillopl.card.CardStatus;
import io.pillopl.group.AccessDeniedException;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.guest.BillingAccount;
import io.pillopl.guest.Guest;
import io.pillopl.guest.GuestType;
import io.pillopl.guest.SeasonalContract;
import io.pillopl.guest.SeasonalContractService;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.BillingAccountDao;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.persistence.dao.GuestDao;
import io.pillopl.persistence.dao.SeasonalContractDao;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.pricing.TariffResolver;
import io.pillopl.pricing.TariffSource;
import io.pillopl.test.IntegrationTest;
import io.pillopl.user.Userinfo;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Orders on the database: a new seasonal resident waits for the ERP, the ERP's numbers activate the order into guest,
 * billing account, contract and card, and the contract's tariff then overrides the pedestal's.
 */
@Transactional
class OrderServiceIntegrationTest extends IntegrationTest {

    static final LocalDate SEASON_START = LocalDate.of(2026, 4, 1);
    static final String ACCOUNT = "400055550001";

    @Autowired OrderService orderService;
    @Autowired SeasonalContractService seasonalContractService;
    @Autowired TariffResolver tariffResolver;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired UserinfoDao userinfoDao;
    @Autowired GuestDao guestDao;
    @Autowired BillingAccountDao billingAccountDao;
    @Autowired SeasonalContractDao seasonalContractDao;
    @Autowired CampingCardDao campingCardDao;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired CardFixture cards;
    @Autowired EntityManager entityManager;

    Group club, bay;
    Userinfo office;
    Pedestal pedestal;
    CampingCard card;

    @BeforeEach
    void setUp() {
        club = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"), "ORDER CLUB", "Club");
        bay = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "BAY ORDERS", "Bay");
        groupManager.addAcceptanceAgreement(club, bay);
        pedestal = infrastructure.pedestal("HR-BAYORD-P001", bay, 2);
        pedestal.getSockets().get(0).setPitchNo("S7");
        pedestal.getSockets().get(1).setPitchNo("S8");
        office = new Userinfo("seasonoffice", "Office", "office@example.org");
        office.setGroup(groupService.fetchStaffGroup("HR RECEPTION"));
        office.getGroups().add(club);
        userinfoDao.saveAndFlush(office);
        card = campingCardDao.saveAndFlush(new CampingCard.Builder("7301", "HR-7301").build());
    }

    Order seasonalOrderOfANewGuest() {
        Order order = orderService.createOrderForNewGuest(office, new Guest.Builder(club, GuestType.SEASONAL,
                "Horvat").firstName("Ana").iban("HR1210010051863000160").trailerPlate("ZG 77 TR"));
        orderService.addItem(office, order.getId(), "HR-SEASON-PACKAGE", 1, bay.getId(), "s7", SEASON_START, null,
                null);
        orderService.addItem(office, order.getId(), "HR-TRANSIT-CARD", 1, null, null, null, null, card.getId());
        return order;
    }

    @Test
    void orderOfANewSeasonalResidentWaitsForTheErpAndIsHandedOverOnce() {
        Order order = orderService.submitOrder(office, seasonalOrderOfANewGuest().getId());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_ERP_ACCOUNT);
        assertThat(order.getOrderNumber()).matches("HR-\\d{6}");
        assertThat(order.getFixedAmount()).isEqualByComparingTo("395.00");
        assertThat(orderService.findOrdersAwaitingErpAccount(true)).containsExactly(order);

        assertThat(orderService.markOrdersExported(List.of(order.getId()))).isEqualTo(1);
        entityManager.clear();
        assertThat(orderService.findOrdersAwaitingErpAccount(true)).isEmpty();
        assertThat(orderService.findOrdersAwaitingErpAccount(false)).extracting(Order::getId)
                .containsExactly(order.getId());
    }

    @Test
    void erpNumbersActivateTheOrderIntoGuestAccountContractAndCard() {
        Order order = orderService.submitOrder(office, seasonalOrderOfANewGuest().getId());

        assertThat(orderService.enrichOrderWithErpNumbers(order.getId(), "P-5501", ACCOUNT)).isTrue();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.ACTIVE);
        Guest guest = order.getGuest();
        assertThat(guest.getErpPartnerNo()).isEqualTo("P-5501");
        assertThat(guest.getGuestType()).isEqualTo(GuestType.SEASONAL);
        assertThat(guest.getTrailerPlate()).isEqualTo("ZG 77 TR");
        BillingAccount account = billingAccountDao.findByErpAccountNo(ACCOUNT);
        assertThat(account.getGuest()).isEqualTo(guest);
        SeasonalContract contract = seasonalContractDao.findStartedContractsOfGuest(guest.getId(),
                SEASON_START.plusDays(1)).get(0);
        assertThat(contract.getPitchNo()).isEqualTo("S7");
        assertThat(contract.getEndDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(contract.getFixedFee()).isEqualByComparingTo("390.00");
        assertThat(contract.getPackageKwh()).isEqualByComparingTo("600");
        assertThat(contract.getTariff().getName()).isEqualTo("HR SEASONAL");
        assertThat(card.getGuest()).isEqualTo(guest);
        assertThat(card.getBillingAccountNo()).isEqualTo(ACCOUNT);
        assertThat(card.getValidFrom()).isEqualTo(SEASON_START);
        assertThat(card.validate(LocalDateTime.of(2026, 7, 1, 12, 0))).isEqualTo(CardStatus.VALID);
        assertThat(card.validate(LocalDateTime.of(2026, 10, 1, 12, 0))).isEqualTo(CardStatus.NOT_VALID_ANYMORE);
    }

    @Test
    void repeatedErpAnswerForAnActiveOrderIsSkipped() {
        Order order = orderService.submitOrder(office, seasonalOrderOfANewGuest().getId());
        orderService.enrichOrderWithErpNumbers(order.getId(), "P-5501", ACCOUNT);

        assertThat(orderService.enrichOrderWithErpNumbers(order.getId(), "P-9999", "400055559999")).isFalse();
        assertThat(order.getErpPartnerNo()).isEqualTo("P-5501");
    }

    @Test
    void erpAnswerForACancelledOrderIsIgnored() {
        Order order = orderService.submitOrder(office, seasonalOrderOfANewGuest().getId());
        orderService.cancelOrder(office, order.getId(), "season not wanted");

        assertThat(orderService.enrichOrderWithErpNumbers(order.getId(), "P-5501", ACCOUNT)).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(billingAccountDao.findByErpAccountNo(ACCOUNT)).isNull();
    }

    @Test
    void orderOfAGuestKnownToTheErpIsActivatedOnSubmit() {
        Guest guest = cards.guest("Novak", club);
        guest.setErpPartnerNo("P-5502");
        BillingAccount account = billingAccountDao.save(guest.openBillingAccount("400055550002", "Reception"));
        Order order = orderService.createOrderForGuest(office, guest.getId(), account.getId());
        orderService.addItem(office, order.getId(), "HR-SEASON-PACKAGE", 1, bay.getId(), "S8", SEASON_START,
                SEASON_START.plusMonths(3), null);

        orderService.submitOrder(office, order.getId());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.ACTIVE);
        assertThat(seasonalContractService.findContractsActiveIn(account.getId(), LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30))).hasSize(1);
        assertThat(seasonalContractService.findContractsActiveIn(account.getId(), LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31))).isEmpty();
    }

    @Test
    void pitchUnderContractIsNotLetTwice() {
        Order first = orderService.submitOrder(office, seasonalOrderOfANewGuest().getId());
        orderService.enrichOrderWithErpNumbers(first.getId(), "P-5501", ACCOUNT);
        Order second = orderService.createOrderForNewGuest(office, new Guest.Builder(club, GuestType.SEASONAL,
                "Rossi"));
        orderService.addItem(office, second.getId(), "HR-SEASON-PACKAGE", 1, bay.getId(), "S7",
                SEASON_START.plusMonths(1), null, null);
        orderService.submitOrder(office, second.getId());

        assertThatThrownBy(() -> orderService.enrichOrderWithErpNumbers(second.getId(), "P-5503", "400055550003"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Pitch S7");
    }

    @Test
    void contractTariffOverridesThePedestalOnTheContractsPitchOnly() {
        Order order = orderService.submitOrder(office, seasonalOrderOfANewGuest().getId());
        orderService.enrichOrderWithErpNumbers(order.getId(), "P-5501", ACCOUNT);
        Socket onPitch = pedestal.getSockets().get(0);
        Socket nextPitch = pedestal.getSockets().get(1);
        LocalDateTime july = LocalDateTime.of(2026, 7, 10, 20, 0);

        assertThat(tariffResolver.resolve(onPitch, card.getId(), july).source())
                .isEqualTo(TariffSource.SEASONAL_CONTRACT);
        assertThat(tariffResolver.resolve(onPitch, card.getId(), july).tariff().getName()).isEqualTo("HR SEASONAL");
        assertThat(tariffResolver.resolve(nextPitch, card.getId(), july).source())
                .isNotEqualTo(TariffSource.SEASONAL_CONTRACT);
        assertThat(tariffResolver.resolve(onPitch, card.getId(), LocalDateTime.of(2026, 10, 2, 10, 0)).source())
                .isNotEqualTo(TariffSource.SEASONAL_CONTRACT);
        assertThat(tariffResolver.resolve(onPitch, null, july).source()).isNotEqualTo(TariffSource.SEASONAL_CONTRACT);
    }

    @Test
    void correctedErpNumbersMoveToOrderGuestAccountAndCards() {
        Order order = orderService.submitOrder(office, seasonalOrderOfANewGuest().getId());
        orderService.enrichOrderWithErpNumbers(order.getId(), "P-5501", ACCOUNT);
        entityManager.flush();

        assertThat(orderService.correctErpNumbers("P-5501", ACCOUNT, "P-6601", "400066660001")).isEqualTo(1);
        assertThat(orderService.correctErpNumbers("P-0000", "400000000000", "P-6602", "400066660002")).isZero();

        entityManager.clear();
        Order corrected = orderService.findOrder(order.getId());
        assertThat(corrected.getErpPartnerNo()).isEqualTo("P-6601");
        assertThat(corrected.getErpAccountNo()).isEqualTo("400066660001");
        assertThat(guestDao.findById(corrected.getGuest().getId()).orElseThrow().getErpPartnerNo())
                .isEqualTo("P-6601");
        assertThat(billingAccountDao.findByErpAccountNo("400066660001")).isNotNull();
        assertThat(campingCardDao.findById(card.getId()).orElseThrow().getBillingAccountNo())
                .isEqualTo("400066660001");
    }

    @Test
    void ordersAreOnlyMadeInTheUsersIssuerScopeAndWithAValidCatalogue() {
        Group otherClub = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"), "OTHER ORDERS",
                "Other");
        Order order = orderService.createOrderForNewGuest(office, new Guest.Builder(club, GuestType.TRANSIT, "Kos"));

        assertThatThrownBy(() -> orderService.createOrderForNewGuest(office, new Guest.Builder(otherClub,
                GuestType.TRANSIT, "Kos"))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> orderService.createOrderForNewGuest(office, new Guest.Builder(bay,
                GuestType.TRANSIT, "Kos"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> orderService.addItem(office, order.getId(), "NO-SUCH-PRODUCT", 1, null, null, null,
                null, null)).isInstanceOf(java.util.NoSuchElementException.class);
        assertThatThrownBy(() -> orderService.addItem(office, order.getId(), "HR-SEASON-PACKAGE", 1, null, null,
                SEASON_START, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> orderService.addItem(office, order.getId(), "HR-SEASON-PACKAGE", 1, bay.getId(),
                null, null, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> orderService.addItem(office, order.getId(), "HR-HOUSE-CARD-SET", 1, null, null,
                null, null, card.getId())).isInstanceOf(IllegalArgumentException.class);
    }
}
