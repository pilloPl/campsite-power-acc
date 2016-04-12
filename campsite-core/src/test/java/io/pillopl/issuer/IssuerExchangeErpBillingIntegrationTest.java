package io.pillopl.issuer;

import io.pillopl.card.CampingCard;
import io.pillopl.card.CardFixture;
import io.pillopl.consumption.ConsumptionRecord;
import io.pillopl.consumption.ExportStatus;
import io.pillopl.consumption.PaymentChannel;
import io.pillopl.consumption.SettlementFlags;
import io.pillopl.dto.consumption.ConsumptionRecordDTO;
import io.pillopl.erp.ErpBillingDataService;
import io.pillopl.erp.ErpBillingLine;
import io.pillopl.erp.ErpBillingLine.LineType;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.guest.BillingAccount;
import io.pillopl.guest.Guest;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.BillingAccountDao;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
import io.pillopl.persistence.dao.IssuerPartnerDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.persistence.dao.SupplySessionDao;
import io.pillopl.pricing.PricingStatus;
import io.pillopl.supply.SupplySession;
import io.pillopl.test.IntegrationTest;
import io.pillopl.util.DateService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The two exports that take consumption records, the issuer exchange (records of a partner's members go to the
 * partner) and the monthly ERP billing (records of the network's billing accounts), on one data set and in both job
 * orders. Every record has exactly one owner, whichever job runs first:
 * <ul>
 *     <li>a network guest's own record at a network pedestal is billed by the ERP;</li>
 *     <li>a network guest's records imported from the partner's campsites are billed by the ERP like own records,
 *     also when the partner's pedestal is settled by its campsite (the network pays the partner);</li>
 *     <li>a partner member's record at a network pedestal goes to the partner, even when the member's card carries a
 *     number that is also the network guest's billing account.</li>
 * </ul>
 */
@Transactional
class IssuerExchangeErpBillingIntegrationTest extends IntegrationTest {

    static final LocalDateTime PLUG_IN = LocalDateTime.of(2031, 3, 5, 18, 0);
    static final LocalDateTime PLUG_OUT = LocalDateTime.of(2031, 3, 6, 9, 0);
    static final LocalDateTime RUN = LocalDateTime.of(2031, 4, 4, 3, 0);
    static final String NETWORK_ACCOUNT = "400031310001";
    static final AtomicInteger TRANSACTIONS = new AtomicInteger(313_000);

    enum JobOrder { ISSUER_EXCHANGE_FIRST, ERP_BILLING_FIRST }

    @TempDir Path exchangeDirectory;

    @Autowired IssuerExchangeService issuerExchangeService;
    @Autowired IssuerExchangeImportService issuerExchangeImportService;
    @Autowired ErpBillingDataService erpBillingDataService;
    @Autowired IssuerPartnerDao issuerPartnerDao;
    @Autowired ConsumptionRecordDao consumptionRecordDao;
    @Autowired SupplySessionDao supplySessionDao;
    @Autowired BillingAccountDao billingAccountDao;
    @Autowired SocketDao socketDao;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired CardFixture cards;
    @Autowired DateService dateService;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbcTemplate;

    IssuerPartner partner;
    Socket networkSocket;
    CampingCard networkCard;
    CampingCard memberCard;

    @BeforeEach
    void setUp() {
        dateService.setClock(Clock.fixed(RUN.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));
        Group partnerIssuers = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"),
                "JOINT CLUB", "Club");
        Group partnerSites = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"),
                "JOINT CLUB CAMPSITES", "Club campsites");
        Group networkSite = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "JOINT BAY",
                "Bay");
        networkSocket = socket(infrastructure.pedestal("HR-JOINT-P001", networkSite, 1), "JOINT-BAY-1", false);
        socket(infrastructure.pedestal("HR-JOINT-C001", partnerSites, 1), "JOINT-CLUB-1", false);
        socket(infrastructure.pedestal("HR-JOINT-C002", partnerSites, 1), "JOINT-CLUB-2", true);

        Guest networkGuest = cards.guest("Horvat", groupService.fetchNetworkCardIssuerGroup());
        billingAccountDao.saveAndFlush(new BillingAccount(networkGuest, NETWORK_ACCOUNT, "Horvat"));
        networkCard = cards.card("3131001", networkGuest);
        networkCard.setBillingAccountNo(NETWORK_ACCOUNT);
        // the partner numbers its members itself: this one collides with the network account
        memberCard = cards.card("3131002", cards.guest("Club member", partnerIssuers));
        memberCard.setBillingAccountNo(NETWORK_ACCOUNT);

        partner = new IssuerPartner("JOINT CLUB", partnerIssuers, IssuerPartner.InterfaceVersion.V1,
                exchangeDirectory.resolve("club").toString());
        partner.setSiteGroup(partnerSites);
        partner.setRecordImport(true);
        partner.setRecordExport(true);
        partner = issuerPartnerDao.saveAndFlush(partner);
    }

    @AfterEach
    void resetClock() {
        dateService.setClock(null);
    }

    @ParameterizedTest
    @EnumSource(JobOrder.class)
    void everyRecordHasExactlyOneExportWhicheverJobRunsFirst(JobOrder order) {
        ConsumptionRecord own = ownRecord(networkCard, "20", "9.00");
        ConsumptionRecord member = ownRecord(memberCard, "10", "4.50");
        issuerExchangeImportService.importConsumptionRecords(partner.getId(), List.of(
                line("JC-1", "JOINT-CLUB-1", "12.5", "4.38"),
                line("JC-2", "JOINT-CLUB-2", "6", "3.00")), partner.getName());
        ConsumptionRecord imported = consumptionRecordDao.findBySourceNameAndExternalRecordNo("JOINT CLUB", "JC-1");
        ConsumptionRecord importedSettledByCampsite =
                consumptionRecordDao.findBySourceNameAndExternalRecordNo("JOINT CLUB", "JC-2");
        assertThat(importedSettledByCampsite.getSettlementFlags().isSettledByCampsite()).isFalse();
        consumptionRecordDao.flush();

        List<ErpBillingLine> lines;
        if (order == JobOrder.ISSUER_EXCHANGE_FIRST) {
            issuerExchangeService.exportConsumptionRecords(partner);
            lines = erpBillingDataService.createErpBillingLines(3, 2031);
        } else {
            lines = erpBillingDataService.createErpBillingLines(3, 2031);
            issuerExchangeService.exportConsumptionRecords(partner);
        }

        assertThat(lines).filteredOn(line -> NETWORK_ACCOUNT.equals(line.getBillingAccountNo()))
                .extracting(ErpBillingLine::getLineType, ErpBillingLine::getQuantity, ErpBillingLine::getAmount)
                .containsExactly(tuple(LineType.POWER, new BigDecimal("38.5000"), new BigDecimal("16.38")));
        assertThat(owner(own)).isEqualTo(tuple(ExportStatus.WORK, "2031_03"));
        assertThat(owner(imported)).isEqualTo(tuple(ExportStatus.WORK, "2031_03"));
        assertThat(owner(importedSettledByCampsite)).isEqualTo(tuple(ExportStatus.WORK, "2031_03"));
        assertThat(owner(member)).isEqualTo(tuple(ExportStatus.TRANSFERRED, null));
    }

    private org.assertj.core.groups.Tuple owner(ConsumptionRecord record) {
        entityManager.flush();
        entityManager.clear();
        return jdbcTemplate.queryForObject("SELECT EXPORT_STATUS, ERP_BILLING_PERIOD FROM CONSUMPTION_RECORD"
                + " WHERE ID = ?", (rs, rowNum) -> tuple(ExportStatus.valueOf(rs.getString(1)), rs.getString(2)),
                record.getId());
    }

    private Socket socket(Pedestal pedestal, String code, boolean settledByCampsite) {
        pedestal.setSettledByCampsite(settledByCampsite);
        Socket socket = pedestal.getSockets().get(0);
        socket.setSocketCode(code);
        return socketDao.saveAndFlush(socket);
    }

    private ConsumptionRecord ownRecord(CampingCard card, String kwh, String cost) {
        SupplySession session = new SupplySession();
        session.setSocket(networkSocket);
        session.setTransactionId(TRANSACTIONS.incrementAndGet());
        session.setCreateDate(PLUG_IN);
        session.setMaxCurrentA(16);
        supplySessionDao.save(session);

        ConsumptionRecord record = new ConsumptionRecord();
        record.setRecordNo("JOINT-" + session.getTransactionId());
        record.setSupplySessionId(session.getId());
        record.setSocketId(networkSocket.getId());
        record.setSocketCode(networkSocket.getSocketCode());
        record.setSiteGroupId(networkSocket.getPedestal().getGroup().getId());
        record.setCardId(card.getId());
        record.setCardRfid(card.getCardRfid());
        record.setBillingAccountNo(card.getBillingAccountNo());
        record.setGuestId(card.getGuest().getId());
        record.setIssuerGroupId(card.getGuest().getGroup().getId());
        record.setPaymentChannel(PaymentChannel.CARD);
        record.setSettlementFlags(SettlementFlags.forCampingCard(true, false));
        record.setKwh(new BigDecimal(kwh));
        record.setPlugIn(PLUG_IN);
        record.setPlugOut(PLUG_OUT);
        record.setNights(1);
        record.setTariffId(1L);
        record.setPricingStatus(PricingStatus.PRICED);
        record.setCost(new BigDecimal(cost));
        record.setCurrency("EUR");
        record.setCreateDate(PLUG_OUT);
        return consumptionRecordDao.saveAndFlush(record);
    }

    private ConsumptionRecordDTO line(String recordNo, String socketCode, String kwh, String cost) {
        return new ConsumptionRecordDTO(null, recordNo, null, socketCode, null, null, "Club campsite", null, null,
                networkCard.getCardRfid(), null, null, "CARD", true, false, false, PLUG_IN, PLUG_OUT, null, 1,
                new BigDecimal(kwh), null, new BigDecimal(cost), "EUR", null, null, "Club tariff", null, null, null,
                null, null);
    }
}
