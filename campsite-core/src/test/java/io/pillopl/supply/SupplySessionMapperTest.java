package io.pillopl.supply;

import io.pillopl.card.CampingCard;
import io.pillopl.dto.supply.SupplySessionDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import io.pillopl.guest.Guest;
import io.pillopl.guest.GuestType;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SupplySessionMapperTest {

    static final LocalDateTime CREATED = LocalDateTime.of(2026, 8, 4, 10, 0);

    final Pedestal pedestal = new Pedestal();
    final Socket socket = new Socket();
    final SupplySession session = new SupplySession();

    @BeforeEach
    void setUp() {
        pedestal.setCode("HR-BAY-P001");
        socket.setId(11L);
        socket.setSocketNo(1);
        socket.setPedestal(pedestal);
        socket.setMeterKwh(new BigDecimal("512.5"));
        session.setId(31L);
        session.setSocket(socket);
        session.setTransactionId(1001);
        session.setCreateDate(CREATED);
        session.setStartMeterKwh(new BigDecimal("500"));
        session.setActive(true);
    }

    @Test
    void activeSessionShowsTheConsumptionSoFarFromTheLastReadingOfTheSocket() {
        SupplySessionDTO dto = SupplySessionMapper.toDTO(session, null, false);

        assertThat(dto.stopMeterKwh()).isEqualByComparingTo("512.5");
        assertThat(dto.kwh()).isEqualByComparingTo("12.5");
        assertThat(dto.start()).isEqualTo(CREATED);
        assertThat(dto.consumptionRecordId()).isNull();
    }

    @Test
    void consumptionSoFarIsNeverNegativeWhenTheSocketMeterWasReset() {
        socket.setMeterKwh(new BigDecimal("3.2"));

        assertThat(SupplySessionMapper.toDTO(session, null, false).kwh()).isEqualByComparingTo("0");
    }

    @Test
    void closedSessionWithoutConsumptionRecordHasNoStopReading() {
        session.close(StopReason.TIMEOUT, CREATED.plusHours(30));

        SupplySessionDTO dto = SupplySessionMapper.toDTO(session, null, false);

        assertThat(dto.active()).isFalse();
        assertThat(dto.stopMeterKwh()).isNull();
        assertThat(dto.kwh()).isNull();
        assertThat(dto.stopReason()).isEqualTo("TIMEOUT");
    }

    @Test
    void guestWithoutFirstNameIsListedByTheNameOnly() {
        Guest guest = new Guest(new Group(GroupType.ISSUER, "NETWORK CARD"), GuestType.TRANSIT, "Horvat");
        session.setCard(new CampingCard.Builder("7001", "HR-1").guest(guest).build());

        assertThat(SupplySessionMapper.toDTO(session, null, false).guestName()).isEqualTo("Horvat");
    }
}
