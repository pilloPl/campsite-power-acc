package io.pillopl.qrpay;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row of a QR payment process: the process (the payment app's order for one supply) is a chain of rows, one
 * per state. A transition inserts a copy with the new state and links the old row to it; the current row has no
 * successor. The row ids come from QRPAY_PROCESS_ID_SEQ and are assigned by the service, so the first row can carry
 * its own id as {@link #processId}.
 */
@Entity
@Table(name = "QRPAY_PROCESS")
@Getter
@Setter
public class QrPayProcess {

    @Id
    @Column(name = "ID")
    private Long id;

    /**
     * Id of the first row; the same in every row of the chain.
     */
    @Column(name = "PROCESS_ID", nullable = false)
    private Long processId;

    /**
     * Order id of the payment app.
     */
    @Column(name = "ORDER_REFERENCE", nullable = false, length = 64)
    private String orderReference;

    /**
     * Name of a {@link QrPayStateName}; the state factory picks the state class by it.
     */
    @Column(name = "STATE", nullable = false, length = 40)
    private String state;

    @Column(name = "SOCKET_ID", nullable = false)
    private Long socketId;

    /**
     * Login of the technical user of the payment app that started the process.
     */
    @Column(name = "CLIENT_USERNAME", nullable = false)
    private String clientUsername;

    /**
     * The guest as the payment app knows him.
     */
    @Column(name = "APP_USER_ID", nullable = false)
    private String appUserId;

    @Column(name = "REQUEST_DATE", nullable = false)
    private LocalDateTime requestDate;

    /**
     * Start of this row's state.
     */
    @Column(name = "START_TIME", nullable = false)
    private LocalDateTime startTime;

    /**
     * End of this row's state (set when the successor is inserted).
     */
    @Column(name = "STOP_TIME")
    private LocalDateTime stopTime;

    @Column(name = "PRICE_INFORMATION")
    private String priceInformation;

    @Column(name = "SUPPLY_SESSION_ID")
    private Long supplySessionId;

    @Column(name = "TRANSACTION_ID")
    private Integer transactionId;

    @Column(name = "KWH", precision = 19, scale = 4)
    private BigDecimal kwh;

    @Column(name = "DURATION_SECONDS")
    private Long durationSeconds;

    @Column(name = "COST", precision = 19, scale = 2)
    private BigDecimal cost;

    @Column(name = "CURRENCY", length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "ERROR_CODE", length = 40)
    private QrPayErrorCode errorCode;

    @Column(name = "ERROR_MESSAGE", length = 1024)
    private String errorMessage;

    @Column(name = "PREVIOUS_PROCESS_ID")
    private Long previousProcessId;

    @Column(name = "NEXT_PROCESS_ID")
    private Long nextProcessId;

    public QrPayProcess() {
    }

    public QrPayProcess(String orderReference, Long socketId, String clientUsername, String appUserId,
                        LocalDateTime requestDate) {
        this.orderReference = orderReference;
        this.socketId = socketId;
        this.clientUsername = clientUsername;
        this.appUserId = appUserId;
        this.requestDate = requestDate;
        this.startTime = requestDate;
        this.state = QrPayStateName.START_REQUESTED.name();
    }

    public QrPayStateName getStateName() {
        return QrPayStateName.fromString(state);
    }

    public boolean isCurrent() {
        return nextProcessId == null;
    }

    /**
     * The process was started by this client for this guest of the app.
     */
    public boolean isOwnedBy(String clientUsername, String appUserId) {
        return this.clientUsername != null && this.clientUsername.equalsIgnoreCase(clientUsername)
                && this.appUserId != null && this.appUserId.equals(appUserId);
    }

    /**
     * Copy of this row for the next state: everything but the id, the state and the links.
     */
    public QrPayProcess copyForNextState(Long newId, QrPayStateName newState, LocalDateTime now) {
        QrPayProcess copy = new QrPayProcess();
        copy.setId(newId);
        copy.setProcessId(processId);
        copy.setOrderReference(orderReference);
        copy.setState(newState.name());
        copy.setSocketId(socketId);
        copy.setClientUsername(clientUsername);
        copy.setAppUserId(appUserId);
        copy.setRequestDate(requestDate);
        copy.setStartTime(now);
        copy.setStopTime(null);
        copy.setPriceInformation(priceInformation);
        copy.setSupplySessionId(supplySessionId);
        copy.setTransactionId(transactionId);
        copy.setKwh(kwh);
        copy.setDurationSeconds(durationSeconds);
        copy.setCost(cost);
        copy.setCurrency(currency);
        copy.setErrorCode(errorCode);
        copy.setErrorMessage(errorMessage);
        copy.setPreviousProcessId(id);
        copy.setNextProcessId(null);
        return copy;
    }

    /**
     * Copy of this row as it is (the state works on it without touching the managed row).
     */
    public QrPayProcess detachedCopy() {
        QrPayProcess copy = copyForNextState(id, getStateName(), startTime);
        copy.setStopTime(stopTime);
        copy.setPreviousProcessId(previousProcessId);
        copy.setNextProcessId(nextProcessId);
        return copy;
    }

    @Override
    public String toString() {
        return "QrPayProcess{id=" + id + ", process=" + processId + ", order=" + orderReference + ", state=" + state
                + ", socket=" + socketId + ", session=" + supplySessionId + ", tx=" + transactionId + ", cost=" + cost
                + ", error=" + errorCode + ", next=" + nextProcessId + "}";
    }
}
