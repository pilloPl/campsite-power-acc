package io.pillopl.infrastructure.supplier;

import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.Market;
import io.pillopl.group.attribute.SupplierMarkerService;
import io.pillopl.infrastructure.BreakerRating;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PlugStandard;
import io.pillopl.infrastructure.Socket;
import io.pillopl.infrastructure.SocketStatus;
import io.pillopl.persistence.dao.PedestalDao;
import io.pillopl.persistence.dao.SocketDao;
import io.pillopl.test.IntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class SupplierPedestalImportIntegrationTest extends IntegrationTest {

    @Autowired SupplierPedestalImportService importService;
    @Autowired SupplierMarkerService supplierMarkerService;
    @Autowired GroupService groupService;
    @Autowired PedestalDao pedestalDao;
    @Autowired SocketDao socketDao;
    @Autowired EntityManager entityManager;

    @Test
    void newDeliveryGoesIntoTheStockOfItsManufacturerWithItsSockets() {
        Group stock = supplierMarkerService.createSupplierStock("ACME PEDESTALS", Market.HR);

        Pedestal pedestal = importService.saveFromImport(row("ACME-0001", null, "CEE_BLUE_16A", "CEE_RED_32A"), false);

        assertThat(pedestal.getCode()).isEqualTo("ACME-0001");
        assertThat(pedestal.getReferenceId()).isEqualTo("ACME-0001");
        assertThat(pedestal.getGroup()).isEqualTo(stock);
        assertThat(pedestal.isBillable()).isFalse();
        assertThat(pedestal.isListedInApp()).isTrue();
        assertThat(pedestal.getPedestalType().getManufacturer()).isEqualTo("ACME PEDESTALS");
        assertThat(pedestal.getSockets()).extracting(Socket::getName).containsExactly("ACME-0001-S1", "ACME-0001-S2");
        assertThat(pedestal.getSockets().get(1).getSocketType().getRating()).isEqualTo(BreakerRating.A32);
        assertThat(pedestal.getSockets()).allMatch(s -> s.getStatus() == SocketStatus.UNKNOWN && !s.isMonitoring());
        assertThat(pedestal.getLocation().getReceptionHours()).hasSize(1);
    }

    @Test
    void networkOwnedPedestalGetsItsCodeFromCountryZipAndReference() {
        PedestalImportRow row = row(null, "ref 42", "CEE_BLUE_16A");

        Pedestal pedestal = importService.saveFromImport(row, true);

        assertThat(pedestal.getCode()).isEqualTo("HR-52210-REF42");
        assertThat(pedestal.getSockets()).extracting(Socket::getName).containsExactly("HR-52210-REF42_1");
        assertThat(pedestal.getGroup()).isNull();
    }

    @Test
    void reimportUpdatesTheSameSocketsAndDeletesTheMissingOnes() {
        supplierMarkerService.createSupplierStock("ACME PEDESTALS", Market.HR);
        Pedestal first = importService.saveFromImport(row("ACME-0001", null, "CEE_BLUE_16A", "SCHUKO"), false);
        Long keptSocketId = first.getSockets().get(0).getId();
        entityManager.flush();
        entityManager.clear();

        Pedestal second = importService.saveFromImport(row("ACME-0001", null, "CEE_BLUE_16A"), false);
        entityManager.flush();
        entityManager.clear();

        Pedestal reloaded = pedestalDao.findById(second.getId()).orElseThrow();
        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(reloaded.getSockets()).extracting(Socket::getId).containsExactly(keptSocketId);
        assertThat(socketDao.findAll()).filteredOn(s -> s.getPedestal().getId().equals(first.getId()))
                .filteredOn(Socket::isDeleted).hasSize(1);
    }

    @Test
    void publicPedestalOfACampsiteHangsUnderNetworkAccessOfItsMarket() {
        PedestalImportRow row = row("ACME-0002", null, "CEE_BLUE_16A");
        row.setGroupOwner("HR SITES");
        row.setAccessibility("PUBLIC_FREE");

        Pedestal pedestal = importService.saveFromImport(row, false);

        assertThat(pedestal.getGroup().getName()).isEqualTo("NETWORK ACCESS HR");
    }

    @Test
    void existingAddressIsReused() {
        Pedestal a = importService.saveFromImport(row("ACME-0003", null, "CEE_BLUE_16A"), false);
        Pedestal b = importService.saveFromImport(row("ACME-0004", null, "CEE_BLUE_16A"), false);

        assertThat(a.getLocation().getId()).isEqualTo(b.getLocation().getId());
        assertThat(a.getLocation().getCampsiteCode()).isEqualTo("HRBAY");
    }

    private static PedestalImportRow row(String externalId, String referenceId, String... plugs) {
        PedestalImportRow row = new PedestalImportRow();
        row.setExternalId(externalId);
        row.setReferenceId(referenceId);
        row.setManufacturer("ACME PEDESTALS");
        row.setTypeName("P4");
        row.setCampsiteName("Bay campsite");
        row.setCampsiteCode("hrbay");
        row.setStreet("Obala");
        row.setHouseNumber("12");
        row.setZipcode("52210");
        row.setCity("Rovinj");
        row.setCountryIso("hr");
        row.getReceptionHours().add(new PedestalImportRow.ReceptionHoursRow(DayOfWeek.MONDAY, LocalTime.of(8, 0),
                LocalTime.of(20, 0)));
        int pos = 1;
        for (String plug : plugs) {
            PedestalImportRow.SocketRow socket = new PedestalImportRow.SocketRow();
            socket.setPos(pos);
            socket.setName(externalId + "-S" + pos);
            socket.setPlugStandard(plug);
            socket.setPhases(PlugStandard.valueOf(plug) == PlugStandard.CEE_RED_32A ? 3 : 1);
            socket.setAmperes(PlugStandard.valueOf(plug) == PlugStandard.CEE_RED_32A ? 32 : 16);
            row.getSockets().add(socket);
            pos++;
        }
        return row;
    }
}
