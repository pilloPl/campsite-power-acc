package io.pillopl.order;

import io.pillopl.card.CampingCard;
import io.pillopl.card.CardActivation;
import io.pillopl.group.AccessDeniedException;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.GroupType;
import io.pillopl.group.Market;
import io.pillopl.guest.BillingAccount;
import io.pillopl.guest.Guest;
import io.pillopl.guest.GuestType;
import io.pillopl.guest.SeasonalContractService;
import io.pillopl.persistence.dao.BillingAccountDao;
import io.pillopl.persistence.dao.CampingCardDao;
import io.pillopl.persistence.dao.GuestDao;
import io.pillopl.persistence.dao.OrderDao;
import io.pillopl.persistence.dao.ProductDefinitionDao;
import io.pillopl.user.Userinfo;
import io.pillopl.util.DateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Orders of seasonal contracts, cards and house card sets, and their way through the ERP: an order of a guest the
 * ERP does not know yet waits (AWAITING_ERP_ACCOUNT) until the ERP sends the partner and account numbers back;
 * then it is activated: the guest is created or updated, the billing account gets its number, the seasonal package
 * becomes a contract and the ordered cards are billed to the account.
 * <p>
 * The hand-over to the ERP and the reception of the numbers are the ERP module's jobs; they call
 * {@link #findOrdersAwaitingErpAccount}, {@link #markOrdersExported} and {@link #enrichOrderWithErpNumbers}.
 */
@Service
public class OrderService {

    private static final Logger LOG = LoggerFactory.getLogger(OrderService.class);

    private final OrderDao orderDao;

    private final ProductDefinitionDao productDefinitionDao;

    private final GuestDao guestDao;

    private final BillingAccountDao billingAccountDao;

    private final CampingCardDao campingCardDao;

    private final SeasonalContractService seasonalContractService;

    private final GroupService groupService;

    private final DateService dateService;

    public OrderService(OrderDao orderDao, ProductDefinitionDao productDefinitionDao, GuestDao guestDao,
                        BillingAccountDao billingAccountDao, CampingCardDao campingCardDao,
                        SeasonalContractService seasonalContractService, GroupService groupService,
                        DateService dateService) {
        this.orderDao = orderDao;
        this.productDefinitionDao = productDefinitionDao;
        this.guestDao = guestDao;
        this.billingAccountDao = billingAccountDao;
        this.campingCardDao = campingCardDao;
        this.seasonalContractService = seasonalContractService;
        this.groupService = groupService;
        this.dateService = dateService;
    }

    @Transactional(readOnly = true)
    public Order findOrder(Long orderId) {
        return orderId != null ? orderDao.findById(orderId).orElse(null) : null;
    }

    @Transactional(readOnly = true)
    public List<Order> findOrdersOfGuest(Userinfo principal, Long guestId) {
        Guest guest = guestId != null ? guestDao.findById(guestId).orElse(null) : null;
        if (guest == null) {
            throw new NoSuchElementException("Guest " + guestId + " not found");
        }
        checkIsManageable(principal, guest.getGroup());
        return orderDao.findByGuestIdOrderByCreateDateDesc(guestId);
    }

    /**
     * An order of a guest the network knows, billed to the given account of the guest (null: a new account waiting
     * for its ERP number is opened with the order).
     */
    @Transactional
    public Order createOrderForGuest(Userinfo principal, Long guestId, Long billingAccountId) {
        Guest guest = guestId != null ? guestDao.findById(guestId).orElse(null) : null;
        if (guest == null || guest.isDeleted()) {
            throw new NoSuchElementException("Guest " + guestId + " not found");
        }
        checkIsManageable(principal, guest.getGroup());
        if (guest.isDeactivated()) {
            throw new IllegalStateException("Guest " + guestId + " is deactivated");
        }
        BillingAccount account;
        if (billingAccountId != null) {
            account = billingAccountDao.findById(billingAccountId).orElse(null);
            if (account == null || !account.getGuest().equals(guest)) {
                throw new IllegalArgumentException(String.format("Billing account %s is no account of guest %s",
                        billingAccountId, guestId));
            }
            if (account.isClosed()) {
                throw new IllegalStateException("Billing account " + billingAccountId + " is closed");
            }
        } else {
            account = guest.openBillingAccount(null, "Ordered");
            account.setCreateDate(dateService.getNow());
            account = billingAccountDao.save(account);
        }
        Order order = Order.forGuest(guest, account);
        return saveNewOrder(principal, order);
    }

    /**
     * An order of a guest new to the network; the guest is created when the order is activated.
     */
    @Transactional
    public Order createOrderForNewGuest(Userinfo principal, Guest.Builder details) {
        Guest guestDetails = details.build();
        if (guestDetails.getGroup() == null
                || !GroupType.ISSUER.name().equals(guestDetails.getGroup().getType())) {
            throw new IllegalArgumentException("The guest of an order hangs in an ISSUER group");
        }
        if (guestDetails.getName() == null || guestDetails.getName().isBlank()) {
            throw new IllegalArgumentException("Name of the guest is missing");
        }
        checkIsManageable(principal, guestDetails.getGroup());
        return saveNewOrder(principal, Order.forNewGuest(guestDetails));
    }

    /**
     * Adds a product of the catalogue. A seasonal package needs the campsite and the start of the season (the end
     * defaults to the product's duration); a card may name the card handed out.
     */
    @Transactional
    public OrderItem addItem(Userinfo principal, Long orderId, String productCode, int quantity, Long siteGroupId,
                             String pitchNo, LocalDate startDate, LocalDate endDate, Long cardId) {
        Order order = getOrder(orderId);
        checkIsManageable(principal, order.getGroup());
        ProductDefinition product = productCode != null ? productDefinitionDao.findByCodeAndDeletedFalse(productCode)
                : null;
        if (product == null) {
            throw new NoSuchElementException("Product " + productCode + " not found");
        }
        OrderItem item = order.addItem(product, quantity);
        if (product.isSeasonalPackage()) {
            Group siteGroup = siteGroupId != null ? groupService.fetchByGroupId(siteGroupId) : null;
            if (siteGroup == null || !GroupType.SITE.name().equals(siteGroup.getType())) {
                throw new IllegalArgumentException("Seasonal package needs the campsite (SITE group): " + siteGroupId);
            }
            if (startDate == null) {
                throw new IllegalArgumentException("Seasonal package needs the start of the season");
            }
            item.setSiteGroup(siteGroup);
            item.setPitchNo(pitchNo != null && !pitchNo.isBlank() ? pitchNo.trim().toUpperCase() : null);
            item.setStartDate(startDate);
            item.setEndDate(endDate != null ? endDate : defaultEnd(product, startDate));
            order.setGuestType(GuestType.SEASONAL);
        }
        if (cardId != null) {
            if (!product.isCard()) {
                throw new IllegalArgumentException("Only a card item names a card: " + productCode);
            }
            CampingCard card = campingCardDao.findById(cardId).orElse(null);
            if (card == null || card.isDeleted()) {
                throw new NoSuchElementException("Card " + cardId + " not found");
            }
            if (card.getGuest() != null && !card.getGuest().equals(order.getGuest())) {
                throw new IllegalArgumentException("Card " + card.getCardNumber() + " belongs to another guest");
            }
            item.setCard(card);
            item.setRequestedCardNumber(card.getCardNumber());
        }
        order.setModDate(dateService.getNow());
        orderDao.saveAndFlush(order);
        return item;
    }

    /**
     * The order is complete. With both ERP numbers known it is activated at once, otherwise it waits for the ERP.
     */
    @Transactional
    public Order submitOrder(Userinfo principal, Long orderId) {
        Order order = getOrder(orderId);
        checkIsManageable(principal, order.getGroup());
        LocalDateTime now = dateService.getNow();
        if (order.submit(now)) {
            activate(order, now);
        } else {
            LOG.info(String.format("Order %s waits for the ERP partner and account", order));
        }
        return orderDao.save(order);
    }

    @Transactional
    public Order cancelOrder(Userinfo principal, Long orderId, String reason) {
        Order order = getOrder(orderId);
        checkIsManageable(principal, order.getGroup());
        order.cancel(reason != null ? reason : "by " + principal.getUsername(), dateService.getNow());
        LOG.info(String.format("Order %s cancelled by %s", order, principal.getUsername()));
        return orderDao.save(order);
    }

    /**
     * Orders to hand to the ERP for partner and account creation.
     *
     * @param notExportedOnly false to hand over again the orders the ERP did not answer
     */
    @Transactional(readOnly = true)
    public List<Order> findOrdersAwaitingErpAccount(boolean notExportedOnly) {
        return orderDao.findOrdersAwaitingErpAccount(notExportedOnly);
    }

    @Transactional
    public int markOrdersExported(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return 0;
        }
        return orderDao.flagOrdersExported(orderIds, dateService.getNow());
    }

    /**
     * The ERP sends the partner and account number of an order. An order handled already is skipped (the ERP
     * repeats its answers); otherwise the numbers are taken and the order is activated.
     *
     * @return true when the order was activated by this call
     */
    @Transactional
    public boolean enrichOrderWithErpNumbers(Long orderId, String partnerNo, String accountNo) {
        Order order = getOrder(orderId);
        OrderStatus actualStatus = order.getStatus();

        // skip orders that left NEW
        if (actualStatus == OrderStatus.ACTIVE) {
            LOG.info(String.format("Skipped order %s already active with partner '%s' and account '%s'",
                    order.getOrderNumber(), order.getErpPartnerNo(), order.getErpAccountNo()));
            return false;
        }
        if (actualStatus == OrderStatus.CANCELLED) {
            LOG.warn(String.format("ERP numbers '%s' / '%s' for cancelled order %s ignored", partnerNo, accountNo,
                    order.getOrderNumber()));
            return false;
        }
        if (actualStatus == OrderStatus.NEW) {
            // the reception created the guest in the ERP itself before submitting
            LOG.warn(String.format("ERP numbers for order %s that was never submitted", order.getOrderNumber()));
            order.setStatus(OrderStatus.AWAITING_ERP_ACCOUNT);
        }
        LocalDateTime now = dateService.getNow();
        order.takeErpNumbers(partnerNo, accountNo, now);
        activate(order, now);
        orderDao.save(order);
        LOG.info(String.format("Order %s activated with partner '%s' and account '%s' (was %s)",
                order.getOrderNumber(), partnerNo, accountNo, actualStatus));
        return true;
    }

    /**
     * The ERP corrected partner and account numbers: orders, guest, billing account and cards follow.
     *
     * @return number of orders corrected
     */
    @Transactional
    public int correctErpNumbers(String oldPartnerNo, String oldAccountNo, String newPartnerNo, String newAccountNo) {
        if (!CampingCard.isValidBillingAccountNo(newAccountNo)) {
            throw new IllegalArgumentException("Billing account number is not valid: " + newAccountNo);
        }
        Integer updated = orderDao.updateErpNumbers(oldPartnerNo, oldAccountNo, newPartnerNo, newAccountNo);
        LOG.info(String.format("ERP numbers %s / %s corrected to %s / %s on %s orders", oldPartnerNo, oldAccountNo,
                newPartnerNo, newAccountNo, updated));
        return updated != null ? updated : 0;
    }

    /**
     * Guest, account, contracts and cards of the order.
     */
    private void activate(Order order, LocalDateTime now) {
        if (!CampingCard.isValidBillingAccountNo(order.getErpAccountNo())) {
            throw new IllegalArgumentException("ERP account number is not valid: " + order.getErpAccountNo());
        }
        // the guest: new guests are created now, known guests get the details of the order
        Guest guest = order.getGuest();
        if (guest == null) {
            guest = order.toGuestDetails().build();
            guest.setCreateDate(now);
        } else {
            guest.updateDetails(order.toGuestDetails().build(), now);
            if (order.getGuestType() == GuestType.SEASONAL) {
                guest.setGuestType(GuestType.SEASONAL);
            }
        }
        guest.setErpPartnerNo(order.getErpPartnerNo());
        guest = guestDao.save(guest);

        // the billing account: the one of the order, the guest's with the number, or a new one
        BillingAccount account = order.getBillingAccount();
        if (account == null) {
            account = guest.findBillingAccount(order.getErpAccountNo());
        }
        if (account == null) {
            account = guest.openBillingAccount(order.getErpAccountNo(), "Order " + order.getOrderNumber());
            account.setCreateDate(now);
        }
        BillingAccount owner = billingAccountDao.findByErpAccountNo(order.getErpAccountNo());
        if (owner != null && !owner.equals(account)) {
            throw new IllegalStateException(String.format("ERP account %s belongs to billing account %s already",
                    order.getErpAccountNo(), owner.getId()));
        }
        account.setErpAccountNo(order.getErpAccountNo());
        account = billingAccountDao.save(account);

        // contracts and cards of the items
        List<String> done = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            if (item.getProductDefinition().createsContract()) {
                seasonalContractService.createContract(account, item);
                done.add("contract " + item.getPitchNo());
            }
            CampingCard card = item.getCard();
            if (card != null) {
                card.setGuest(guest);
                card.setBillingAccountNo(account.getErpAccountNo());
                card.setActivation(CardActivation.ACTIVATED);
                card.setDeleted(false);
                OrderItem season = findSeasonalPackage(order);
                if (season != null) {
                    // the card of a seasonal resident is valid for the season
                    card.setValidFrom(season.getStartDate());
                    card.setValidTo(season.getEndDate());
                } else if (card.getValidFrom() == null) {
                    card.setValidFrom(now.toLocalDate());
                }
                campingCardDao.save(card);
                done.add("card " + card.getCardNumber());
            }
        }
        order.activate(guest, account, now);
        LOG.info(String.format("Order %s activated for %s on %s: %s", order.getOrderNumber(), guest, account, done));
    }

    private static OrderItem findSeasonalPackage(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.isSeasonalPackage()) {
                return item;
            }
        }
        return null;
    }

    private Order saveNewOrder(Userinfo principal, Order order) {
        Market market = groupService.determineMarketByGroupId(order.getGroup().getId());
        order.setOrderNumber((market != null ? market : Market.UNKNOWN).name() + "-" + orderDao.nextOrderNumber());
        order.setCreateDate(dateService.getNow());
        order.setCreatedBy(principal.getUsername());
        order = orderDao.save(order);
        LOG.info(String.format("Order %s created by %s", order, principal.getUsername()));
        return order;
    }

    private static LocalDate defaultEnd(ProductDefinition product, LocalDate start) {
        return product.getDurationMonths() != null ? start.plusMonths(product.getDurationMonths()).minusDays(1)
                : null;
    }

    private void checkIsManageable(Userinfo principal, Group issuerGroup) {
        if (!groupService.isManageable(principal.getGroup(GroupType.ISSUER), issuerGroup)) {
            throw new AccessDeniedException(String.format("User '%s' has no access to the orders of %s",
                    principal.getUsername(), issuerGroup != null ? issuerGroup.getName() : null));
        }
    }

    private Order getOrder(Long orderId) {
        Order order = findOrder(orderId);
        if (order == null) {
            throw new NoSuchElementException("Order " + orderId + " not found");
        }
        return order;
    }
}
