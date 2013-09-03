package io.pillopl.card;

import io.pillopl.group.attribute.GroupAttribute;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Own card of a campsite, attached to its SITE group: accepted without further validation at every pedestal of the
 * group's subtree. A group may have many house cards.
 */
@Entity
@Table(name = "HOUSECARD")
@Getter
@Setter
public class HouseCard extends GroupAttribute {

    @Enumerated(EnumType.STRING)
    @Column(name = "HOUSE_CARD_TYPE", length = 16, nullable = false)
    private HouseCardType houseCardType;

    @Column(name = "RFID", length = 64, nullable = false)
    private String rfid;

    @Column(name = "CARD_NUMBER", length = 64)
    private String cardNumber;

    @Column(name = "CREATION_DATE", nullable = false)
    private LocalDateTime creationDate;

    @Column(name = "LAST_USED")
    private LocalDateTime lastUsed;

    @Column(name = "DELETED_DATE")
    private LocalDateTime deletedDate;

    @Column(name = "ACTIVE", nullable = false)
    private boolean active = true;

    @Column(name = "DELETED", nullable = false)
    private boolean deleted;

    public HouseCard() {
        this.creationDate = LocalDateTime.now();
    }

    public HouseCard(HouseCardType houseCardType, String rfid, String cardNumber) {
        this();
        this.houseCardType = houseCardType;
        this.rfid = rfid;
        this.cardNumber = cardNumber;
    }

    public boolean isRental() {
        return houseCardType == HouseCardType.RENTAL;
    }

    public void markDeleted(LocalDateTime at) {
        this.deleted = true;
        this.active = false;
        this.deletedDate = at;
    }

    @Override
    public String toString() {
        return "HouseCard{id=" + getId() + ", groupId=" + getGroupId() + ", " + houseCardType + ", rfid='" + rfid
                + "', active=" + active + ", deleted=" + deleted + "}";
    }
}
