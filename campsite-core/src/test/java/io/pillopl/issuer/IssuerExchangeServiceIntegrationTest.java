package io.pillopl.issuer;

import io.pillopl.card.CampingCard;
import io.pillopl.card.CardFixture;
import io.pillopl.card.RfidConverter;
import io.pillopl.consumption.ConsumptionRecord;
import io.pillopl.consumption.ExportStatus;
import io.pillopl.consumption.PaymentChannel;
import io.pillopl.consumption.SettlementFlags;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.group.attribute.WhitelistFilter;
import io.pillopl.group.attribute.WhitelistFilterType;
import io.pillopl.guest.Guest;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Socket;
import io.pillopl.issuer.IssuerSyncStatus.Direction;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
import io.pillopl.persistence.dao.IssuerPartnerDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.persistence.dao.SupplySessionDao;
import io.pillopl.persistence.dao.WhitelistFilterDao;
import io.pillopl.supply.SupplySession;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Whole runs of the exchange over the file interface (v1) in a temporary exchange directory. The service commits
 * its own transactions, so every test works with groups and partners of its own and retires its partners after.
 */
class IssuerExchangeServiceIntegrationTest extends IntegrationTest {

    static final AtomicInteger TRANSACTIONS = new AtomicInteger(910_000);
    static final AtomicInteger UIDS = new AtomicInteger(0x0A100000);

    @TempDir
    Path exchangeDirectory;

    @Autowired IssuerExchangeService issuerExchangeService;
    @Autowired IssuerPartnerDao issuerPartnerDao;
    @Autowired CampingCardDao campingCardDao;
    @Autowired ConsumptionRecordDao consumptionRecordDao;
    @Autowired SupplySessionDao supplySessionDao;
    @Autowired SocketDao socketDao;
    @Autowired WhitelistFilterDao whitelistFilterDao;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired CardFixture cards;
    @Autowired TransactionTemplate transactionTemplate;

    final List<Long> partnerIds = new ArrayList<>();
    final List<Long> networkCardIds = new ArrayList<>();
    String tag;
    Group clubIssuers;
    Group clubSites;
    Guest clubGuest;
    Socket networkSocket;
    Socket clubSocket;

    @BeforeEach
    void setUp() {
        tag = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        transactionTemplate.executeWithoutResult(transaction -> tree());
    }

    private void tree() {
        clubIssuers = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"), "XCH CLUB " + tag,
                "Club");
        clubSites = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "XCH CLUB SITES " + tag,
                "Club sites");
        Group bay = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"), "XCH BAY " + tag, "Bay");
        networkSocket = infrastructure.pedestal("HR-XB" + tag, bay, 1).getSockets().get(0);
        clubSocket = infrastructure.pedestal("HR-XC" + tag, clubSites, 1).getSockets().get(0);
        clubSocket.setSocketCode("XC-" + tag + "-1");
        clubSocket = socketDao.saveAndFlush(clubSocket);
        networkSocket.setSocketCode("XB-" + tag + "-1");
        networkSocket = socketDao.saveAndFlush(networkSocket);
        clubGuest = cards.guest("CLUB MEMBERS " + tag, clubIssuers);
    }

    @AfterEach
    void retirePartners() {
        for (Long id : partnerIds) {
            IssuerPartner partner = issuerPartnerDao.findById(id).orElseThrow();
            partner.setDeleted(true);
            issuerPartnerDao.save(partner);
        }
        // network cards would show up in the card lists of other tests
        for (Long id : networkCardIds) {
            CampingCard card = campingCardDao.findById(id).orElseThrow();
            card.setDeleted(true);
            campingCardDao.save(card);
        }
    }

    private CampingCard networkCard(CampingCard card) {
        networkCardIds.add(card.getId());
        return card;
    }

    @Test
    void cardLinesOfThePartnerAreImportedLineByLine() throws IOException {
        IssuerPartner club = partner("club", p -> p.setCardImport(true));
        incoming("club", "cards-in", "cards.csv", IssuerExchangeV1.CARD_HEADER,
                hex() + ";CC-1;ADD;2026-07-01 09:30;770012345678;2026-12-31",
                hex() + ";CC-2;MODIFY;2026-07-01 09:30;770012345678;");

        issuerExchangeService.exchangeAll();

        IssuerSyncStatus status = status(club, Direction.CARD_IMPORT);
        assertThat(status.getStatus()).isEqualTo(IssuerSyncStatus.Status.OK);
        assertThat(status.getItemsAccepted()).isEqualTo(1);
        assertThat(status.getItemsRejected()).isEqualTo(1);
        assertThat(status.getMessage()).endsWith("card not found");
        assertThat(exchangeDirectory.resolve("club/cards-in/done/cards.csv")).exists();
    }

    @Test
    void recordsOfThePartnersMembersAreExportedAndTransferred() throws IOException {
        IssuerPartner club = partner("club", p -> p.setRecordExport(true));
        ConsumptionRecord ofMember = record(cards.card(nextDecimal(), clubGuest));
        ConsumptionRecord ofNetworkGuest = record(networkCard(cards.card(nextDecimal(),
                cards.guest("Horvat", groupService.fetchNetworkCardIssuerGroup()))));

        issuerExchangeService.exchangeAll();

        assertThat(consumptionRecordDao.findById(ofMember.getId()).orElseThrow().getExportStatus())
                .isEqualTo(ExportStatus.TRANSFERRED);
        assertThat(consumptionRecordDao.findById(ofNetworkGuest.getId()).orElseThrow().getExportStatus())
                .isEqualTo(ExportStatus.NEW);
        assertThat(onlyFile("club/records-out")).anyMatch(line -> line.startsWith(ofMember.getRecordNo() + ";"));
        assertThat(status(club, Direction.RECORD_EXPORT).getItemsAccepted()).isEqualTo(1);
    }

    @Test
    void unreachablePartnerKeepsItsRecordsWaiting() throws IOException {
        Files.writeString(exchangeDirectory.resolve("blocked"), "not a directory");
        IssuerPartner club = partner("blocked", p -> p.setRecordExport(true));
        ConsumptionRecord ofMember = record(cards.card(nextDecimal(), clubGuest));

        issuerExchangeService.exchangeAll();

        assertThat(consumptionRecordDao.findById(ofMember.getId()).orElseThrow().getExportStatus())
                .isEqualTo(ExportStatus.NEW);
        IssuerSyncStatus status = status(club, Direction.RECORD_EXPORT);
        assertThat(status.getStatus()).isEqualTo(IssuerSyncStatus.Status.ERROR);
        assertThat(status.getLastSuccess()).isNull();
    }

    @Test
    void recordsOfThePartnersCampsitesAreImported() throws IOException {
        IssuerPartner club = partner("club", p -> {
            p.setSiteGroup(clubSites);
            p.setRecordImport(true);
        });
        CampingCard networkCard = networkCard(cards.card(nextDecimal(), cards.guest("Babic",
                groupService.fetchNetworkCardIssuerGroup())));
        incoming("club", "records-in", "records.csv", IssuerExchangeV1.RECORD_HEADER,
                "CLUB-" + tag + ";Club;" + clubSocket.getSocketCode() + ";7;" + networkCard.getCardRfid()
                        + ";2026-07-10T18:00;2026-07-11T09:00;;1;12.5;4.38;EUR;Club",
                "CLUB-" + tag + "-X;Club;" + networkSocket.getSocketCode() + ";7;" + networkCard.getCardRfid()
                        + ";2026-07-10T18:00;2026-07-11T09:00;;1;12.5;4.38;EUR;Club");

        issuerExchangeService.exchangeAll();

        ConsumptionRecord imported = consumptionRecordDao.findBySourceNameAndExternalRecordNo(club.getName(),
                "CLUB-" + tag);
        assertThat(imported.getCardId()).isEqualTo(networkCard.getId());
        assertThat(imported.getExportStatus()).isEqualTo(ExportStatus.NEW);
        IssuerSyncStatus status = status(club, Direction.RECORD_IMPORT);
        assertThat(status.getItemsAccepted()).isEqualTo(1);
        assertThat(status.getItemsRejected()).isEqualTo(1);
    }

    @Test
    void networkCardListGoesToEveryPartnerItsFiltersLetThrough() throws IOException {
        IssuerPartner club = partner("club", p -> {
            p.setSiteGroup(clubSites);
            p.setCardExport(true);
        });
        IssuerPartner alliance = partner("alliance", p -> {
            p.setSiteGroup(clubSites);
            p.setCardExport(true);
        });
        Group networkChild = transactionTemplate.execute(transaction -> groupManager.createChildGroup(
                groupService.fetchNetworkCardIssuerGroup(), "XCH NETWORK SEASON " + tag, "Season"));
        WhitelistFilter notForAlliance = new WhitelistFilter(false, WhitelistFilterType.EXPORT);
        notForAlliance.setGroup(networkChild);
        notForAlliance.setIssuerPartner(alliance);
        whitelistFilterDao.saveAndFlush(notForAlliance);
        CampingCard seasonCard = networkCard(cards.card(nextDecimal(), "S" + tag,
                cards.guest("Season " + tag, networkChild), LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(30)));

        issuerExchangeService.exchangeAll();

        String line = RfidConverter.decimalToHexBigEndian(seasonCard.getCardRfid()) + ";S" + tag + ";ADD;";
        assertThat(onlyFile("club/cards-out")).anyMatch(l -> l.startsWith(line));
        // nothing let through: no file for the alliance
        assertThat(allLines("alliance/cards-out")).noneMatch(l -> l.startsWith(line));
        CampingCard exported = campingCardDao.findById(seasonCard.getId()).orElseThrow();
        assertThat(exported.getPartnerSyncState()).isEqualTo(PartnerSyncState.NOT);
        assertThat(exported.getTargetPartnerSyncState()).isEqualTo(PartnerSyncState.ADD);
        assertThat(status(club, Direction.CARD_EXPORT).getStatus()).isEqualTo(IssuerSyncStatus.Status.OK);
    }

    @Test
    void cardsStayWaitingWhenOnePartnerDidNotGetTheList() throws IOException {
        partner("club", p -> {
            p.setSiteGroup(clubSites);
            p.setCardExport(true);
        });
        Files.writeString(exchangeDirectory.resolve("blocked"), "not a directory");
        IssuerPartner blocked = partner("blocked", p -> {
            p.setSiteGroup(clubSites);
            p.setCardExport(true);
        });
        CampingCard networkCard = networkCard(cards.card(nextDecimal(), cards.guest("Kovac " + tag,
                groupService.fetchNetworkCardIssuerGroup())));

        issuerExchangeService.exchangeAll();

        assertThat(campingCardDao.findById(networkCard.getId()).orElseThrow().getPartnerSyncState())
                .isEqualTo(PartnerSyncState.ADD);
        assertThat(status(blocked, Direction.CARD_EXPORT).getStatus()).isEqualTo(IssuerSyncStatus.Status.ERROR);
    }

    @Test
    void inactivePartnerIsLeftOut() throws IOException {
        IssuerPartner club = partner("club", p -> {
            p.setCardImport(true);
            p.setActive(false);
        });
        incoming("club", "cards-in", "cards.csv", IssuerExchangeV1.CARD_HEADER,
                hex() + ";CC-1;ADD;2026-07-01 09:30;770012345678;");

        issuerExchangeService.exchangeAll();

        assertThat(exchangeDirectory.resolve("club/cards-in/cards.csv")).exists();
        Integer statuses = transactionTemplate.execute(s ->
                issuerPartnerDao.findById(club.getId()).orElseThrow().getSyncStatuses().size());
        assertThat(statuses).isZero();
    }

    private IssuerPartner partner(String directory, java.util.function.Consumer<IssuerPartner> setup) {
        IssuerPartner partner = new IssuerPartner("XCH " + directory.toUpperCase() + " " + tag, clubIssuers,
                IssuerPartner.InterfaceVersion.V1, exchangeDirectory.resolve(directory).toString());
        setup.accept(partner);
        partner = issuerPartnerDao.saveAndFlush(partner);
        partnerIds.add(partner.getId());
        return partner;
    }

    private IssuerSyncStatus status(IssuerPartner partner, Direction direction) {
        return transactionTemplate.execute(s -> {
            IssuerPartner reloaded = issuerPartnerDao.findById(partner.getId()).orElseThrow();
            return reloaded.getSyncStatuses().stream().filter(st -> st.getDirection() == direction).findFirst()
                    .orElseThrow();
        });
    }

    private ConsumptionRecord record(CampingCard card) {
        SupplySession session = new SupplySession();
        session.setSocket(networkSocket);
        session.setTransactionId(TRANSACTIONS.incrementAndGet());
        session.setCreateDate(LocalDateTime.now().minusHours(3));
        session.setMaxCurrentA(16);
        supplySessionDao.save(session);

        ConsumptionRecord record = new ConsumptionRecord();
        record.setRecordNo("XCH-" + session.getTransactionId());
        record.setSupplySessionId(session.getId());
        record.setSocketId(networkSocket.getId());
        record.setSocketCode(networkSocket.getSocketCode());
        record.setSiteGroupId(networkSocket.getPedestal().getGroup().getId());
        record.setCardId(card.getId());
        record.setCardRfid(card.getCardRfid());
        record.setGuestId(card.getGuest().getId());
        record.setIssuerGroupId(card.getGuest().getGroup().getId());
        record.setPaymentChannel(PaymentChannel.CARD);
        record.setSettlementFlags(new SettlementFlags(true, false, false));
        record.setKwh(new BigDecimal("6.5"));
        record.setPlugIn(LocalDateTime.now().minusHours(3));
        record.setPlugOut(LocalDateTime.now().minusHours(1));
        record.setDurationSeconds(7200L);
        record.setNights(1);
        record.setCost(new BigDecimal("2.28"));
        record.setCurrency("EUR");
        record.setCreateDate(LocalDateTime.now().minusHours(1));
        return consumptionRecordDao.saveAndFlush(record);
    }

    private void incoming(String partnerDirectory, String directory, String name, String... lines)
            throws IOException {
        Path dir = Files.createDirectories(exchangeDirectory.resolve(partnerDirectory).resolve(directory));
        Files.write(dir.resolve(name), List.of(lines), StandardCharsets.UTF_8);
    }

    private List<String> onlyFile(String directory) throws IOException {
        try (Stream<Path> files = Files.list(exchangeDirectory.resolve(directory))) {
            List<Path> found = files.toList();
            assertThat(found).hasSize(1);
            return Files.readAllLines(found.get(0));
        }
    }

    private List<String> allLines(String directory) throws IOException {
        Path dir = exchangeDirectory.resolve(directory);
        List<String> lines = new ArrayList<>();
        if (Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                for (Path file : files.toList()) {
                    lines.addAll(Files.readAllLines(file));
                }
            }
        }
        return lines;
    }

    private static String hex() {
        return Integer.toHexString(UIDS.incrementAndGet()).toUpperCase();
    }

    private static String nextDecimal() {
        return RfidConverter.hexBigEndianToDecimal(hex());
    }
}
