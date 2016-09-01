package io.pillopl.guest;

import io.pillopl.api.GuestApi;
import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.api.exception.ObjectNotFoundException;
import io.pillopl.card.CampingCard;
import io.pillopl.card.CardMapper;
import io.pillopl.dto.card.CampingCardDTO;
import io.pillopl.dto.guest.BillingAccountDTO;
import io.pillopl.dto.guest.GuestDTO;
import io.pillopl.dto.guest.GuestDetailDTO;
import io.pillopl.dto.guest.OrderDTO;
import io.pillopl.dto.guest.SeasonalContractDTO;
import io.pillopl.dto.guest.StayDTO;
import io.pillopl.dto.guest.UnitDTO;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.order.OrderService;
import io.pillopl.user.PrincipalResolver;
import io.pillopl.user.Userinfo;
import io.pillopl.user.UserrightName;
import io.pillopl.util.ApiExceptionTranslator;
import io.pillopl.util.ApiValues;
import io.pillopl.util.DateService;
import io.pillopl.value.CardId;
import io.pillopl.value.GroupId;
import io.pillopl.value.PitchNo;
import io.pillopl.value.Username;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Guest and stay calls of the console: GUEST_VIEW for the lists and the guest page, GUEST_EDIT to create guests and
 * units, check in, change the departure and cancel orders, STAY_CHECKOUT to check a stay out.
 */
@Service
@Transactional
public class GuestApiService implements GuestApi {

    private final GuestService guestService;

    private final PrincipalResolver principalResolver;

    private final OrderService orderService;

    private final GroupService groupService;

    private final DateService dateService;

    public GuestApiService(GuestService guestService, PrincipalResolver principalResolver, OrderService orderService,
                           GroupService groupService, DateService dateService) {
        this.guestService = guestService;
        this.principalResolver = principalResolver;
        this.orderService = orderService;
        this.groupService = groupService;
        this.dateService = dateService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GuestDTO> findGuests(Username authUsername, String search) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.GUEST_VIEW);
            return GuestMapper.toGuestDTOs(guestService.findGuests(principal, search));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GuestDetailDTO getGuest(Username authUsername, Long guestId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.GUEST_VIEW);
            if (guestId == null) {
                throw new InvalidInputException("Guest id is missing");
            }
            Guest guest = guestService.findManageableGuest(principal, guestId);
            List<UnitDTO> units = new ArrayList<>();
            for (Unit unit : guest.getUnits()) {
                if (!unit.isDeleted()) {
                    units.add(GuestMapper.toDTO(unit));
                }
            }
            List<BillingAccountDTO> accounts = new ArrayList<>();
            for (BillingAccount account : guest.getBillingAccounts()) {
                accounts.add(GuestMapper.toDTO(account));
            }
            List<CampingCardDTO> cards = new ArrayList<>();
            for (CampingCard card : guestService.findCardsOfGuest(principal, guestId)) {
                cards.add(CardMapper.toDTO(card, dateService.getNow()));
            }
            List<StayDTO> stays = guestService.findStaysOfGuest(principal, guestId).stream()
                    .map(GuestApiService::toDTO).toList();
            List<OrderDTO> orders = orderService.findOrdersOfGuest(principal, guestId).stream()
                    .map(GuestMapper::toDTO).toList();
            return new GuestDetailDTO(GuestMapper.toDTO(guest), units, cards, accounts,
                    GuestMapper.toContractDTOs(guestService.findContractsOfGuest(principal, guestId)), stays, orders);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StayDTO> findStays(Username authUsername, GroupId campsite, String state, LocalDate from,
                                   LocalDate to) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.GUEST_VIEW);
            if (from == null || to == null) {
                throw new InvalidInputException("Period of the stays is missing");
            }
            String stayState = state == null || state.isBlank() ? GuestService.STAYS_ALL
                    : state.trim().replace(' ', '_').replace('-', '_').toUpperCase(Locale.ROOT);
            if (!List.of(GuestService.STAYS_ALL, GuestService.STAYS_OPEN, GuestService.STAYS_CHECKED_OUT)
                    .contains(stayState)) {
                throw new InvalidInputException("Unknown stay state: " + state);
            }
            return guestService.findStays(principal, campsite != null ? campsite.value() : null, stayState, from, to)
                    .stream().map(GuestApiService::toDTO).toList();
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeasonalContractDTO> findSeasonalContracts(Username authUsername, String status) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.GUEST_VIEW);
            ContractStatus contractStatus = ApiValues.parseEnumOrNull(ContractStatus.class, status,
                    "contract status");
            return GuestMapper.toContractDTOs(guestService.findSeasonalContracts(principal, contractStatus));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public GuestDTO createGuest(Username authUsername, GroupId issuerGroup, GuestDTO details) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.GUEST_EDIT);
            if (issuerGroup == null || details == null) {
                throw new InvalidInputException("Issuer group and details of the guest are missing");
            }
            Group group = groupService.fetchByGroupId(issuerGroup.value());
            if (group == null) {
                throw ObjectNotFoundException.of("Group", issuerGroup.value());
            }
            GuestType type = ApiValues.parseEnumOrNull(GuestType.class, details.guestType(), "guest type");
            Guest.Builder builder = new Guest.Builder(group, type != null ? type : GuestType.TRANSIT, details.name())
                    .firstName(details.firstName())
                    .email(details.email())
                    .phone(details.phone())
                    .city(details.city())
                    .countryIso(details.countryIso())
                    .carPlate(details.carPlate())
                    .trailerPlate(details.trailerPlate())
                    .paymentMethod(ApiValues.parseEnumOrNull(Guest.PaymentMethod.class, details.paymentMethod(),
                            "payment method"));
            return GuestMapper.toDTO(guestService.createGuest(principal, builder));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public UnitDTO addUnit(Username authUsername, Long guestId, String unitType, String plate, BigDecimal lengthM) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.GUEST_EDIT);
            Unit.UnitType type = ApiValues.parseEnum(Unit.UnitType.class, unitType, "unit type");
            return GuestMapper.toDTO(guestService.addUnit(principal, guestId, type, plate, lengthM));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public StayDTO changeDeparture(Username authUsername, Long stayId, LocalDate departure) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.GUEST_EDIT);
            if (stayId == null || departure == null) {
                throw new InvalidInputException("Stay and new departure are missing");
            }
            return toDTO(guestService.changeDeparture(principal, stayId, departure));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public OrderDTO cancelOrder(Username authUsername, Long orderId, String reason) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.GUEST_EDIT);
            if (orderId == null) {
                throw new InvalidInputException("Order id is missing");
            }
            return GuestMapper.toDTO(orderService.cancelOrder(principal, orderId,
                    reason != null && !reason.isBlank() ? reason.trim() : null));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public StayDTO checkIn(Username authUsername, Long guestId, GroupId campsite, PitchNo pitchNo, LocalDate arrival,
                           LocalDate departure, CardId cardId, Long unitId, String billingAccountNo) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.GUEST_EDIT);
            if (guestId == null || campsite == null) {
                throw new InvalidInputException("Guest and campsite of the stay are missing");
            }
            if (arrival == null || departure == null) {
                throw new InvalidInputException("Arrival and departure of the stay are missing");
            }
            Stay stay = guestService.checkIn(principal, guestId, campsite.value(),
                    pitchNo != null ? pitchNo.value() : null, arrival, departure,
                    cardId != null ? cardId.value() : null, unitId, billingAccountNo);
            return toDTO(stay);
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    @Override
    public StayDTO checkOut(Username authUsername, Long stayId) {
        try {
            Userinfo principal = principalResolver.resolve(authUsername, UserrightName.STAY_CHECKOUT);
            if (stayId == null) {
                throw new InvalidInputException("Stay id is missing");
            }
            return toDTO(guestService.checkOut(principal, stayId));
        } catch (RuntimeException e) {
            throw ApiExceptionTranslator.translate(e);
        }
    }

    static StayDTO toDTO(Stay stay) {
        CampingCard card = stay.getCard();
        return new StayDTO(stay.getId(), stay.getGuest().getId(), stay.getGuest().getFullName(),
                stay.getSiteGroup().getId(), stay.getSiteGroup().getDisplayName(), stay.getPitchNo(),
                card != null ? card.getId() : null, card != null ? card.getCardNumber() : null,
                card != null ? card.getValidTo() : null, card != null ? card.getBillingAccountNo() : null,
                stay.getUnitPlate(), stay.getArrivalDate(), stay.getDepartureDate(), stay.getNights(),
                stay.getCheckInDate(), stay.getCheckedInBy(), stay.getCheckOutDate(), stay.getCheckedOutBy());
    }
}
