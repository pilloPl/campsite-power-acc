package io.pillopl.monitoring;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Acknowledgement of a live alert of the dashboard by a console user (the alert itself is derived from the data).
 */
@Entity
@Table(name = "CONSOLE_ALERT_ACK")
@Getter
@Setter
public class ConsoleAlertAck {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_console_alert_ack")
    @SequenceGenerator(name = "seq_console_alert_ack", sequenceName = "CONSOLE_ALERT_ACK_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "ALERT_KEY", length = 128, nullable = false, unique = true)
    private String alertKey;

    @Column(name = "ACKNOWLEDGED_BY", length = 100, nullable = false)
    private String acknowledgedBy;

    @Column(name = "ACK_DATE", nullable = false)
    private LocalDateTime ackDate;

    @Column(name = "ACK_COMMENT", length = 1000)
    private String comment;

    public ConsoleAlertAck() {
    }

    public ConsoleAlertAck(String alertKey, String acknowledgedBy, LocalDateTime ackDate, String comment) {
        this.alertKey = alertKey;
        this.acknowledgedBy = acknowledgedBy;
        this.ackDate = ackDate;
        this.comment = comment != null && !comment.isBlank() ? comment.trim() : null;
    }

    @Override
    public String toString() {
        return "ConsoleAlertAck{" + alertKey + " by " + acknowledgedBy + " at " + ackDate + "}";
    }
}
