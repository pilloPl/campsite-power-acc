package io.pillopl.issuer;

import io.pillopl.card.CampingCard;
import io.pillopl.card.CardFixture;
import io.pillopl.consumption.ConsumptionRecord;
import io.pillopl.consumption.ExportStatus;
import io.pillopl.consumption.PaymentChannel;
import io.pillopl.consumption.RecordValidationState;
import io.pillopl.dto.consumption.ConsumptionRecordDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.guest.Guest;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Socket;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
import io.pillopl.persistence.dao.IssuerPartnerDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.pricing.PricingStatus;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class IssuerExchangeImportServiceIntegrationTest extends IntegrationTest {

    static final LocalDateTime PLUG_IN = LocalDateTime.of(2026, 7, 10, 18, 0);
    static final LocalDateTime PLUG_OUT = LocalDateTime.of(2026, 7, 11, 9, 0);

    @Autowired IssuerExchangeImportService issuerExchangeImportService;
    @Autowired IssuerPartnerDao issuerPartnerDao;
    @Autowired ConsumptionRecordDao consumptionRecordDao;
    @Autowired SocketDao socketDao;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;
    @Autowired CardFixture cards;

    IssuerPartner partner;
    CampingCard networkCard;
    Guest networkGuest;
    Socket partnerSocket;
    Socket networkSocket;

    @BeforeEach
    void setUp() {
        Group partnerIssuers = groupManager.createChildGroup(groupService.fetchIssuerGroup("HR ISSUERS"),
                "RECORD IMPORT CLUB", "Club");
        Group partnerSites = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"),
                "RECORD IMPORT CLUB CAMPSITES", "Club campsites");
        Group networkSite = groupManager.createChildGroup(groupService.fetchSiteGroup("HR SITES"),
                "RECORD IMPORT BAY", "Bay");
        partnerSocket = socket(infrastructure.pedestal("HR-CLUB-P001", partnerSites, 1).getSockets().get(0),
                "CLUB-P001-1");
        networkSocket = socket(infrastructure.pedestal("HR-BAYIMP-P001", networkSite, 1).getSockets().get(0),
                "BAY-P001-1");
        networkGuest = cards.guest("Horvat", groupService.fetchNetworkCardIssuerGroup());
        networkCard = cards.card("8101", networkGuest);

        partner = new IssuerPartner("RECORD CLUB", partnerIssuers, IssuerPartner.InterfaceVersion.V2,
                "http://club.test/api");
        partner.setSiteGroup(partnerSites);
        partner.setRecordImport(true);
        partner = issuerPartnerDao.saveAndFlush(partner);
    }

    @Test
    void recordOfANetworkCardAtThePartnersCampsiteIsImportedWithThePartnersPrice() {
        List<ConsumptionRecordDTO> accepted = importLines(line("CLUB-77", "8101", "CLUB-P001-1", "12.5", "4.375"));

        assertThat(accepted).hasSize(1);
        ConsumptionRecord record = consumptionRecordDao.findBySourceNameAndExternalRecordNo("RECORD CLUB", "CLUB-77");
        assertThat(record.getSupplySessionId()).isNull();
        assertThat(record.isImported()).isTrue();
        assertThat(record.getRecordNo()).startsWith("NA-20260711-");
        assertThat(record.getCardId()).isEqualTo(networkCard.getId());
        assertThat(record.getIssuerGroupId()).isEqualTo(networkGuest.getGroup().getId());
        assertThat(record.getSiteGroupId()).isEqualTo(partner.getSiteGroup().getId());
        assertThat(record.getPaymentChannel()).isEqualTo(PaymentChannel.CARD);
        assertThat(record.getSettlementFlags().isBillable()).isTrue();
        assertThat(record.getKwh()).isEqualByComparingTo("12.5");
        assertThat(record.getDurationSeconds()).isEqualTo(15 * 3600L);
        assertThat(record.getNights()).isEqualTo(1);
        assertThat(record.getPricingStatus()).isEqualTo(PricingStatus.PRICED);
        assertThat(record.getCost()).isEqualByComparingTo("4.38");
        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.NEW);
    }

    @Test
    void lineImportedBeforeIsAcceptedWithoutASecondRecord() {
        importLines(line("CLUB-77", "8101", "CLUB-P001-1", "12.5", "4.38"));

        List<ConsumptionRecordDTO> accepted = importLines(line("CLUB-77", "8101", "CLUB-P001-1", "12.5", "4.38"));

        assertThat(accepted).hasSize(1);
        assertThat(consumptionRecordDao.findAll()).filteredOn(r -> "RECORD CLUB".equals(r.getSourceName()))
                .hasSize(1);
    }

    @Test
    void recordFromASocketOutsideThePartnersCampsitesIsRefused() {
        assertThat(importLines(line("CLUB-78", "8101", "BAY-P001-1", "3", "1.00"))).isEmpty();
        assertThat(importLines(line("CLUB-79", "8101", "NO-SUCH-SOCKET", "3", "1.00"))).isEmpty();
        assertThat(consumptionRecordDao.findBySourceNameAndExternalRecordNo("RECORD CLUB", "CLUB-78")).isNull();
    }

    @Test
    void recordOfAnUnknownCardIsRefused() {
        assertThat(importLines(line("CLUB-80", "999999", "CLUB-P001-1", "3", "1.00"))).isEmpty();
    }

    @Test
    void lineWithoutRecordNumberCardSocketOrPlugOutIsRefused() {
        ConsumptionRecordDTO noPlugOut = new ConsumptionRecordDTO(null, "CLUB-81", null, "CLUB-P001-1", null, null,
                null, null, null, "8101", null, null, null, true, false, false, PLUG_IN, null, null, 1,
                BigDecimal.ONE, null, BigDecimal.ONE, "EUR", null, null, null, null, null, null, null, null);

        assertThat(importLines(line(null, "8101", "CLUB-P001-1", "3", "1.00"),
                line("CLUB-82", null, "CLUB-P001-1", "3", "1.00"),
                line("CLUB-83", "8101", " ", "3", "1.00"),
                noPlugOut)).isEmpty();
    }

    @Test
    void recordWithoutThePartnersCostWaitsForAReview() {
        importLines(line("CLUB-84", "8101", "CLUB-P001-1", "3", null));

        ConsumptionRecord record = consumptionRecordDao.findBySourceNameAndExternalRecordNo("RECORD CLUB", "CLUB-84");
        assertThat(record.getPricingStatus()).isEqualTo(PricingStatus.PRICING_FAILED);
        assertThat(record.getCost()).isNull();
        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.REVIEW);
    }

    @Test
    void implausibleImportedRecordIsSuspectAndNotPriced() {
        importLines(line("CLUB-85", "8101", "CLUB-P001-1", "900", "300.00"));

        ConsumptionRecord record = consumptionRecordDao.findBySourceNameAndExternalRecordNo("RECORD CLUB", "CLUB-85");
        assertThat(record.getValidationState()).isEqualTo(RecordValidationState.SUSPECT);
        assertThat(record.getPricingStatus()).isNull();
        assertThat(record.getExportStatus()).isEqualTo(ExportStatus.REVIEW);
    }

    @Test
    void partnerWithoutCampsitesCannotImportRecords() {
        IssuerPartner issuerOnly = issuerPartnerDao.saveAndFlush(new IssuerPartner("ISSUER ONLY",
                partner.getIssuerGroup(), IssuerPartner.InterfaceVersion.V1, "/tmp/issuer"));

        assertThatThrownBy(() -> issuerExchangeImportService.importConsumptionRecords(issuerOnly.getId(), List.of(),
                "ISSUER ONLY")).isInstanceOf(IllegalArgumentException.class);
    }

    private List<ConsumptionRecordDTO> importLines(ConsumptionRecordDTO... lines) {
        List<ConsumptionRecordDTO> accepted = issuerExchangeImportService.importConsumptionRecords(partner.getId(),
                List.of(lines), partner.getName());
        consumptionRecordDao.flush();
        return accepted;
    }

    private Socket socket(Socket socket, String code) {
        socket.setSocketCode(code);
        return socketDao.saveAndFlush(socket);
    }

    static ConsumptionRecordDTO line(String recordNo, String cardRfid, String socketCode, String kwh, String cost) {
        return new ConsumptionRecordDTO(null, recordNo, null, socketCode, null, null, "Club campsite", null, null,
                cardRfid, null, null, "CARD", true, false, false, PLUG_IN, PLUG_OUT, null, 1, new BigDecimal(kwh),
                null, cost != null ? new BigDecimal(cost) : null, "EUR", null, null, "Club tariff", null, null, null,
                null, null);
    }
}
