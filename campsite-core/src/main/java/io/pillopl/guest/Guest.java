package io.pillopl.guest;

import io.pillopl.group.Group;
import io.pillopl.group.IsLinkedToGroup;
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
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Guest of the network: holder of the billing account its camping cards are issued on. The guest hangs in the
 * ISSUER group of the card issuer; whoever manages that group manages the guest and its cards.
 * <p>
 * The details stay flat on the guest as the reception enters them (address, contacts, bank account, plates), and
 * an order copies them when the guest orders a seasonal contract or cards.
 */
@Entity
@Table(name = "GUEST")
@Getter
@Setter
public class Guest implements IsLinkedToGroup {

    /**
     * How the guest pays the consumption the ERP bills.
     */
    public enum PaymentMethod {
        RECEPTION,
        DIRECT_DEBIT,
        INVOICE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_guest")
    @SequenceGenerator(name = "seq_guest", sequenceName = "GUEST_ID_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "GROUP_ID", nullable = false)
    private Group group;

    @Enumerated(EnumType.STRING)
    @Column(name = "GUEST_TYPE", length = 16, nullable = false)
    private GuestType guestType = GuestType.TRANSIT;

    @Column(name = "NAME", length = 255, nullable = false)
    private String name;

    @Column(name = "FIRST_NAME", length = 255)
    private String firstName;

    @Column(name = "SALUTATION", length = 16)
    private String salutation;

    @Column(name = "INITIALS", length = 32)
    private String initials;

    @Column(name = "EMAIL", length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 16, nullable = false)
    private GuestStatus status = GuestStatus.ACTIVE;

    @Column(name = "ERP_PARTNER_NO", length = 32)
    private String erpPartnerNo;

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

    @Column(name = "PHONE", length = 32)
    private String phone;

    @Column(name = "MOBILE", length = 32)
    private String mobile;

    @Column(name = "EMAIL_NOTIFICATION", nullable = false)
    private boolean emailNotification;

    @Column(name = "SMS_NOTIFICATION", nullable = false)
    private boolean smsNotification;

    @Column(name = "BANK_ACCOUNT_NAME", length = 200)
    private String bankAccountName;

    @Column(name = "IBAN", length = 34)
    private String iban;

    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_METHOD", length = 32)
    private PaymentMethod paymentMethod;

    @Column(name = "ORGANISATION_NAME", length = 200)
    private String organisationName;

    @Column(name = "VAT_NUMBER", length = 32)
    private String vatNumber;

    @Column(name = "REFERENCE_NO", length = 100)
    private String referenceNo;

    /**
     * Plate of the car, read by the barrier camera.
     */
    @Column(name = "CAR_PLATE", length = 32)
    private String carPlate;

    /**
     * Plate of the caravan or trailer, when it has its own.
     */
    @Column(name = "TRAILER_PLATE", length = 32)
    private String trailerPlate;

    @Column(name = "REMARKS", length = 1000)
    private String remarks;

    @Column(name = "CREATE_DATE", nullable = false)
    private LocalDateTime createDate = LocalDateTime.now();

    @Column(name = "MOD_DATE")
    private LocalDateTime modDate;

    @Column(name = "DELETED", nullable = false)
    private boolean deleted;

    @OneToMany(mappedBy = "guest", cascade = CascadeType.ALL)
    @SQLRestriction("DELETED = FALSE")
    @OrderBy("id")
    private List<Unit> units = new ArrayList<>();

    public Guest() {
    }

    public Guest(Group group, GuestType guestType, String name) {
        this.group = group;
        this.guestType = guestType;
        this.name = name;
    }

    private Guest(Builder builder) {
        this(builder.group, builder.guestType, builder.name);
        this.firstName = builder.firstName;
        this.salutation = builder.salutation;
        this.initials = builder.initials;
        this.email = builder.email;
        this.street = builder.street;
        this.houseNo = builder.houseNo;
        this.houseNoAdd = builder.houseNoAdd;
        this.zipCode = builder.zipCode;
        this.city = builder.city;
        this.countryIso = builder.countryIso;
        this.language = builder.language;
        this.phone = builder.phone;
        this.mobile = builder.mobile;
        this.emailNotification = builder.emailNotification;
        this.smsNotification = builder.smsNotification;
        this.bankAccountName = builder.bankAccountName;
        this.iban = builder.iban;
        this.paymentMethod = builder.paymentMethod;
        this.organisationName = builder.organisationName;
        this.vatNumber = builder.vatNumber;
        this.referenceNo = builder.referenceNo;
        this.carPlate = builder.carPlate;
        this.trailerPlate = builder.trailerPlate;
        this.remarks = builder.remarks;
    }

    public boolean isDeactivated() {
        return status == GuestStatus.DEACTIVATED;
    }

    public boolean isSeasonal() {
        return guestType == GuestType.SEASONAL;
    }

    public String getFullName() {
        return firstName != null && !firstName.isBlank() ? firstName + " " + name : name;
    }

    /**
     * Street, house number and addition as printed on the invoice; null when the street is unknown.
     */
    public String getStreetLine() {
        if (street == null || street.isBlank()) {
            return null;
        }
        StringBuilder line = new StringBuilder(street.trim());
        if (houseNo != null && !houseNo.isBlank()) {
            line.append(' ').append(houseNo.trim());
            if (houseNoAdd != null && !houseNoAdd.isBlank()) {
                line.append(houseNoAdd.trim());
            }
        }
        return line.toString();
    }

    public boolean hasErpPartner() {
        return erpPartnerNo != null && !erpPartnerNo.isBlank();
    }

    /**
     * The guest can be billed by direct debit: bank account holder and IBAN are known.
     */
    public boolean isDirectDebitPossible() {
        return paymentMethod == PaymentMethod.DIRECT_DEBIT && iban != null && !iban.isBlank()
                && bankAccountName != null && !bankAccountName.isBlank();
    }

    /**
     * The plate the guest arrives with: the trailer's when it has one, otherwise the car's.
     */
    public String getUnitPlate() {
        if (trailerPlate != null && !trailerPlate.isBlank()) {
            return trailerPlate;
        }
        return carPlate != null && !carPlate.isBlank() ? carPlate : null;
    }

    public Unit addUnit(Unit.UnitType unitType, String plate, BigDecimal lengthM) {
        Unit unit = new Unit(this, unitType, plate, lengthM);
        units.add(unit);
        return unit;
    }

    /**
     * The guest's unit with a plate, or null: a guest with one camper or caravan needs no choice at check-in.
     */
    public Unit getOnlyUnitWithPlate() {
        Unit found = null;
        for (Unit unit : units) {
            if (unit.getPlate() != null) {
                if (found != null) {
                    return null;
                }
                found = unit;
            }
        }
        return found;
    }

    /**
     * Copies the details an order or the reception changed; null values keep the current detail.
     */
    public void updateDetails(Guest details, LocalDateTime now) {
        this.name = nvl(details.name, this.name);
        this.firstName = nvl(details.firstName, this.firstName);
        this.salutation = nvl(details.salutation, this.salutation);
        this.initials = nvl(details.initials, this.initials);
        this.email = nvl(details.email, this.email);
        this.street = nvl(details.street, this.street);
        this.houseNo = nvl(details.houseNo, this.houseNo);
        this.houseNoAdd = nvl(details.houseNoAdd, this.houseNoAdd);
        this.zipCode = nvl(details.zipCode, this.zipCode);
        this.city = nvl(details.city, this.city);
        this.countryIso = nvl(details.countryIso, this.countryIso);
        this.language = nvl(details.language, this.language);
        this.phone = nvl(details.phone, this.phone);
        this.mobile = nvl(details.mobile, this.mobile);
        this.bankAccountName = nvl(details.bankAccountName, this.bankAccountName);
        this.iban = nvl(details.iban, this.iban);
        this.paymentMethod = details.paymentMethod != null ? details.paymentMethod : this.paymentMethod;
        this.organisationName = nvl(details.organisationName, this.organisationName);
        this.vatNumber = nvl(details.vatNumber, this.vatNumber);
        this.referenceNo = nvl(details.referenceNo, this.referenceNo);
        this.carPlate = nvl(details.carPlate, this.carPlate);
        this.trailerPlate = nvl(details.trailerPlate, this.trailerPlate);
        this.remarks = nvl(details.remarks, this.remarks);
        this.modDate = now;
    }

    private static String nvl(String value, String current) {
        return value != null && !value.isBlank() ? value.trim() : current;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Guest other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Guest{id=" + id + ", name='" + getFullName() + "', " + guestType + ", " + status + ", erpPartnerNo="
                + erpPartnerNo + "}";
    }

    /**
     * Builder for guests the reception or an order creates: group, type and name are mandatory.
     */
    public static class Builder {

        private final Group group;

        private final GuestType guestType;

        private final String name;

        private String firstName;

        private String salutation;

        private String initials;

        private String email;

        private String street;

        private String houseNo;

        private String houseNoAdd;

        private String zipCode;

        private String city;

        private String countryIso;

        private String language;

        private String phone;

        private String mobile;

        private boolean emailNotification;

        private boolean smsNotification;

        private String bankAccountName;

        private String iban;

        private PaymentMethod paymentMethod;

        private String organisationName;

        private String vatNumber;

        private String referenceNo;

        private String carPlate;

        private String trailerPlate;

        private String remarks;

        public Builder(Group group, GuestType guestType, String name) {
            this.group = group;
            this.guestType = guestType;
            this.name = name;
        }

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder salutation(String salutation) {
            this.salutation = salutation;
            return this;
        }

        public Builder initials(String initials) {
            this.initials = initials;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder street(String street) {
            this.street = street;
            return this;
        }

        public Builder houseNo(String houseNo) {
            this.houseNo = houseNo;
            return this;
        }

        public Builder houseNoAdd(String houseNoAdd) {
            this.houseNoAdd = houseNoAdd;
            return this;
        }

        public Builder zipCode(String zipCode) {
            this.zipCode = zipCode;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
            return this;
        }

        public Builder countryIso(String countryIso) {
            this.countryIso = countryIso;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder mobile(String mobile) {
            this.mobile = mobile;
            return this;
        }

        public Builder emailNotification(boolean emailNotification) {
            this.emailNotification = emailNotification;
            return this;
        }

        public Builder smsNotification(boolean smsNotification) {
            this.smsNotification = smsNotification;
            return this;
        }

        public Builder bankAccountName(String bankAccountName) {
            this.bankAccountName = bankAccountName;
            return this;
        }

        public Builder iban(String iban) {
            this.iban = iban;
            return this;
        }

        public Builder paymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public Builder organisationName(String organisationName) {
            this.organisationName = organisationName;
            return this;
        }

        public Builder vatNumber(String vatNumber) {
            this.vatNumber = vatNumber;
            return this;
        }

        public Builder referenceNo(String referenceNo) {
            this.referenceNo = referenceNo;
            return this;
        }

        public Builder carPlate(String carPlate) {
            this.carPlate = carPlate;
            return this;
        }

        public Builder trailerPlate(String trailerPlate) {
            this.trailerPlate = trailerPlate;
            return this;
        }

        public Builder remarks(String remarks) {
            this.remarks = remarks;
            return this;
        }

        public Guest build() {
            return new Guest(this);
        }
    }
}
