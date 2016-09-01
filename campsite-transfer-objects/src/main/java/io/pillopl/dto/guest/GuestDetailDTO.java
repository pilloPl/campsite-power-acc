package io.pillopl.dto.guest;

import io.pillopl.dto.card.CampingCardDTO;

import java.io.Serializable;
import java.util.List;

/**
 * A guest with everything the reception shows on its page: units, cards, billing accounts, seasonal contracts,
 * stays (latest first) and orders.
 */
public record GuestDetailDTO(GuestDTO guest, List<UnitDTO> units, List<CampingCardDTO> cards,
                             List<BillingAccountDTO> billingAccounts, List<SeasonalContractDTO> seasonalContracts,
                             List<StayDTO> stays, List<OrderDTO> orders) implements Serializable {

    public GuestDetailDTO {
        units = units != null ? List.copyOf(units) : List.of();
        cards = cards != null ? List.copyOf(cards) : List.of();
        billingAccounts = billingAccounts != null ? List.copyOf(billingAccounts) : List.of();
        seasonalContracts = seasonalContracts != null ? List.copyOf(seasonalContracts) : List.of();
        stays = stays != null ? List.copyOf(stays) : List.of();
        orders = orders != null ? List.copyOf(orders) : List.of();
    }
}
