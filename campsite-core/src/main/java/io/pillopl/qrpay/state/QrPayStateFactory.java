package io.pillopl.qrpay.state;

import io.pillopl.persistence.dao.QrPayProcessDao;
import io.pillopl.qrpay.QrPayProcess;
import io.pillopl.qrpay.QrPayStateName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Makes the state of a QR payment process from its current row: one prototype bean per state name, holding a
 * detached copy of the row (a state changes its copy, the transition writes it as the next row).
 */
@Component
public class QrPayStateFactory implements ApplicationContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(QrPayStateFactory.class);

    private final QrPayProcessDao qrPayProcessDao;

    private ApplicationContext context;

    public QrPayStateFactory(QrPayProcessDao qrPayProcessDao) {
        this.qrPayProcessDao = qrPayProcessDao;
    }

    /**
     * First row of a new process, committed at once when called outside a transaction (the pedestal must find the
     * process when it reports the remote supply start).
     */
    @Transactional
    public QrPayState startProcess(String orderReference, Long socketId, String clientUsername, String appUserId,
                                   LocalDateTime requestDate, String priceInformation) {
        QrPayProcess process = new QrPayProcess(orderReference, socketId, clientUsername, appUserId, requestDate);
        Long id = qrPayProcessDao.nextRowId();
        process.setId(id);
        process.setProcessId(id);
        process.setPriceInformation(priceInformation);
        qrPayProcessDao.saveAndFlush(process);
        QrPayState state = getState(process.detachedCopy());
        LOG.debug(String.format("Started QR payment %s", state));
        return state;
    }

    /**
     * @return the state of the current row of the process, null when there is none (unknown or archived)
     */
    @Transactional(readOnly = true)
    public QrPayState createState(Long processId) {
        QrPayProcess current = processId != null ? qrPayProcessDao.findCurrentByProcessId(processId) : null;
        return current != null ? getState(current.detachedCopy()) : null;
    }

    @Transactional(readOnly = true)
    public QrPayState createStateByOrderReference(String orderReference) {
        QrPayProcess current = orderReference != null
                ? qrPayProcessDao.findCurrentByOrderReference(orderReference) : null;
        return current != null ? getState(current.detachedCopy()) : null;
    }

    public QrPayState getState(QrPayProcess process) {
        if (process == null) {
            return null;
        }
        QrPayState state;
        QrPayStateName stateName = process.getStateName();
        switch (stateName) {
            case START_REQUESTED:
                state = context.getBean(QrPayStartRequestedState.class);
                break;
            case SOCKET_START_WAITING:
                state = context.getBean(SocketStartWaitingState.class);
                break;
            case SOCKET_START_RECEIVED:
                state = context.getBean(SocketStartReceivedState.class);
                break;
            case START_CONFIRMED:
                state = context.getBean(QrPayStartConfirmedState.class);
                break;
            case STOP_REQUESTED:
                state = context.getBean(QrPayStopRequestedState.class);
                break;
            case SOCKET_STOP_WAITING:
                state = context.getBean(SocketStopWaitingState.class);
                break;
            case SOCKET_STOP_RECEIVED:
                state = context.getBean(SocketStopReceivedState.class);
                break;
            case CALCULATION_REQUESTED:
                state = context.getBean(QrPayCalculationRequestedState.class);
                break;
            case CALCULATION_DONE:
                state = context.getBean(QrPayCalculationDoneState.class);
                break;
            case CLOSED:
                state = context.getBean(QrPayClosedState.class);
                break;
            case ERROR:
                state = context.getBean(QrPayErrorState.class);
                break;
            default:
                throw new UnsupportedOperationException("Unknown state of QR payment " + process);
        }
        state.setProcess(process);
        LOG.debug(String.format("Created QR payment state %s", state));
        return state;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }
}
