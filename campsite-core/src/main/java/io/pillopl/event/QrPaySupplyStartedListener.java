package io.pillopl.event;

import io.pillopl.qrpay.QrPayProcessSupervisor;
import io.pillopl.value.QrPayAuthTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * A supply started with a QR payment auth tag: the payment records the session and confirms the start to the app.
 * Supplies of cards are not relevant for the QR payment.
 */
@Component
public class QrPaySupplyStartedListener extends EventListener {

    private static final Logger LOG = LoggerFactory.getLogger(QrPaySupplyStartedListener.class);

    private final QrPayProcessSupervisor qrPayProcessSupervisor;

    public QrPaySupplyStartedListener(EventService eventService, EventTaskExecutorFactory executorFactory,
                                      QrPayProcessSupervisor qrPayProcessSupervisor) {
        super(eventService, executorFactory);
        this.qrPayProcessSupervisor = qrPayProcessSupervisor;
    }

    @Override
    public String getName() {
        return "qrPaySupplyStartedListener";
    }

    @Override
    public EventType getType() {
        return EventType.SUPPLY_STARTED;
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
                LOG.debug(String.format("Supply of QR payment %s started: %s", processId, getEvent()));
                qrPayProcessSupervisor.onSupplyStarted(processId,
                        EventAttribute.SUPPLY_SESSION_ID.getLongValue(getEvent()),
                        EventAttribute.TRANSACTION_ID.getIntegerValue(getEvent()));
            }
        });
    }
}
