package io.pillopl.monitoring;

import io.pillopl.infrastructure.Socket;
import io.pillopl.infrastructure.SocketStatus;
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
 * One status period of a monitored socket. A new row starts when status, error flags or current change; otherwise
 * the last row is moved forward (update date, meter reading).
 */
@Entity
@Table(name = "SOCKET_STATUS_LOG")
@Getter
@Setter
public class SocketStatusLog {

    public static final long NO_ERROR = 0L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_socket_status_log")
    @SequenceGenerator(name = "seq_socket_status_log", sequenceName = "SOCKET_STATUS_LOG_ID_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "SOCKET_ID", nullable = false)
    private Socket socket;

    @Column(name = "CREATE_DATE", nullable = false)
    private LocalDateTime createDate;

    @Column(name = "UPDATE_DATE", nullable = false)
    private LocalDateTime updateDate;

    @Column(name = "REMOTE_DATE")
    private LocalDateTime remoteDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 32)
    private SocketStatus status;

    @Column(name = "METER_KWH", nullable = false, precision = 19, scale = 4)
    private BigDecimal meterKwh = BigDecimal.ZERO;

    @Column(name = "ERROR_FLAGS", nullable = false)
    private long errorFlags = NO_ERROR;

    @Column(name = "CURRENT_A", precision = 7, scale = 2)
    private BigDecimal currentA;

    public SocketStatusLog() {
    }

    public SocketStatusLog(Socket socket, LocalDateTime serverDate, LocalDateTime remoteDate) {
        this.socket = socket;
        this.createDate = serverDate;
        this.updateDate = serverDate;
        this.remoteDate = remoteDate;
    }

    /**
     * Status the socket returns to when a message carries no status: the last logged one, where the link and
     * authorization statuses count as available.
     */
    public static SocketStatus lastStatusOrAvailable(SocketStatusLog lastLog) {
        SocketStatus lastLogStatus = lastLog != null ? lastLog.getStatus() : SocketStatus.AVAILABLE;
        if (lastLogStatus == SocketStatus.UNKNOWN || lastLogStatus == SocketStatus.OCCUPIED) {
            lastLogStatus = SocketStatus.AVAILABLE;
        }
        return lastLogStatus;
    }

    @Override
    public String toString() {
        return "SocketStatusLog{id=" + id + ", " + status + ", flags=" + errorFlags + ", meter=" + meterKwh + "}";
    }
}
