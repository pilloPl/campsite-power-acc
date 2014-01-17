package io.pillopl.card;

import io.pillopl.dto.card.CampingCardDTO;
import io.pillopl.dto.card.CardCheckDTO;
import io.pillopl.dto.card.LocalCardListDTO;
import io.pillopl.dto.card.LocalCardListEntryDTO;
import io.pillopl.guest.Guest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Camping cards, card checks and local card lists to DTOs.
 */
public final class CardMapper {

    private CardMapper() {
        throw new AssertionError();
    }

    /**
     * @param now the card status in the DTO is the validation at this moment
     */
    public static CampingCardDTO toDTO(CampingCard card, LocalDateTime now) {
        Guest guest = card.getGuest();
        return new CampingCardDTO(card.getId(), card.getCardRfid(), card.getCardNumber(),
                card.getActivation().name(),
                guest != null ? guest.getId() : null,
                guest != null ? guest.getFullName() : null,
                guest != null && guest.getGroup() != null ? guest.getGroup().getDisplayName() : null,
                card.getValidFrom(), card.getValidTo(), card.getBillingAccountNo(),
                card.getPartnerSyncState() != null ? card.getPartnerSyncState().name() : null,
                card.validate(now).name());
    }

    public static List<CampingCardDTO> toDTOs(List<CampingCard> cards, LocalDateTime now) {
        List<CampingCardDTO> result = new ArrayList<>(cards.size());
        for (CampingCard card : cards) {
            result.add(toDTO(card, now));
        }
        return result;
    }

    public static CardCheckDTO toCheckDTO(String socket, String cardRfid,
                                          ValidationService.ValidationResult result) {
        CampingCard card = result.getCard();
        Guest guest = result.getGuest();
        return new CardCheckDTO(socket, cardRfid, result.getStatus().name(), result.isHouseCardAccepted(),
                card != null ? card.getId() : null, guest != null ? guest.getFullName() : null);
    }

    public static LocalCardListDTO toDTO(LocalCardList list) {
        List<LocalCardListEntryDTO> entries = new ArrayList<>(list.size());
        for (LocalCardListEntry entry : list.entries()) {
            entries.add(new LocalCardListEntryDTO(entry.rfid(), entry.validTo(), entry.houseCard()));
        }
        return new LocalCardListDTO(list.pedestalCode(), list.capacity(), list.houseCards(), list.cards(),
                list.droppedEntries(), entries);
    }
}
