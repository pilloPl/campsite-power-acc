package io.pillopl.event;

import io.pillopl.qrpay.QrPayProcessSupervisor;
import io.pillopl.value.QrPayAuthTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * A supply of a QR payment stopped (pedestal, remote stop or the closing job): the payment takes the price of the
 * consumption record and closes.
 */
@Component
public class QrPaySupplyStoppedListener extends EventListener {

    private static final Logger LOG = LoggerFactory.getLogger(QrPaySupplyStoppedListener.class);

    private final QrPayProcessSupervisor qrPayProcessSupervisor;

    public QrPaySupplyStoppedListener(EventService eventService, EventTaskExecutorFactory executorFactory,
                                      QrPayProcessSupervisor qrPayProcessSupervisor) {
        super(eventService, executorFactory);
        this.qrPayProcessSupervisor = qrPayProcessSupervisor;
    }

    @Override
    public String getName() {
        return "qrPaySupplyStoppedListener";
    }

    @Override
    public EventType getType() {
        return EventType.SUPPLY_STOPPED;
    }

    @Override
    public void handleEvent(Event event) {
        final Long processId = QrPayAuthTag.parse(EventAttribute.RFID.getStringValue(event));
        if (processId == null) {
            return;
        }
        execute(new EventTask(event) {
            @Override
            public void handleEvent() {
                LOG.debug(String.format("Supply of QR payment %s stopped: %s", processId, getEvent()));
                qrPayProcessSupervisor.onSupplyStopped(processId,
                        EventAttribute.SUPPLY_SESSION_ID.getLongValue(getEvent()),
                        EventAttribute.TRANSACTION_ID.getIntegerValue(getEvent()));
            }
        });
    }
}
