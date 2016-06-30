package io.pillopl.qrpay.state;

import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.Socket;
import io.pillopl.qrpay.QrPayErrorCode;
import io.pillopl.qrpay.QrPayStateName;
import io.pillopl.value.QrPayAuthTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * The app asked for the supply: the socket is started remotely with the process's auth tag.
 */
@Component
@Scope("prototype")
public class QrPayStartRequestedState extends QrPayState {

    private static final Logger LOG = LoggerFactory.getLogger(QrPayStartRequestedState.class);

    @Override
    public QrPayState remoteSupplyStart() {
        Socket socket = pedestalService.findSocket(process.getSocketId());
        Pedestal pedestal = pedestalService.findBySocketId(process.getSocketId());
        if (socket == null || pedestal == null || socket.getSocketNo() == null) {
            return handleError(QrPayErrorCode.SOCKET_UNKNOWN, "Socket " + process.getSocketId()
                    + " has no pedestal link");
        }
        String authTag = QrPayAuthTag.of(process.getProcessId()).value();
        LOG.info(String.format("Remote start of socket %s with %s for order %s", socket.getSocketCode(), authTag,
                process.getOrderReference()));
        if (!remoteSupplyControl.remoteSupplyStart(pedestal, socket.getSocketNo(), authTag)) {
            // the app gets the error with the answer to its start, no supply follows
            return handleError(QrPayErrorCode.REMOTE_SUPPLY_START_FAILED, "Pedestal " + pedestal.getCode()
                    + " did not accept the remote start of " + authTag);
        }
        return transitionTo(QrPayStateName.SOCKET_START_WAITING);
    }

    /**
     * The pedestal answers the remote start before its transition is committed.
     */
    @Override
    public boolean awaitsOwnTransition() {
        return true;
    }
}
