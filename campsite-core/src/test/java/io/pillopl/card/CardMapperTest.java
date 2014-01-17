package io.pillopl.card;

import io.pillopl.dto.card.CampingCardDTO;
import io.pillopl.dto.card.CardCheckDTO;
import io.pillopl.dto.card.HouseCardDTO;
import io.pillopl.dto.card.LocalCardListDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import io.pillopl.guest.Guest;
import io.pillopl.guest.GuestType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CardMapperTest {

    final Group club = new Group(GroupType.ISSUER, "CARAVAN CLUB", "Caravan club");
    final Guest guest = new Guest(club, GuestType.SEASONAL, "Kovac");

    @Test
    void cardShowsGuestIssuerAndTodaysValidation() {
        CampingCard card = new CampingCard.Builder("3283263748", "HR-1").guest(guest)
                .validTo(LocalDate.of(2026, 7, 14)).billingAccountNo("400012345678").build();

        CampingCardDTO dto = CardMapper.toDTO(card, LocalDateTime.of(2026, 7, 20, 10, 0));

        assertThat(dto.guestName()).isEqualTo("Kovac");
        assertThat(dto.issuerGroupName()).isEqualTo("Caravan club");
        assertThat(dto.activation()).isEqualTo("ACTIVATED");
        assertThat(dto.cardStatus()).isEqualTo("NOT_VALID_ANYMORE");
        assertThat(dto.partnerSyncState()).isEqualTo("ADD");
        assertThat(CardMapper.toDTOs(List.of(card), LocalDateTime.of(2026, 7, 1, 0, 0))).extracting(
                CampingCardDTO::cardStatus).containsExactly("VALID");
    }

    @Test
    void cardWithoutGuestHasNoGuestFields() {
        CampingCardDTO dto = CardMapper.toDTO(new CampingCard.Builder("1", "1").build(), LocalDateTime.now());

        assertThat(dto.guestId()).isNull();
        assertThat(dto.cardStatus()).isEqualTo("NO_GUEST");
    }

    @Test
    void checkShowsTheAnswerOfThePedestal() {
        ValidationService.ValidationResult result = new ValidationService.ValidationResult.Builder()
                .validationStatus(ValidationStatus.ACCEPTED)
                .houseCard(new HouseCard(HouseCardType.STAFF, "7", "S7")).build();

        CardCheckDTO dto = CardMapper.toCheckDTO("HR-BAY-P001/1", "7", result);

        assertThat(dto.validationStatus()).isEqualTo("ACCEPTED");
        assertThat(dto.houseCard()).isTrue();
        assertThat(dto.cardId()).isNull();
    }

    @Test
    void localListKeepsOrderAndCounts() {
        LocalCardList list = new LocalCardList("HR-BAY-P001", 2, List.of(
                new LocalCardListEntry("H1", null, true, true),
                new LocalCardListEntry("C1", LocalDate.of(2026, 7, 14), false, true)), 1, 1, 3);

        LocalCardListDTO dto = CardMapper.toDTO(list);

        assertThat(dto.entries()).extracting("rfid").containsExactly("H1", "C1");
        assertThat(dto.droppedEntries()).isEqualTo(3);
        assertThat(dto.capacity()).isEqualTo(2);
    }

    @Test
    void houseCardShowsItsCampsite() {
        HouseCard houseCard = new HouseCard(HouseCardType.RENTAL, "8", "R8");
        houseCard.setGroup(new Group(GroupType.SITE, "BAY CAMPSITE", "Bay"));

        HouseCardDTO dto = HouseCardMapper.toDTO(houseCard);

        assertThat(dto.houseCardType()).isEqualTo("RENTAL");
        assertThat(dto.groupName()).isEqualTo("Bay");
        assertThat(dto.active()).isTrue();
        assertThat(HouseCardMapper.toDTOs(List.of(houseCard))).hasSize(1);
    }
}
