package io.pillopl.supply;

import io.pillopl.card.CampingCard;
import io.pillopl.card.HouseCard;
import io.pillopl.infrastructure.Socket;
import io.pillopl.value.QrPayAuthTag;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Supply of a socket for a camping card or a house card, from the pedestal's supplyStarted to its supplyStopped
 * (or to the job closing it). The start meter and the remote date are the pedestal's values; the transaction id is
 * what the pedestal quotes when it stops.
 */
@Entity
@Table(name = "SUPPLY_SESSION")
@Getter
@Setter
public class SupplySession {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_supply_session")
    @SequenceGenerator(name = "seq_supply_session", sequenceName = "SUPPLY_SESSION_ID_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "SOCKET_ID", nullable = false)
    private Socket socket;

    @ManyToOne
    @JoinColumn(name = "CARD_ID")
    private CampingCard card;

    @ManyToOne
    @JoinColumn(name = "HOUSECARD_ID")
    private HouseCard houseCard;

    @Column(name = "TRANSACTION_ID", nullable = false, unique = true)
    private Integer transactionId;

    @Column(name = "START_METER_KWH", precision = 19, scale = 4)
    private BigDecimal startMeterKwh;

    @Column(name = "REMOTE_DATE")
    private LocalDateTime remoteDate;

    @Column(name = "CREATE_DATE", nullable = false)
    private LocalDateTime createDate;

    @Column(name = "ACTIVE", nullable = false)
    private boolean active;

    @Column(name = "MAX_CURRENT_A", nullable = false)
    private int maxCurrentA;

    @Enumerated(EnumType.STRING)
    @Column(name = "STOP_REASON", length = 32)
    private StopReason stopReason;

    @Column(name = "STOP_DATE")
    private LocalDateTime stopDate;

    /**
     * Stay of the guest the session belongs to (set once stays exist); its check-out closes the session.
     */
    @Column(name = "STAY_ID")
    private Long stayId;

    /**
     * Plate of the camper or caravan supplied, when the guest registered one.
     */
    @Column(name = "UNIT_PLATE", length = 32)
    private String unitPlate;

    /**
     * Process of the QR payment that started the supply remotely (session without card).
     */
    @Column(name = "QRPAY_PROCESS_ID")
    private Long qrPayProcessId;

    public SupplySession() {
    }

    public boolean isForHouseCard() {
        return houseCard != null;
    }

    public boolean isForQrPay() {
        return qrPayProcessId != null;
    }

    /**
     * UID of the card the session was started with (camping card or house card), or the auth tag of the QR payment's
     * remote start.
     */
    public String getCardRfid() {
        if (houseCard != null) {
            return houseCard.getRfid();
        }
        if (card == null && qrPayProcessId != null) {
            return QrPayAuthTag.of(qrPayProcessId).value();
        }
        return card != null ? card.getCardRfid() : null;
    }

    public boolean isStartedWith(String cardRfid) {
        String own = getCardRfid();
        return own != null && own.equals(cardRfid);
    }

    /**
     * The session is supplying since longer than the timeout and was not stopped by the pedestal.
     */
    public boolean isStale(LocalDateTime now, int timeoutInHours) {
        LocalDateTime start = remoteDate != null ? remoteDate : createDate;
        return active && start != null && start.plusHours(timeoutInHours).isBefore(now);
    }

    public void close(StopReason reason, LocalDateTime at) {
        this.stopReason = reason;
        this.stopDate = at;
        this.active = false;
    }

    @Override
    public String toString() {
        return "SupplySession{id=" + id + ", tx=" + transactionId + ", socket="
                + (socket != null ? socket.getSocketCode() : null) + ", card=" + getCardRfid() + ", start="
                + startMeterKwh + "@" + remoteDate + ", active=" + active + ", reason=" + stopReason + "}";
    }
}
