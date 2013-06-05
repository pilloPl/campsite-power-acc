package io.pillopl.infrastructure;

import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.dto.infrastructure.LocationDTO;
import io.pillopl.dto.infrastructure.PedestalDTO;
import io.pillopl.dto.infrastructure.PedestalImportDTO;
import io.pillopl.dto.infrastructure.PedestalSummaryDTO;
import io.pillopl.dto.infrastructure.ReceptionHoursDTO;
import io.pillopl.dto.infrastructure.SocketImportDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import io.pillopl.infrastructure.supplier.PedestalImportRow;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PedestalMapperTest {

    @Test
    void deletedSocketsAreLeftOutOfTheDetailAndTheList() {
        Pedestal pedestal = pedestal();
        pedestal.getSockets().get(1).setDeleted(true);

        PedestalDTO dto = PedestalMapper.toDTO(pedestal, null);
        PedestalSummaryDTO summary = PedestalMapper.toSummary(pedestal);

        assertThat(dto.sockets()).extracting("name").containsExactly("P001_1");
        assertThat(summary.numberOfSockets()).isEqualTo(1);
        assertThat(summary.groupName()).isEqualTo("Bay");
        assertThat(summary.city()).isEqualTo("Rovinj");
    }

    @Test
    void pedestalThatNeverSentAnythingCountsAsTimedOut() {
        Pedestal pedestal = pedestal();
        LocalDateTime lastIncoming = LocalDateTime.of(2016, 5, 1, 10, 0);

        assertThat(PedestalMapper.toDTO(pedestal, null).linkTimeout()).isTrue();
        PedestalDTO alive = PedestalMapper.toDTO(pedestal, new PedestalState(pedestal, lastIncoming));
        assertThat(alive.linkTimeout()).isFalse();
        assertThat(alive.lastIncoming()).isEqualTo(lastIncoming);
    }

    @Test
    void socketInErrorIsFlaggedInTheList() {
        Pedestal pedestal = pedestal();
        pedestal.getSockets().get(0).setMonitoring(true);
        pedestal.getSockets().get(0).setStatus(SocketStatus.FAULTED);

        assertThat(PedestalMapper.toSummary(pedestal).socketInError()).isTrue();
    }

    @Test
    void locationFromTheFormNeedsAnAddressAndGetsAnUpperCaseCampsiteCode() {
        LocationDTO form = new LocationDTO(99L, "Bay", " hrbay ", "Obala", "12", "52210", "Rovinj", null, "HR",
                null, null, null, List.of(new ReceptionHoursDTO(DayOfWeek.MONDAY, LocalTime.of(8, 0),
                LocalTime.of(20, 0))));

        Location location = PedestalMapper.toLocation(form);

        assertThat(location.getId()).isNull();
        assertThat(location.getCampsiteCode()).isEqualTo("HRBAY");
        assertThat(location.getReceptionHours()).hasSize(1);
        assertThat(location.getReceptionHours().get(0).getLocation()).isSameAs(location);
        assertThatThrownBy(() -> PedestalMapper.toLocation(new LocationDTO(null, "Bay", null, "Obala", "12", " ",
                "Rovinj", null, null, null, null, null, null))).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> PedestalMapper.toLocation(null)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void importRowTakesTheSocketsAndTheLinkVersion() {
        PedestalImportDTO dto = importDto("v2");

        PedestalImportRow row = PedestalMapper.toImportRow(dto);

        assertThat(row.getLinkVersion()).isEqualTo(PedestalLinkVersion.V2);
        assertThat(row.getSockets()).hasSize(1);
        assertThat(row.getSockets().get(0).getPos()).isEqualTo(1);
        assertThat(row.getSockets().get(0).getAmperes()).isEqualTo(16);
        assertThat(PedestalMapper.toImportRow(importDto(null)).getLinkVersion()).isEqualTo(PedestalLinkVersion.V1);
    }

    @Test
    void unknownLinkVersionIsRejected() {
        assertThatThrownBy(() -> PedestalMapper.toImportRow(importDto("v9")))
                .isInstanceOf(InvalidInputException.class);
    }

    private static PedestalImportDTO importDto(String linkVersion) {
        return new PedestalImportDTO(null, "REF-1", "ACME-0001", false, true, "ACME", "P4", linkVersion, "SN1",
                "8938", null, null, "PUBLIC_24H", "Bay", "HRBAY", "Obala", "12", "52210", "Rovinj", "HR", null,
                List.of(new SocketImportDTO("S1", 1, "1", "CEE", 1, 16, "A1")));
    }

    private static Pedestal pedestal() {
        Location location = new Location("Rovinj", "52210", "Obala", "12");
        Pedestal pedestal = new Pedestal("P001", new PedestalType("ACME", "P4", PedestalLinkVersion.V1), location);
        Group bay = new Group(GroupType.SITE, "BAY CAMPSITE", "Bay");
        bay.setId(5L);
        pedestal.setGroup(bay);
        pedestal.addSocket(new Socket("P001_1", null, 1));
        pedestal.addSocket(new Socket("P001_2", null, 2));
        return pedestal;
    }
}
