package io.pillopl.erp;

import io.pillopl.card.CampingCard;
import io.pillopl.config.SystemPropertyKey;
import io.pillopl.config.SystemPropertyService;
import io.pillopl.consumption.ConsumptionRecord;
import io.pillopl.consumption.ExportStatus;
import io.pillopl.consumption.PaymentChannel;
import io.pillopl.consumption.SettlementFlags;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.guest.Guest;
import io.pillopl.guest.GuestType;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.order.Order;
import io.pillopl.order.OrderService;
import io.pillopl.order.OrderStatus;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
import io.pillopl.persistence.dao.OrderDao;
import io.pillopl.persistence.dao.SupplySessionDao;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.pricing.PricingStatus;
import io.pillopl.supply.SupplySession;
import io.pillopl.test.IntegrationTest;
import io.pillopl.user.Userinfo;
import io.pillopl.util.DateService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The file exchange with the ERP on the database: partner requests out once, the ERP's numbers activate the orders,
 * corrections move the numbers; the monthly billing lines go out once and the ERP's booking answers come back.
 */
@Transactional
class ErpExchangeServiceIntegrationTest extends IntegrationTest {

    static final String ACCOUNT = "400088880001";
    static final LocalDateTime OCTOBER_RUN = LocalDateTime.of(2026, 10, 3, 3, 0);

    @Autowired ErpExchangeService erpExchangeService;
    @Autowired ErpBillingDataService erpBillingDataService;
    @Autowired SystemPropertyService systemPropertyService;
    @Autowired DateService dateService;
    @Autowired OrderService orderService;
    @Autowired OrderDao orderDao;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired UserinfoDao userinfoDao;
    @Autowired CampingCardDao campingCardDao;
    @Autowired ConsumptionRecordDao consumptionRecordDao;
    @Autowired SupplySessionDao supplySessionDao;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired EntityManager entityManager;

    @TempDir Path erpDirectory;
    Path outbound;
    Path inbound;
    Group bay;
    Order order;

    @BeforeEach
    void setUp() {
        outbound = erpDirectory.resolve("out");
        inbound = erpDirectory.resolve("in");
        systemPropertyService.setProperty(SystemPropertyKey.ErpOutboundDirectory, outbound.toString(), null);
        systemPropertyService.setProperty(SystemPropertyKey.ErpInboundDirectory, inbound.toString(), null);
        dateService.setClock(Clock.fixed(OCTOBER_RUN.atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()));

        Group club = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"), "EXCHANGE CLUB",
                "Club");
        bay = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "BAY EXCHANGE", "Bay");
        groupManager.addAcceptanceAgreement(club, bay);
        Userinfo office = new Userinfo("exchangeoffice", "Office", "exchange-office@example.org");
        office.setGroup(groupService.fetchStaffGroup("HR RECEPTION"));
        office.getGroups().add(club);
        userinfoDao.saveAndFlush(office);
        CampingCard card = campingCardDao.saveAndFlush(new CampingCard.Builder("7801", "HR-7801").build());
        order = orderService.createOrderForNewGuest(office, new Guest.Builder(club, GuestType.SEASONAL, "Babic")
                .firstName("Mia").organisationName("Mia; Sailing").iban("HR1210010051863000160"));
        orderService.addItem(office, order.getId(), "HR-SEASON-PACKAGE", 1, bay.getId(), "S9",
                LocalDate.of(2026, 4, 1), null, null);
        orderService.addItem(office, order.getId(), "HR-TRANSIT-CARD", 1, null, null, null, null, card.getId());
        orderService.submitOrder(office, order.getId());
    }

    @AfterTransaction
    void restoreConfiguration() {
        dateService.setClock(null);
        systemPropertyService.reloadSystemProperties();
    }

    @Test
    void ordersWaitingForTheErpAreHandedOverOnce() throws IOException {
        assertThat(erpExchangeService.exportPartnerRequests()).isEqualTo(1);
        assertThat(erpExchangeService.exportPartnerRequests()).isZero();

        Path file = outbound.resolve("partner-requests-20261003030000.csv");
        List<String> lines = Files.readAllLines(file);
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0)).startsWith("ORDER_NUMBER;GUEST_TYPE;NAME");
        assertThat(lines.get(1)).startsWith(order.getOrderNumber() + ";SEASONAL;Babic;Mia;Mia, Sailing;");
        assertThat(lines.get(1).split(";", -1)).hasSize(17);
        assertThat(files(outbound)).containsExactly("partner-requests-20261003030000.csv");
    }

    @Test
    void erpNumbersActivateTheOrderAndAFileWithABadLineGoesToError() throws IOException {
        answer("partner-answers-1.csv", "TYPE;ORDER;PARTNER;ACCOUNT",
                "ORDER;" + order.getOrderNumber() + ";P-7801;" + ACCOUNT,
                "ORDER;HR-999999;P-1;400000000001",
                "",
                "SOMETHING;else");

        assertThat(erpExchangeService.importPartnerAnswers()).isEqualTo(1);

        assertThat(orderDao.findByOrderNumber(order.getOrderNumber()).getStatus()).isEqualTo(OrderStatus.ACTIVE);
        assertThat(files(inbound.resolve("error"))).containsExactly("partner-answers-1.csv");
        assertThat(erpExchangeService.importPartnerAnswers()).isZero();
    }

    @Test
    void correctedNumbersMoveToTheOrderAndTheAnswerFileIsProcessed() throws IOException {
        orderService.enrichOrderWithErpNumbers(order.getId(), "P-7801", ACCOUNT);
        answer("partner-answers-2.csv", "TYPE;OLD_PARTNER;OLD_ACCOUNT;NEW_PARTNER;NEW_ACCOUNT",
                "CORRECTION;P-7801;" + ACCOUNT + ";P-7802;400088880002");

        assertThat(erpExchangeService.importPartnerAnswers()).isEqualTo(1);

        entityManager.clear();
        assertThat(orderDao.findByOrderNumber(order.getOrderNumber()).getErpAccountNo()).isEqualTo("400088880002");
        assertThat(files(inbound.resolve("processed"))).containsExactly("partner-answers-2.csv");
    }

    @Test
    void lastMonthIsBilledOnceAndTheErpBookingAnswersComeBack() throws IOException {
        orderService.enrichOrderWithErpNumbers(order.getId(), "P-7801", ACCOUNT);
        Pedestal pedestal = infrastructure.pedestal("HR-BAYEXC-P001", bay, 1);
        ConsumptionRecord september = record(pedestal, "650", LocalDateTime.of(2026, 9, 12, 9, 0));

        assertThat(erpExchangeService.exportBillingLines()).isEqualTo(2);
        assertThat(erpExchangeService.exportBillingLines()).isZero();

        List<String> lines = Files.readAllLines(outbound.resolve("billing-lines-20261003030000.csv"));
        assertThat(lines).hasSize(3);
        assertThat(lines.get(1)).startsWith("2026_09;P-7801;" + ACCOUNT + ";" + order.getOrderNumber()
                + ";ART-SEASON-FIX;");
        assertThat(lines.get(1)).endsWith(";Pitch power of your organisation");
        assertThat(lines.get(2)).contains(";ART-SEASON-KWH;").contains(";50.0000;0.3800;19.00;EUR;A1;");

        answer("billing-answers-1.csv", "PERIOD;ACCOUNT;ARTICLE;RESULT;TEXT",
                "2026_09;" + ACCOUNT + ";ART-SEASON-FIX;OK;",
                "2026_09;" + ACCOUNT + ";ART-SEASON-KWH;ERROR;article blocked",
                "2026_09;" + ACCOUNT + ";ART-UNKNOWN;OK;",
                "2026_09;" + ACCOUNT + ";ART-SEASON-FIX;MAYBE;");

        assertThat(erpExchangeService.importBillingAnswers()).isEqualTo(2);

        assertThat(erpBillingDataService.findErpBillingLines("2026_09")).extracting(ErpBillingLine::getExportStatus)
                .containsExactly(ExportStatus.PROCESSED, ExportStatus.ERROR);
        entityManager.flush();
        entityManager.clear();
        assertThat(consumptionRecordDao.findById(september.getId()).orElseThrow().getExportStatus())
                .isEqualTo(ExportStatus.ERROR);
        assertThat(files(inbound.resolve("error"))).containsExactly("billing-answers-1.csv");
    }

    private ConsumptionRecord record(Pedestal pedestal, String kwh, LocalDateTime plugOut) {
        SupplySession session = new SupplySession();
        session.setSocket(pedestal.getSockets().get(0));
        session.setTransactionId(780_001);
        session.setCreateDate(plugOut.minusHours(5));
        session.setMaxCurrentA(16);
        supplySessionDao.save(session);

        ConsumptionRecord record = new ConsumptionRecord();
        record.setRecordNo("EXC-" + session.getTransactionId());
        record.setSupplySessionId(session.getId());
        record.setSiteGroupId(bay.getId());
        record.setPitchNo("S9");
        record.setBillingAccountNo(ACCOUNT);
        record.setPaymentChannel(PaymentChannel.CARD);
        record.setSettlementFlags(new SettlementFlags(true, false, false));
        record.setKwh(new BigDecimal(kwh));
        record.setPlugIn(plugOut.minusHours(5));
        record.setPlugOut(plugOut);
        record.setNights(1);
        record.setTariffId(4L);
        record.setPricingStatus(PricingStatus.PRICED);
        record.setCost(new BigDecimal(kwh).multiply(new BigDecimal("0.38")));
        record.setCreateDate(plugOut);
        return consumptionRecordDao.saveAndFlush(record);
    }

    private void answer(String name, String... lines) throws IOException {
        Files.createDirectories(inbound);
        Files.write(inbound.resolve(name), List.of(lines));
    }

    private static List<String> files(Path directory) throws IOException {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.map(path -> path.getFileName().toString()).sorted().toList();
        }
    }
}
