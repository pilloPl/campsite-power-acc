package io.pillopl.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
 * Socket of a pedestal: a CEE (or Schuko) socket with its kWh meter and breaker. The meter value is the last reading
 * received from the pedestal. A "free socket" supplies without a card (no billing).
 */
@Entity
@Table(name = "SOCKET")
@Getter
@Setter
public class Socket {

    public static final int STD_MAX_CURRENT_IN_AMPERES = 16;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_socket")
    @SequenceGenerator(name = "seq_socket", sequenceName = "SOCKET_ID_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PEDESTAL_ID", nullable = false)
    private Pedestal pedestal;

    @ManyToOne(optional = false)
    @JoinColumn(name = "SOCKET_TYPE_ID", nullable = false)
    private SocketType socketType;

    @Column(name = "NAME", length = 128, nullable = false)
    private String name;

    @Column(name = "POS", nullable = false)
    private int position;

    /**
     * Number of the socket in the pedestal link messages (1..6).
     */
    @Column(name = "SOCKET_NO")
    private Integer socketNo;

    @Column(name = "SOCKET_CODE", length = 64, unique = true)
    private String socketCode;

    @Column(name = "PITCH_NO", length = 16)
    private String pitchNo;

    @Column(name = "METER_KWH", precision = 19, scale = 4, nullable = false)
    private BigDecimal meterKwh = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 32, nullable = false)
    private SocketStatus status = SocketStatus.UNKNOWN;

    @Column(name = "MOD_DATE", nullable = false)
    private LocalDateTime modDate = LocalDateTime.now();

    @Column(name = "LAST_STATUS_UPDATE")
    private LocalDateTime lastStatusUpdate;

    @Column(name = "FIRMWARE_VERSION", length = 64)
    private String firmwareVersion;

    @Column(name = "MONITORING", nullable = false)
    private boolean monitoring;

    @Column(name = "FREE_SOCKET", nullable = false)
    private boolean freeSocket;

    @Column(name = "DELETED", nullable = false)
    private boolean deleted;

    public Socket() {
    }

    public Socket(String name, SocketType socketType, int position) {
        this.name = name;
        this.socketType = socketType;
        this.position = position;
        this.socketNo = position;
    }

    public boolean isAssignedToPitch() {
        return pitchNo != null && !pitchNo.isBlank();
    }

    public BreakerRating getBreakerRating() {
        return socketType != null ? socketType.getRating() : null;
    }

    /**
     * Maximum current the socket may draw: the breaker rating, or the standard 16 A when the type has none.
     */
    public int getMaxCurrentInAmperes() {
        BreakerRating rating = getBreakerRating();
        return rating != null ? rating.getAmperes() : STD_MAX_CURRENT_IN_AMPERES;
    }

    public void updateStatus(SocketStatus newStatus, LocalDateTime at) {
        this.status = newStatus;
        this.lastStatusUpdate = at;
        this.modDate = at;
    }

    @Override
    public String toString() {
        return "Socket{id=" + id + ", name='" + name + "', code='" + socketCode + "', pitch='" + pitchNo + "', "
                + status + "}";
    }
}
