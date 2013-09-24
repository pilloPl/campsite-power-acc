package io.pillopl.event;

/**
 * A card was authorized (or refused) at a socket or a pedestal; carries the answer given to the pedestal.
 */
public class AuthorizationEvent extends Event {

    public AuthorizationEvent(Long socketId, Long pedestalId, String rfid, String authorizationResult) {
        super(EventType.AUTHORIZATION);
        if (socketId != null) {
            setAttribute(EventAttribute.SOCKET_ID, socketId);
        }
        if (pedestalId != null) {
            setAttribute(EventAttribute.PEDESTAL_ID, pedestalId);
        }
        setAttribute(EventAttribute.RFID, rfid);
        setAttribute(EventAttribute.AUTHORIZATION_RESULT, authorizationResult);
    }

    public static String getAuthorizationResult(Event event) {
        return EventAttribute.AUTHORIZATION_RESULT.getStringValue(event);
    }

    public static String getRfid(Event event) {
        return EventAttribute.RFID.getStringValue(event);
    }
}
