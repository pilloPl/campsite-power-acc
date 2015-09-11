package io.pillopl.card;

import io.pillopl.guest.BillingAccount;
import io.pillopl.guest.Guest;
import io.pillopl.issuer.PartnerSyncState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * RFID card of a guest. Issued at the reception for a stay (valid from arrival to departure day) or for the season
 * of a seasonal resident, or delivered by an issuer that has an acceptance agreement with the network.
 * <p>
 * {@code cardRfid} is the UID the pedestal readers send (decimal), {@code cardNumber} is printed on the card.
 * Consumption on the card is billed to {@code billingAccountNo} of the guest.
 */
@Entity
@Table(name = "CAMPING_CARD")
@Getter
@Setter
public class CampingCard {

    static final int BILLING_ACCOUNT_NO_MIN_LENGTH = 6;

    static final int BILLING_ACCOUNT_NO_MAX_LENGTH = 20;

    /**
     * Builder for new cards: UID and printed number are mandatory.
     */
    public static class Builder {

        private final String cardRfid;

        private final String cardNumber;

        private CardActivation activation = CardActivation.ACTIVATED;

        private Guest guest;

        private LocalDate validFrom;

        private LocalDate validTo;

        private String billingAccountNo;

        private PartnerSyncState partnerSyncState = PartnerSyncState.ADD;

        public Builder(String cardRfid, String cardNumber) {
            this.cardRfid = cardRfid;
            this.cardNumber = cardNumber;
        }

        public Builder activation(CardActivation activation) {
            this.activation = activation;
            return this;
        }

        public Builder guest(Guest guest) {
            this.guest = guest;
            return this;
        }

        public Builder validFrom(LocalDate validFrom) {
            this.validFrom = validFrom;
            return this;
        }

        public Builder validTo(LocalDate validTo) {
            this.validTo = validTo;
            return this;
        }

        public Builder billingAccountNo(String billingAccountNo) {
            this.billingAccountNo = billingAccountNo;
            return this;
        }

        public Builder partnerSyncState(PartnerSyncState partnerSyncState) {
            this.partnerSyncState = partnerSyncState;
            return this;
        }

        public CampingCard build() {
            return new CampingCard(this);
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_camping_card")
    @SequenceGenerator(name = "seq_camping_card", sequenceName = "CAMPING_CARD_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "CARD_RFID", length = 64, nullable = false)
    private String cardRfid;

    @Column(name = "CARD_NUMBER", length = 64, nullable = false)
    private String cardNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 16, nullable = false)
    private CardActivation activation = CardActivation.ACTIVATED;

    @ManyToOne
    @JoinColumn(name = "GUEST_ID")
    private Guest guest;

    @Column(name = "VALID_FROM")
    private LocalDate validFrom;

    @Column(name = "VALID_TO")
    private LocalDate validTo;

    @Column(name = "BILLING_ACCOUNT_NO", length = 40)
    private String billingAccountNo;

    @Column(name = "DELETED", nullable = false)
    private boolean deleted;

    @Column(name = "CREATE_DATE", nullable = false)
    private LocalDateTime createDate = LocalDateTime.now();

    @Column(name = "UPLOAD_DATE")
    private LocalDateTime uploadDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "PARTNER_SYNC_STATE", length = 16, nullable = false)
    private PartnerSyncState partnerSyncState = PartnerSyncState.ADD;

    @Enumerated(EnumType.STRING)
    @Column(name = "TARGET_PARTNER_SYNC_STATE", length = 16)
    private PartnerSyncState targetPartnerSyncState;

    public CampingCard() {
    }

    private CampingCard(Builder builder) {
        this.cardRfid = builder.cardRfid;
        this.cardNumber = builder.cardNumber;
        this.activation = builder.activation;
        this.guest = builder.guest;
        this.validFrom = builder.validFrom;
        this.validTo = builder.validTo;
        this.billingAccountNo = builder.billingAccountNo;
        this.partnerSyncState = builder.partnerSyncState;
    }

    /**
     * Validity of the card at a moment (the pedestal's remote date). The first failing check wins: deactivated card,
     * stay or season not begun, stay or season over (the departure day is still valid), no guest, guest deactivated,
     * billing account missing, malformed, or unknown to a guest whose billing accounts the network keeps.
     */
    public CardStatus validate(final LocalDateTime validationDate) {
        if (activation == CardActivation.DEACTIVATED) {
            return CardStatus.CARD_DEACTIVATED;
        }
        LocalDate validationDay = validationDate != null ? validationDate.toLocalDate() : null;
        if (validationDay != null && validFrom != null && validFrom.isAfter(validationDay)) {
            return CardStatus.NOT_VALID_YET;
        }
        if (validationDay != null && validTo != null && validTo.isBefore(validationDay)) {
            return CardStatus.NOT_VALID_ANYMORE;
        }
        if (guest == null) {
            return CardStatus.NO_GUEST;
        }
        if (guest.isDeactivated()) {
            return CardStatus.GUEST_DEACTIVATED;
        }
        if (!isValidBillingAccountNo(billingAccountNo)) {
            return CardStatus.BILLING_ACCOUNT_INVALID;
        }
        if (!isBillingAccountOfGuest()) {
            return CardStatus.BILLING_ACCOUNT_INVALID;
        }

        return CardStatus.VALID;
    }

    /**
     * The guest's billing accounts kept by the network must contain the card's account, open and known to the ERP.
     * Guests of issuers that bill their members themselves have no accounts here; their cards carry the issuer's
     * account number and only its format is checked.
     */
    boolean isBillingAccountOfGuest() {
        if (guest.getBillingAccounts().isEmpty()) {
            return true;
        }
        BillingAccount account = guest.findBillingAccount(billingAccountNo);
        return account != null && account.isBillable();
    }

    /**
     * A billing account number of the ERP: 6 to 20 digits.
     */
    public static boolean isValidBillingAccountNo(String billingAccountNo) {
        if (billingAccountNo == null || billingAccountNo.isBlank()) {
            return false;
        }
        if (billingAccountNo.length() < BILLING_ACCOUNT_NO_MIN_LENGTH
                || billingAccountNo.length() > BILLING_ACCOUNT_NO_MAX_LENGTH) {
            return false;
        }
        for (int i = 0; i < billingAccountNo.length(); i++) {
            if (!Character.isDigit(billingAccountNo.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * The card wants the change {@code partnerSyncState} towards the issuer partners; what is exported depends on
     * what the partners got last ({@link PartnerSyncState#getByTargetPartnerSyncState}). No change means UNKNOWN.
     */
    public void calculatePartnerSyncState(PartnerSyncState partnerSyncState) {
        if (partnerSyncState == null) {
            this.partnerSyncState = PartnerSyncState.UNKNOWN;
        } else {
            this.partnerSyncState = partnerSyncState.getByTargetPartnerSyncState(targetPartnerSyncState);
        }
    }

    /**
     * The partners got the card's state with the export at {@code uploadDate}: nothing more to export.
     */
    public void partnerSyncExported(LocalDateTime uploadDate) {
        this.targetPartnerSyncState = this.partnerSyncState;
        this.partnerSyncState = PartnerSyncState.NOT;
        this.uploadDate = uploadDate;
    }

    public boolean isActive() {
        return !deleted && activation == CardActivation.ACTIVATED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CampingCard other)) return false;
        return Objects.equals(cardRfid, other.cardRfid) && Objects.equals(cardNumber, other.cardNumber)
                && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardRfid, cardNumber);
    }

    @Override
    public String toString() {
        return "CampingCard{id=" + id + ", rfid='" + cardRfid + "', number='" + cardNumber + "', validFrom="
                + validFrom + ", validTo=" + validTo + ", " + activation + "}";
    }
}
