package io.pillopl.card;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class LocalCardListEntryTest {

    @Test
    void houseCardEntryHasNoLastValidDay() {
        LocalCardListEntry entry = LocalCardListEntry.of(new HouseCard(HouseCardType.STAFF, "6001", "M-S1"));

        assertThat(entry.rfid()).isEqualTo("6001");
        assertThat(entry.validTo()).isNull();
        assertThat(entry.houseCard()).isTrue();
        assertThat(entry.active()).isTrue();
    }

    @Test
    void deletedOrDeactivatedHouseCardIsRemovedFromTheController() {
        HouseCard deactivated = new HouseCard(HouseCardType.RENTAL, "6002", "M-R1");
        deactivated.setActive(false);
        HouseCard deleted = new HouseCard(HouseCardType.RENTAL, "6003", "M-R2");
        deleted.markDeleted(LocalDateTime.of(2012, 7, 1, 10, 0));

        assertThat(LocalCardListEntry.of(deactivated).active()).isFalse();
        assertThat(LocalCardListEntry.of(deleted).active()).isFalse();
    }

    @Test
    void campingCardEntryKeepsTheDepartureDay() {
        CampingCard card = new CampingCard.Builder("3283263748", "C-100")
                .validTo(LocalDate.of(2012, 8, 15))
                .build();

        LocalCardListEntry entry = LocalCardListEntry.of(card);

        assertThat(entry.validTo()).isEqualTo(LocalDate.of(2012, 8, 15));
        assertThat(entry.houseCard()).isFalse();
        assertThat(entry.active()).isTrue();
    }

    @Test
    void deactivatedCampingCardIsAnInactiveEntry() {
        CampingCard card = new CampingCard.Builder("3283263749", "C-101")
                .activation(CardActivation.DEACTIVATED)
                .build();

        assertThat(LocalCardListEntry.of(card).active()).isFalse();
    }
}
