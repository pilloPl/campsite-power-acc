package io.pillopl.event;

/**
 * A card was refused at a pedestal because its campsite does not accept it: maybe it is a welcome card the
 * (affiliate) campsite may adopt as its house card.
 */
public class CheckWelcomeCardAdoptionEvent extends Event {

    public CheckWelcomeCardAdoptionEvent(String pedestalCode, String rfid) {
        super(EventType.CHECK_WELCOME_CARD_ADOPTION);
        setAttribute(EventAttribute.PEDESTAL_CODE, pedestalCode);
        setAttribute(EventAttribute.RFID, rfid);
    }

    public static String getPedestalCode(Event event) {
        return EventAttribute.PEDESTAL_CODE.getStringValue(event);
    }

    public static String getRfid(Event event) {
        return EventAttribute.RFID.getStringValue(event);
    }
}
