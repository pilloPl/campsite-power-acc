package io.pillopl.card;

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
