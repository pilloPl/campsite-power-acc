package io.pillopl.qrpay.state;

import io.pillopl.event.Event;
import io.pillopl.event.EventAttribute;
import io.pillopl.event.EventService;
import io.pillopl.event.EventType;
import io.pillopl.infrastructure.PedestalService;
import io.pillopl.qrpay.QrPayCostService;
import io.pillopl.qrpay.QrPayErrorCode;
import io.pillopl.qrpay.QrPayProcess;
import io.pillopl.qrpay.QrPayProcessService;
import io.pillopl.qrpay.QrPayStateName;
import io.pillopl.supply.RemoteSupplyControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base of the QR payment state machine. Rules for the states:
 * <ul>
 * <li>by default every signal is refused: the process goes to ERROR (UNSUPPORTED_OPERATION) and the caller gets an
 * {@link IllegalStateException};</li>
 * <li>a state overrides only the signals it accepts and returns the state after the transition;</li>
 * <li>a state never signals itself, the supervisor (app calls) and the supply listeners drive the flow;</li>
 * <li>states are prototype beans made by the {@link QrPayStateFactory}, each holds a detached copy of the current
 * row.</li>
 * </ul>
 */
public abstract class QrPayState {

    private static final Logger LOG = LoggerFactory.getLogger(QrPayState.class);

    @Autowired
    protected QrPayProcessService qrPayProcessService;

    @Autowired
    protected QrPayCostService qrPayCostService;

    @Autowired
    protected RemoteSupplyControl remoteSupplyControl;

    @Autowired
    protected PedestalService pedestalService;

    @Autowired
    private EventService eventService;

    protected QrPayProcess process;

    public QrPayProcess getProcess() {
        return process;
    }

    void setProcess(QrPayProcess process) {
        this.process = process;
    }

    public QrPayStateName getStateName() {
        return process.getStateName();
    }

    /**
     * The state sent a command to the pedestal and its own transition may not be committed when the pedestal's
     * answer arrives: the caller should retry the signal a few times before treating it as wrong.
     */
    public boolean awaitsOwnTransition() {
        return false;
    }

    /**
     * Sends the remote start to the pedestal.
     */
    public QrPayState remoteSupplyStart() {
        return unsupported("remoteSupplyStart");
    }

    /**
     * The pedestal reported the start of the supply for the process.
     */
    public QrPayState supplyStarted(Long supplySessionId, Integer transactionId) {
        return unsupported("supplyStarted");
    }

    /**
     * The start is confirmed to the app: from now the app shows the supply as running.
     */
    public QrPayState confirmStart() {
        return unsupported("confirmStart");
    }

    /**
     * The app asks to stop. Ignored where the supply already stops.
     */
    public QrPayState stopRequested() {
        if (getStateName().isIgnoreStopRequest()) {
            LOG.warn(String.format("Ignoring stop request of QR payment %s in %s", process.getProcessId(),
                    getStateName()));
            return this;
        }
        return unsupported("stopRequested");
    }

    /**
     * Sends the remote stop to the pedestal. Ignored where the supply already stops.
     */
    public QrPayState remoteSupplyStop() {
        if (getStateName().isIgnoreStopRequest() && getStateName() != QrPayStateName.STOP_REQUESTED) {
            LOG.warn(String.format("Ignoring remote stop of QR payment %s in %s", process.getProcessId(),
                    getStateName()));
            return this;
        }
        return unsupported("remoteSupplyStop");
    }

    /**
     * The pedestal reported the stop of the supply.
     */
    public QrPayState supplyStopped(Long supplySessionId, Integer transactionId) {
        return unsupported("supplyStopped");
    }

    public QrPayState requestCalculation() {
        return unsupported("requestCalculation");
    }

    /**
     * Takes the price of the consumption record. Stays in the state while the record is not written yet.
     */
    public QrPayState calculate() {
        return unsupported("calculate");
    }

    public QrPayState close() {
        return unsupported("close");
    }

    /**
     * Moves the process to ERROR with the code and an internal message, and announces the error.
     */
    public QrPayState handleError(QrPayErrorCode errorCode, String message) {
        LOG.info(String.format("QR payment %s in %s goes to ERROR: %s %s", process.getProcessId(), getStateName(),
                errorCode, message));
        process.setErrorCode(errorCode);
        process.setErrorMessage(message);
        QrPayState errorState = qrPayProcessService.transAndSave(QrPayStateName.ERROR, process);
        sendErrorEvent(errorCode, message);
        return errorState;
    }

    protected QrPayState transitionTo(QrPayStateName stateName) {
        LOG.debug(String.format("QR payment %s: %s -> %s", process.getProcessId(), getStateName(), stateName));
        return qrPayProcessService.transAndSave(stateName, process);
    }

    private QrPayState unsupported(String signal) {
        String message = getClass().getSimpleName() + " does not support " + signal;
        handleError(QrPayErrorCode.UNSUPPORTED_OPERATION, message);
        throw new IllegalStateException(message + " (QR payment " + process.getOrderReference() + ")");
    }

    private void sendErrorEvent(QrPayErrorCode errorCode, String message) {
        try {
            Event event = new Event(EventType.QRPAY_ERROR);
            event.setAttribute(EventAttribute.ID, process.getProcessId());
            event.setAttribute(EventAttribute.SOCKET_ID, process.getSocketId());
            event.setAttribute(EventAttribute.STATUS, errorCode.name());
            event.setAttribute(EventAttribute.ERROR_MESSAGE, message);
            eventService.send(event);
        } catch (Exception e) {
            LOG.error("Error sending QR payment error event.", e);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + process + "}";
    }
}
