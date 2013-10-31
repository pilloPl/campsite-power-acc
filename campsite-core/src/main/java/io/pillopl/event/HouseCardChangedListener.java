package io.pillopl.event;

import io.pillopl.card.LocalCardListService;
import org.springframework.stereotype.Component;

/**
 * Sends a changed house card to the local card lists of its campsite's pedestals.
 */
@Component
public class HouseCardChangedListener extends EventListener {

    private final LocalCardListService localCardListService;

    public HouseCardChangedListener(EventService eventService, EventTaskExecutorFactory executorFactory,
                                    LocalCardListService localCardListService) {
        super(eventService, executorFactory);
        this.localCardListService = localCardListService;
    }

    @Override
    public String getName() {
        return "houseCardChangedListener";
    }

    @Override
    public EventType getType() {
        return EventType.HOUSE_CARD_CHANGED;
    }

    @Override
    public void handleEvent(Event event) {
        execute(new EventTask(event) {
            @Override
            public void handleEvent() {
                localCardListService.updateLocalListWith(HouseCardChangedEvent.getHouseCardId(getEvent()));
            }
        });
    }
}
