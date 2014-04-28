package io.pillopl.supply;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A stop the pedestal sent and the pedestal backend could not book. The pedestal got ACCEPTED anyway (it must free
 * the socket); the row tells the service team which supply may be missing in the consumption records.
 */
@Entity
@Table(name = "SUPPLY_INCIDENT_LOG")
@Getter
@Setter
public class SupplyIncident {

    public enum IncidentType {
        NO_TRANSACTION_ID,
        NO_SESSION,
        SESSION_ALREADY_CLOSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_supply_incident")
    @SequenceGenerator(name = "seq_supply_incident", sequenceName = "SUPPLY_INCIDENT_LOG_ID_SEQ", allocationSize = 1)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "INCIDENT_TYPE", length = 32, nullable = false)
    private IncidentType incidentType;

    @Column(name = "PEDESTAL_CODE", length = 64)
    private String pedestalCode;

    @Column(name = "TRANSACTION_ID")
    private Integer transactionId;

    @Column(name = "CARD_RFID", length = 64)
    private String cardRfid;

    @Column(name = "SUPPLY_SESSION_ID")
    private Long supplySessionId;

    @Column(name = "METER_KWH", precision = 19, scale = 4)
    private BigDecimal meterKwh;

    @Column(name = "REMOTE_DATE")
    private LocalDateTime remoteDate;

    @Column(name = "MESSAGE", length = 1000, nullable = false)
    private String message;

    @Column(name = "CREATE_DATE", nullable = false)
    private LocalDateTime createDate;

    public SupplyIncident() {
    }

    public SupplyIncident(IncidentType incidentType, String pedestalCode, String message, LocalDateTime createDate) {
        this.incidentType = incidentType;
        this.pedestalCode = pedestalCode;
        this.message = message;
        this.createDate = createDate;
    }

    @Override
    public String toString() {
        return "SupplyIncident{" + incidentType + ", pedestal=" + pedestalCode + ", tx=" + transactionId + ", card="
                + cardRfid + ", '" + message + "'}";
    }
}
