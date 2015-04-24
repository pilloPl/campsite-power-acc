package io.pillopl.order;

import io.pillopl.group.Group;
import io.pillopl.guest.BillingAccount;
import io.pillopl.guest.Guest;
import io.pillopl.guest.GuestType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Order of a seasonal contract, cards or a house card set. The order keeps its own flat copy of the guest's details
 * as entered (the guest may change them later) and walks NEW → AWAITING_ERP_ACCOUNT → ACTIVE: a guest unknown to
 * the ERP waits for the partner and account numbers, which the ERP sends back for the exported orders.
 */
@Entity(name = "OrderHeader")
@Table(name = "ORDER_HEADER")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_order_header")
    @SequenceGenerator(name = "seq_order_header", sequenceName = "ORDER_HEADER_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "ORDER_NUMBER", length = 32, nullable = false, unique = true)
    private String orderNumber;

    /**
     * ISSUER group the guest of the order hangs in (or will hang in).
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "GROUP_ID", nullable = false)
    private Group group;

    @ManyToOne
    @JoinColumn(name = "GUEST_ID")
    private Guest guest;

    @ManyToOne
    @JoinColumn(name = "BILLING_ACCOUNT_ID")
    private BillingAccount billingAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 32, nullable = false)
    private OrderStatus status = OrderStatus.NEW;

    @Enumerated(EnumType.STRING)
    @Column(name = "GUEST_TYPE", length = 16, nullable = false)
    private GuestType guestType = GuestType.TRANSIT;

    @Column(name = "SALUTATION", length = 16)
    private String salutation;

    @Column(name = "NAME", length = 255, nullable = false)
    private String name;

    @Column(name = "FIRST_NAME", length = 255)
    private String firstName;

    @Column(name = "INITIALS", length = 32)
    private String initials;

    @Column(name = "ORGANISATION_NAME", length = 200)
    private String organisationName;

    @Column(name = "VAT_NUMBER", length = 32)
    private String vatNumber;

    @Column(name = "STREET", length = 100)
    private String street;

    @Column(name = "HOUSE_NO", length = 16)
    private String houseNo;

    @Column(name = "HOUSE_NO_ADD", length = 16)
    private String houseNoAdd;

    @Column(name = "ZIP_CODE", length = 16)
    private String zipCode;

    @Column(name = "CITY", length = 100)
    private String city;

    @Column(name = "COUNTRY_ISO", length = 2)
    private String countryIso;

    @Column(name = "LANGUAGE", length = 8)
    private String language;

    @Column(name = "EMAIL", length = 255)
    private String email;

    @Column(name = "PHONE", length = 32)
    private String phone;

    @Column(name = "MOBILE", length = 32)
    private String mobile;

    @Column(name = "BANK_ACCOUNT_NAME", length = 200)
    private String bankAccountName;

    @Column(name = "IBAN", length = 34)
    private String iban;

    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_METHOD", length = 32)
    private Guest.PaymentMethod paymentMethod;

    @Column(name = "CAR_PLATE", length = 32)
    private String carPlate;

    @Column(name = "TRAILER_PLATE", length = 32)
    private String trailerPlate;

    /**
     * Cards and house card sets are sent to another address than the guest's.
     */
    @Column(name = "HAS_DELIVERY_CONTACT", nullable = false)
    private boolean hasDeliveryContact;

    @Column(name = "DELIVERY_NAME", length = 255)
    private String deliveryName;

    @Column(name = "DELIVERY_STREET", length = 100)
    private String deliveryStreet;

    @Column(name = "DELIVERY_HOUSE_NO", length = 16)
    private String deliveryHouseNo;

    @Column(name = "DELIVERY_ZIP_CODE", length = 16)
    private String deliveryZipCode;

    @Column(name = "DELIVERY_CITY", length = 100)
    private String deliveryCity;

    @Column(name = "DELIVERY_COUNTRY_ISO", length = 2)
    private String deliveryCountryIso;

    @Column(name = "ERP_PARTNER_NO", length = 32)
    private String erpPartnerNo;

    @Column(name = "ERP_ACCOUNT_NO", length = 40)
    private String erpAccountNo;

    /**
     * Handed to the ERP to create partner and account; reset when the ERP asks for the order again.
     */
    @Column(name = "EXPORTED", nullable = false)
    private boolean exported;

    @Column(name = "EXPORT_DATE")
    private LocalDateTime exportDate;

    @Column(name = "REMARKS", length = 2000)
    private String remarks;

    @Column(name = "CREATE_DATE", nullable = false)
    private LocalDateTime createDate = LocalDateTime.now();

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @Column(name = "MOD_DATE")
    private LocalDateTime modDate;

    @Column(name = "ACTIVATION_DATE")
    private LocalDateTime activationDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("pos")
    private List<OrderItem> items = new ArrayList<>();

    public Order() {
    }

    /**
     * An order of a guest the network knows: its details are copied, its ERP numbers taken over.
     */
    public static Order forGuest(Guest guest, BillingAccount billingAccount) {
        Order order = new Order();
        order.group = guest.getGroup();
        order.guest = guest;
        order.billingAccount = billingAccount;
        order.copyDetailsFrom(guest);
        order.erpPartnerNo = guest.getErpPartnerNo();
        order.erpAccountNo = billingAccount != null ? billingAccount.getErpAccountNo() : null;
        return order;
    }

    /**
     * An order of a new guest: the details as entered, the guest is created with the order.
     */
    public static Order forNewGuest(Guest details) {
        Order order = new Order();
        order.group = details.getGroup();
        order.copyDetailsFrom(details);
        return order;
    }

    private void copyDetailsFrom(Guest details) {
        this.guestType = details.getGuestType();
        this.salutation = details.getSalutation();
        this.name = details.getName();
        this.firstName = details.getFirstName();
        this.initials = details.getInitials();
        this.organisationName = details.getOrganisationName();
        this.vatNumber = details.getVatNumber();
        this.street = details.getStreet();
        this.houseNo = details.getHouseNo();
        this.houseNoAdd = details.getHouseNoAdd();
        this.zipCode = details.getZipCode();
        this.city = details.getCity();
        this.countryIso = details.getCountryIso();
        this.language = details.getLanguage();
        this.email = details.getEmail();
        this.phone = details.getPhone();
        this.mobile = details.getMobile();
        this.bankAccountName = details.getBankAccountName();
        this.iban = details.getIban();
        this.paymentMethod = details.getPaymentMethod();
        this.carPlate = details.getCarPlate();
        this.trailerPlate = details.getTrailerPlate();
    }

    /**
     * The details of the order as a guest: creates the guest of a new-guest order, or updates the known guest.
     */
    public Guest.Builder toGuestDetails() {
        return new Guest.Builder(group, guestType, name)
                .salutation(salutation)
                .firstName(firstName)
                .initials(initials)
                .organisationName(organisationName)
                .vatNumber(vatNumber)
                .street(street)
                .houseNo(houseNo)
                .houseNoAdd(houseNoAdd)
                .zipCode(zipCode)
                .city(city)
                .countryIso(countryIso)
                .language(language)
                .email(email)
                .phone(phone)
                .mobile(mobile)
                .bankAccountName(bankAccountName)
                .iban(iban)
                .paymentMethod(paymentMethod)
                .carPlate(carPlate)
                .trailerPlate(trailerPlate)
                .referenceNo(orderNumber);
    }

    public OrderItem addItem(ProductDefinition product, int quantity) {
        if (status != OrderStatus.NEW) {
            throw new IllegalStateException("Order " + orderNumber + " is " + status + ", no items can be added");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1: " + quantity);
        }
        OrderItem item = new OrderItem(this, items.size() + 1, product, quantity);
        items.add(item);
        return item;
    }

    public boolean hasErpNumbers() {
        return erpPartnerNo != null && !erpPartnerNo.isBlank() && erpAccountNo != null && !erpAccountNo.isBlank();
    }

    public boolean containsSeasonalPackage() {
        for (OrderItem item : items) {
            if (item.isSeasonalPackage()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fixed amount of all items (the ERP bills it with the first bill).
     */
    public BigDecimal getFixedAmount() {
        BigDecimal sum = BigDecimal.ZERO;
        for (OrderItem item : items) {
            sum = sum.add(item.getFixedAmount());
        }
        return sum;
    }

    /**
     * The order is complete: with both ERP numbers it can be activated at once, otherwise it waits for the ERP.
     *
     * @return true when the order can be activated now
     */
    public boolean submit(LocalDateTime at) {
        if (status != OrderStatus.NEW) {
            throw new IllegalStateException("Order " + orderNumber + " is " + status + ", it cannot be submitted");
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("Order " + orderNumber + " has no items");
        }
        this.modDate = at;
        if (hasErpNumbers()) {
            return true;
        }
        this.status = OrderStatus.AWAITING_ERP_ACCOUNT;
        this.exported = false;
        return false;
    }

    /**
     * The ERP created partner and account. Numbers already set are overwritten when the ERP sends others.
     */
    public void takeErpNumbers(String partnerNo, String accountNo, LocalDateTime at) {
        if (status != OrderStatus.AWAITING_ERP_ACCOUNT) {
            throw new IllegalStateException("Order " + orderNumber + " is " + status + ", it awaits no ERP numbers");
        }
        if (partnerNo == null || partnerNo.isBlank() || accountNo == null || accountNo.isBlank()) {
            throw new IllegalArgumentException("ERP partner and account number are both needed for order "
                    + orderNumber);
        }
        if (!Objects.equals(partnerNo, erpPartnerNo)) {
            this.erpPartnerNo = partnerNo;
        }
        if (!Objects.equals(accountNo, erpAccountNo)) {
            this.erpAccountNo = accountNo;
        }
        this.modDate = at;
    }

    public void markExported(LocalDateTime at) {
        this.exported = true;
        this.exportDate = at;
    }

    public void activate(Guest activeGuest, BillingAccount account, LocalDateTime at) {
        if (!status.isOpen()) {
            throw new IllegalStateException("Order " + orderNumber + " is " + status + ", it cannot be activated");
        }
        if (!hasErpNumbers()) {
            throw new IllegalStateException("Order " + orderNumber + " has no ERP numbers yet");
        }
        this.guest = activeGuest;
        this.billingAccount = account;
        this.status = OrderStatus.ACTIVE;
        this.activationDate = at;
        this.modDate = at;
    }

    public void cancel(String reason, LocalDateTime at) {
        if (!status.isOpen()) {
            throw new IllegalStateException("Order " + orderNumber + " is " + status + ", it cannot be cancelled");
        }
        this.status = OrderStatus.CANCELLED;
        this.remarks = remarks == null || remarks.isBlank() ? "Cancelled: " + reason
                : remarks + "\nCancelled: " + reason;
        this.modDate = at;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Order{id=" + id + ", number='" + orderNumber + "', " + status + ", name='" + name + "', partner="
                + erpPartnerNo + ", account=" + erpAccountNo + ", exported=" + exported + "}";
    }
}
