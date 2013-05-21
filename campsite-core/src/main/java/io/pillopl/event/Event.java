package io.pillopl.event;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * An event on the in-process event queues: a type, an id and typed attributes.
 */
public class Event {

    private final Map<EventAttribute, Object> attributes = new EnumMap<>(EventAttribute.class);

    private final String id;

    private final EventType type;

    public Event(EventType type) {
        this.type = type;
        this.id = UUID.randomUUID().toString();
    }

    public Object getAttribute(EventAttribute key) {
        return attributes.get(key);
    }

    public Map<EventAttribute, Object> getAttributes() {
        return attributes;
    }

    public String getId() {
        return id;
    }

    public EventType getType() {
        return type;
    }

    /**
     * @throws IllegalArgumentException when the value has not the class of the attribute
     */
    public Event setAttribute(EventAttribute key, Object value) {
        if (value == null || key.getAttributeClass().isAssignableFrom(value.getClass())) {
            attributes.put(key, value);
        } else {
            throw new IllegalArgumentException(key.name() + " must be of class "
                    + key.getAttributeClass().getSimpleName() + " but is of class " + value.getClass().getName());
        }
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{ Event-Type: ").append(type);
        sb.append(", Event-Id: ").append(id);
        for (Map.Entry<EventAttribute, Object> entry : attributes.entrySet()) {
            sb.append(", Attribute '").append(entry.getKey()).append("': ")
                    .append(entry.getValue() != null ? entry.getValue().toString() : "");
        }
        sb.append(" }");
        return sb.toString();
    }
}
