package io.pillopl.persistence.dao.impl;

import io.pillopl.consumption.ConsumptionRecord;
import io.pillopl.consumption.ExportStatus;
import io.pillopl.consumption.PaymentChannel;
import io.pillopl.consumption.RecordValidationState;
import io.pillopl.group.Group;
import io.pillopl.group.GroupManager;
import io.pillopl.group.GroupService;
import io.pillopl.infrastructure.InfrastructureFixture;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.persistence.dao.ConsumptionRecordDao;
import io.pillopl.supply.SupplySession;
import io.pillopl.persistence.dao.SupplySessionDao;
import io.pillopl.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Visibility of consumption records: the operator sees the records of the pedestals in his SITE subtree, the issuer
 * the records of his guests in his ISSUER subtree, wherever they supplied.
 */
@Transactional
class ConsumptionRecordDaoIntegrationTest extends IntegrationTest {

    static final LocalDateTime DAY = LocalDateTime.of(2026, 7, 10, 0, 0);
    static final AtomicInteger TRANSACTIONS = new AtomicInteger(880_000);

    @Autowired ConsumptionRecordDao consumptionRecordDao;
    @Autowired SupplySessionDao supplySessionDao;
    @Autowired GroupService groupService;
    @Autowired GroupManager groupManager;
    @Autowired InfrastructureFixture infrastructure;

    Group sites;
    Group bay;
    Group bayNorth;
    Group lake;
    Group club;
    Group clubJunior;
    Group otherClub;
    Pedestal pedestal;

    @BeforeEach
    void setUp() {
        sites = groupService.fetchSiteGroup("HR SITES");
        bay = groupManager.createChildGroup(sites, "BAY VISIBILITY", "Bay");
        bayNorth = groupManager.createChildGroup(bay, "BAY VISIBILITY NORTH", "Bay north");
        lake = groupManager.createChildGroup(sites, "LAKE VISIBILITY", "Lake");
        Group issuers = groupService.fetchIssuerGroup("HR ISSUERS");
        club = groupManager.createChildGroup(issuers, "VISIBILITY CLUB", "Club");
        clubJunior = groupManager.createChildGroup(club, "VISIBILITY CLUB JUNIOR", "Club junior");
        otherClub = groupManager.createChildGroup(issuers, "VISIBILITY OTHER CLUB", "Other club");
        pedestal = infrastructure.pedestal("HR-VIS-P001", bay, 1);
    }

    @Test
    void operatorSeesTheRecordsOfThePedestalsInHisSiteSubtree() {
        ConsumptionRecord atBay = record("BAY-1", bay, otherClub, DAY.plusHours(10));
        ConsumptionRecord atBayNorth = record("BAY-2", bayNorth, otherClub, DAY.plusHours(11));
        record("LAKE-1", lake, otherClub, DAY.plusHours(12));

        assertThat(consumptionRecordDao.findConsumptionRecords(DAY, DAY.plusDays(1), bay.getId(), null))
                .containsExactly(atBayNorth, atBay);
    }

    @Test
    void issuerSeesTheRecordsOfHisGuestsAtEveryCampsite() {
        ConsumptionRecord atLake = record("LAKE-1", lake, clubJunior, DAY.plusHours(12));
        ConsumptionRecord atBay = record("BAY-1", bay, club, DAY.plusHours(10));
        record("BAY-2", bay, otherClub, DAY.plusHours(11));
        record("BAY-3", bay, null, DAY.plusHours(13));

        assertThat(consumptionRecordDao.findConsumptionRecords(DAY, DAY.plusDays(1), null, club.getId()))
                .containsExactly(atLake, atBay);
    }

    @Test
    void recordSeenFromBothSidesComesOnce() {
        ConsumptionRecord both = record("BAY-1", bay, club, DAY.plusHours(10));
        ConsumptionRecord issuerOnly = record("LAKE-1", lake, club, DAY.plusHours(9));
        ConsumptionRecord siteOnly = record("BAY-2", bay, otherClub, DAY.plusHours(8));

        assertThat(consumptionRecordDao.findConsumptionRecords(DAY, DAY.plusDays(1), bay.getId(), club.getId()))
                .containsExactly(both, issuerOnly, siteOnly);
    }

    @Test
    void onlyRecordsWithPlugOutInThePeriodAreFound() {
        record("BAY-1", bay, club, DAY.minusMinutes(1));
        ConsumptionRecord first = record("BAY-2", bay, club, DAY);
        ConsumptionRecord last = record("BAY-3", bay, club, DAY.plusDays(1));
        record("BAY-4", bay, club, DAY.plusDays(1).plusSeconds(1));

        assertThat(consumptionRecordDao.findConsumptionRecords(DAY, DAY.plusDays(1), bay.getId(), null))
                .containsExactly(last, first);
    }

    @Test
    void userWithoutGroupsSeesNothing() {
        record("BAY-1", bay, club, DAY.plusHours(10));

        assertThat(consumptionRecordDao.findConsumptionRecords(DAY, DAY.plusDays(1), null, null)).isEmpty();
    }

    @Test
    void consolePageListsOneValidationStateAPageAtATime() {
        ConsumptionRecord suspect1 = record("BAY-1", bay, club, DAY.plusHours(10));
        suspect1.markSuspect("too much");
        ConsumptionRecord suspect2 = record("BAY-2", bay, club, DAY.plusHours(11));
        suspect2.markSuspect("too much");
        record("BAY-3", bay, club, DAY.plusHours(12));
        consumptionRecordDao.flush();

        assertThat(consumptionRecordDao.findConsumptionRecords(DAY, DAY.plusDays(1), bay.getId(), club.getId(),
                RecordValidationState.SUSPECT, 1, 1)).containsExactly(suspect2);
        assertThat(consumptionRecordDao.findConsumptionRecords(DAY, DAY.plusDays(1), bay.getId(), club.getId(),
                RecordValidationState.SUSPECT, 2, 1)).containsExactly(suspect1);
        assertThat(consumptionRecordDao.findConsumptionRecords(DAY, DAY.plusDays(1), bay.getId(), club.getId(),
                null, 1, 10)).hasSize(3);
        assertThatThrownBy(() -> consumptionRecordDao.findConsumptionRecords(DAY, DAY.plusDays(1), bay.getId(),
                null, null, 0, 10)).hasMessageContaining("Page 0 of size 10 does not exist");
    }

    @Test
    void exportGetsOnlyTheRecordsWaitingForExportOldestFirst() {
        ConsumptionRecord older = record("BAY-1", bay, club, DAY.plusHours(9));
        ConsumptionRecord newer = record("BAY-2", bay, club, DAY.plusHours(10));
        record("BAY-3", bay, club, DAY.plusHours(11)).markSuspect("too much");
        record("BAY-4", bay, club, DAY.plusHours(12)).setExportStatus(ExportStatus.TRANSFERRED);
        consumptionRecordDao.flush();

        assertThat(consumptionRecordDao.findConsumptionRecordIdsForExport(DAY, DAY.plusDays(1), bay.getId(), null))
                .containsExactly(older.getId(), newer.getId());
    }

    @Test
    void issuerExportGetsTheRecordsOfHisGuestsAtEveryCampsite() {
        ConsumptionRecord atLake = record("LAKE-1", lake, clubJunior, DAY.plusHours(9));
        ConsumptionRecord atBay = record("BAY-1", bay, club, DAY.plusHours(10));
        record("BAY-2", bay, otherClub, DAY.plusHours(11));

        assertThat(consumptionRecordDao.findConsumptionRecordIdsForExport(DAY, DAY.plusDays(1), null, club.getId()))
                .containsExactly(atLake.getId(), atBay.getId());
    }

    @Test
    void recordNumbersIncrease() {
        long first = consumptionRecordDao.nextRecordNumber();

        assertThat(consumptionRecordDao.nextRecordNumber()).isGreaterThan(first);
    }

    private ConsumptionRecord record(String recordNo, Group siteGroup, Group issuerGroup, LocalDateTime plugOut) {
        SupplySession session = new SupplySession();
        session.setSocket(pedestal.getSockets().get(0));
        session.setTransactionId(TRANSACTIONS.incrementAndGet());
        session.setCreateDate(plugOut.minusHours(1));
        session.setMaxCurrentA(16);
        supplySessionDao.save(session);

        ConsumptionRecord record = new ConsumptionRecord();
        record.setRecordNo(recordNo + "-" + session.getTransactionId());
        record.setSupplySessionId(session.getId());
        record.setSiteGroupId(siteGroup.getId());
        record.setIssuerGroupId(issuerGroup != null ? issuerGroup.getId() : null);
        record.setPaymentChannel(issuerGroup != null ? PaymentChannel.CARD : PaymentChannel.HOUSE_CARD);
        record.setPlugOut(plugOut);
        record.setCreateDate(plugOut);
        return consumptionRecordDao.saveAndFlush(record);
    }
}
