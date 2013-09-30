package io.pillopl.event;

import io.pillopl.group.attribute.WelcomeCardAdoptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Checks refused cards for a welcome card adoption, outside the pedestal's authorization call.
 */
@Component
public class WelcomeCardAdoptionListener extends EventListener {

    private static final Logger LOG = LoggerFactory.getLogger(WelcomeCardAdoptionListener.class);

    private final WelcomeCardAdoptionService welcomeCardAdoptionService;

    public WelcomeCardAdoptionListener(EventService eventService, EventTaskExecutorFactory executorFactory,
                                       WelcomeCardAdoptionService welcomeCardAdoptionService) {
        super(eventService, executorFactory);
        this.welcomeCardAdoptionService = welcomeCardAdoptionService;
    }

    @Override
    public String getName() {
        return "welcomeCardAdoptionListener";
    }

    @Override
    public EventType getType() {
        return EventType.CHECK_WELCOME_CARD_ADOPTION;
    }

    @Override
    public void handleEvent(Event event) {
        execute(new EventTask(event) {
            @Override
            public void handleEvent() {
                String pedestalCode = CheckWelcomeCardAdoptionEvent.getPedestalCode(getEvent());
                String rfid = CheckWelcomeCardAdoptionEvent.getRfid(getEvent());
                try {
                    welcomeCardAdoptionService.checkAndAdopt(pedestalCode, rfid);
                } catch (Exception e) {
                    LOG.error(String.format("Welcome card adoption check of %s at %s failed", rfid, pedestalCode), e);
                }
            }
        });
    }
}
