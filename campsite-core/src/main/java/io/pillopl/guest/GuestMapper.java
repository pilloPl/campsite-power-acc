package io.pillopl.guest;

import io.pillopl.dto.guest.BillingAccountDTO;
import io.pillopl.dto.guest.GuestDTO;
import io.pillopl.dto.guest.OrderDTO;
import io.pillopl.dto.guest.SeasonalContractDTO;
import io.pillopl.dto.guest.UnitDTO;
import io.pillopl.order.Order;
import io.pillopl.order.OrderItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Guests, units, billing accounts, seasonal contracts and orders to transfer objects for the guests tab.
 */
public final class GuestMapper {

    private GuestMapper() {
        throw new AssertionError();
    }

    public static GuestDTO toDTO(Guest guest) {
        return new GuestDTO(guest.getId(), guest.getGroup() != null ? guest.getGroup().getId() : null,
                guest.getGroup() != null ? guest.getGroup().getDisplayName() : null, name(guest.getGuestType()),
                guest.getName(), guest.getFirstName(), guest.getFullName(), guest.getEmail(),
                guest.getMobile() != null ? guest.getMobile() : guest.getPhone(), guest.getCity(),
                guest.getCountryIso(), name(guest.getStatus()), guest.getErpPartnerNo(), guest.getCarPlate(),
                guest.getTrailerPlate(), name(guest.getPaymentMethod()), guest.getCreateDate());
    }

    public static List<GuestDTO> toGuestDTOs(List<Guest> guests) {
        List<GuestDTO> result = new ArrayList<>(guests.size());
        for (Guest guest : guests) {
            result.add(toDTO(guest));
        }
        return result;
    }

    public static UnitDTO toDTO(Unit unit) {
        return new UnitDTO(unit.getId(), unit.getGuest() != null ? unit.getGuest().getId() : null,
                name(unit.getUnitType()), unit.getPlate(), unit.getLengthM(), unit.getDescription());
    }

    public static BillingAccountDTO toDTO(BillingAccount account) {
        return new BillingAccountDTO(account.getId(), account.getGuest() != null ? account.getGuest().getId() : null,
                account.getErpAccountNo(), account.getDescription(), account.isAwaitingErpNumber(),
                account.isClosed(), account.getCreateDate());
    }

    public static SeasonalContractDTO toDTO(SeasonalContract contract) {
        BillingAccount account = contract.getBillingAccount();
        Guest guest = account != null ? account.getGuest() : null;
        return new SeasonalContractDTO(contract.getId(), guest != null ? guest.getId() : null,
                guest != null ? guest.getFullName() : null, account != null ? account.getId() : null,
                account != null ? account.getErpAccountNo() : null,
                contract.getTariff() != null ? contract.getTariff().getId() : null,
                contract.getTariff() != null ? contract.getTariff().getName() : null, name(contract.getStatus()),
                contract.getSiteGroup() != null ? contract.getSiteGroup().getId() : null,
                contract.getSiteGroup() != null ? contract.getSiteGroup().getDisplayName() : null,
                contract.getPitchNo(), contract.getStartDate(), contract.getEndDate(), contract.getRequestedEndDate(),
                contract.getFixedFee(), name(contract.getFeePeriod()), contract.getPackageKwh());
    }

    public static List<SeasonalContractDTO> toContractDTOs(List<SeasonalContract> contracts) {
        List<SeasonalContractDTO> result = new ArrayList<>(contracts.size());
        for (SeasonalContract contract : contracts) {
            result.add(toDTO(contract));
        }
        return result;
    }

    public static OrderDTO toDTO(Order order) {
        List<String> items = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            items.add(item.getQuantity() > 1 ? item.getQuantity() + " x " + item.getTitle() : item.getTitle());
        }
        return new OrderDTO(order.getId(), order.getOrderNumber(),
                order.getGuest() != null ? order.getGuest().getId() : null, name(order.getStatus()),
                name(order.getGuestType()), items, order.getCreateDate(), order.getActivationDate());
    }

    private static String name(Enum<?> value) {
        return value != null ? value.name() : null;
    }
}
