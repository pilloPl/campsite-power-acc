package io.pillopl.event;

import io.pillopl.monitoring.PedestalMailService;
import jakarta.mail.MessagingException;
import org.springframework.stereotype.Component;

/**
 * Mails the alert of a section switchboard whose power budget cannot be checked to the pedestal's service recipient.
 */
@Component
public class PowerSectionMisconfiguredListener extends EventListener {

    private final PedestalMailService pedestalMailService;

    public PowerSectionMisconfiguredListener(EventService eventService, EventTaskExecutorFactory executorFactory,
                                             PedestalMailService pedestalMailService) {
        super(eventService, executorFactory);
        this.pedestalMailService = pedestalMailService;
    }

    @Override
    public String getName() {
        return "powerSectionMisconfiguredListener";
    }

    @Override
    public EventType getType() {
        return EventType.POWER_SECTION_MISCONFIGURED;
    }

    @Override
    public void handleEvent(Event event) {
        execute(new EventTask(event) {
            @Override
            public void handleEvent() {
                try {
                    pedestalMailService.sendPowerSectionMisconfiguredMail(
                            EventAttribute.PEDESTAL_CODE.getStringValue(getEvent()),
                            PowerSectionMisconfiguredEvent.getPowerSectionId(getEvent()),
                            EventAttribute.SOCKET_NO.getIntegerValue(getEvent()),
                            PowerSectionMisconfiguredEvent.getProblem(getEvent()));
                } catch (MessagingException e) {
                    throw new IllegalStateException("Power section alert mail not sent", e);
                }
            }
        });
    }
}
