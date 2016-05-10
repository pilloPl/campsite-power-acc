package io.pillopl.event;

import io.pillopl.monitoring.PedestalMailService;
import jakarta.mail.MessagingException;
import org.springframework.stereotype.Component;

/**
 * Mails a QR payment that went to ERROR to the service recipient of its pedestal.
 */
@Component
public class QrPayErrorListener extends EventListener {

    private final PedestalMailService pedestalMailService;

    public QrPayErrorListener(EventService eventService, EventTaskExecutorFactory executorFactory,
                              PedestalMailService pedestalMailService) {
        super(eventService, executorFactory);
        this.pedestalMailService = pedestalMailService;
    }

    @Override
    public String getName() {
        return "qrPayErrorListener";
    }

    @Override
    public EventType getType() {
        return EventType.QRPAY_ERROR;
    }

    @Override
    public void handleEvent(Event event) {
        execute(new EventTask(event) {
            @Override
            public void handleEvent() {
                try {
                    pedestalMailService.sendQrPayErrorMail(EventAttribute.SOCKET_ID.getLongValue(getEvent()),
                            EventAttribute.ID.getLongValue(getEvent()),
                            EventAttribute.STATUS.getStringValue(getEvent()),
                            EventAttribute.ERROR_MESSAGE.getStringValue(getEvent()));
                } catch (MessagingException e) {
                    throw new IllegalStateException("QR payment error mail not sent", e);
                }
            }
        });
    }
}
