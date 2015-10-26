package io.pillopl.issuer;

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

import java.time.LocalDateTime;

/**
 * The last run of one direction of the exchange with a partner. {@code lastSuccess} is the watermark of the record
 * export.
 */
@Entity
@Table(name = "ISSUER_SYNC_STATUS")
@Getter
@Setter
public class IssuerSyncStatus {

    static final int MESSAGE_MAX_LENGTH = 1000;

    public enum Direction {
        CARD_IMPORT,
        CARD_EXPORT,
        RECORD_EXPORT,
        RECORD_IMPORT
    }

    public enum Status {
        OK,
        ERROR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_issuer_sync_status")
    @SequenceGenerator(name = "seq_issuer_sync_status", sequenceName = "ISSUER_SYNC_STATUS_ID_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ISSUER_PARTNER_ID", nullable = false)
    private IssuerPartner issuerPartner;

    @Enumerated(EnumType.STRING)
    @Column(name = "DIRECTION", length = 16, nullable = false)
    private Direction direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 8, nullable = false)
    private Status status = Status.OK;

    @Column(name = "LAST_RUN", nullable = false)
    private LocalDateTime lastRun;

    @Column(name = "LAST_SUCCESS")
    private LocalDateTime lastSuccess;

    @Column(name = "ITEMS_ACCEPTED", nullable = false)
    private int itemsAccepted;

    @Column(name = "ITEMS_REJECTED", nullable = false)
    private int itemsRejected;

    @Column(name = "MESSAGE", length = MESSAGE_MAX_LENGTH)
    private String message;

    public IssuerSyncStatus() {
    }

    IssuerSyncStatus(IssuerPartner issuerPartner, Direction direction) {
        this.issuerPartner = issuerPartner;
        this.direction = direction;
    }

    public void succeeded(LocalDateTime at, int accepted, int rejected, String message) {
        this.status = Status.OK;
        this.lastRun = at;
        this.lastSuccess = at;
        this.itemsAccepted = accepted;
        this.itemsRejected = rejected;
        this.message = cut(message);
    }

    /**
     * The run failed as a whole (partner not reachable, unreadable file): the watermark stays.
     */
    public void failed(LocalDateTime at, String message) {
        this.status = Status.ERROR;
        this.lastRun = at;
        this.itemsAccepted = 0;
        this.itemsRejected = 0;
        this.message = cut(message);
    }

    private static String cut(String message) {
        if (message == null || message.length() <= MESSAGE_MAX_LENGTH) {
            return message;
        }
        return message.substring(0, MESSAGE_MAX_LENGTH);
    }

    @Override
    public String toString() {
        return "IssuerSyncStatus{" + direction + ", " + status + ", lastRun=" + lastRun + ", accepted="
                + itemsAccepted + ", rejected=" + itemsRejected + "}";
    }
}
