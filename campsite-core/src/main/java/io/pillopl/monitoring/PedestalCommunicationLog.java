package io.pillopl.monitoring;

import io.pillopl.infrastructure.Pedestal;
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

import java.time.LocalDateTime;

/**
 * A logged message of a pedestal (boot, consumption record) with the event attributes as JSON. Written only when the
 * communication log is switched on for the SITE subtree of the pedestal.
 */
@Entity
@Table(name = "PEDESTAL_COMM_LOG")
@Getter
@Setter
public class PedestalCommunicationLog {

    public enum LogType {
        BOOT,
        CONSUMPTION_RECORD
    }

    public static final int MESSAGE_LENGTH = 4000;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_pedestal_comm_log")
    @SequenceGenerator(name = "seq_pedestal_comm_log", sequenceName = "PEDESTAL_COMM_LOG_ID_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PEDESTAL_ID", nullable = false)
    private Pedestal pedestal;

    @Column(name = "LOG_DATE", nullable = false)
    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(name = "LOG_TYPE", nullable = false, length = 32)
    private LogType logType;

    @Column(name = "MESSAGE", length = MESSAGE_LENGTH)
    private String message;

    public PedestalCommunicationLog() {
    }

    public PedestalCommunicationLog(Pedestal pedestal, LocalDateTime date, LogType logType, String message) {
        this.pedestal = pedestal;
        this.date = date;
        this.logType = logType;
        this.message = message != null && message.length() > MESSAGE_LENGTH
                ? message.substring(0, MESSAGE_LENGTH) : message;
    }
}
