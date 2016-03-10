package io.pillopl.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * An event whose send failed, kept to be sent again by the retry job. The attributes are stored as text and read back
 * with the class of their {@link EventAttribute}.
 */
@Entity
@Table(name = "EVENT_RETRY")
@Getter
@Setter
public class EventRetry {

    public enum Status {
        OPEN,
        SENT,
        FAILED
    }

    static final int MAX_FAILURE_LENGTH = 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_event_retry")
    @SequenceGenerator(name = "seq_event_retry", sequenceName = "EVENT_RETRY_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "EVENT_ID", length = 64, nullable = false)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "EVENT_TYPE", length = 64, nullable = false)
    private EventType eventType;

    @Column(name = "ATTRIBUTES", nullable = false, columnDefinition = "TEXT")
    private String attributes;

    @Column(name = "FAILURE", length = MAX_FAILURE_LENGTH)
    private String failure;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 16, nullable = false)
    private Status status = Status.OPEN;

    @Column(name = "RETRY_COUNT", nullable = false)
    private int retryCount;

    @Column(name = "CREATE_DATE", nullable = false)
    private LocalDateTime createDate;

    @Column(name = "LAST_RETRY_DATE")
    private LocalDateTime lastRetryDate;

    public EventRetry() {
    }

    public static EventRetry of(Event event, String failure, LocalDateTime createDate) {
        EventRetry retry = new EventRetry();
        retry.eventId = event.getId();
        retry.eventType = event.getType();
        retry.attributes = writeAttributes(event.getAttributes());
        retry.failure = abbreviate(failure);
        retry.createDate = createDate;
        return retry;
    }

    /**
     * The event to send again: same type and attributes, a new event id.
     */
    public Event toEvent() {
        Event event = new Event(eventType);
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(attributes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (String name : properties.stringPropertyNames()) {
            EventAttribute attribute = EventAttribute.valueOf(name);
            event.setAttribute(attribute, readValue(attribute, properties.getProperty(name)));
        }
        return event;
    }

    public void sent(LocalDateTime at) {
        retryCount++;
        lastRetryDate = at;
        status = Status.SENT;
    }

    /**
     * Another failed attempt; after the last allowed one the event is given up.
     */
    public void failedAgain(String reason, LocalDateTime at, int maxAttempts) {
        retryCount++;
        lastRetryDate = at;
        failure = abbreviate(reason);
        if (retryCount >= maxAttempts) {
            status = Status.FAILED;
        }
    }

    static String writeAttributes(Map<EventAttribute, Object> values) {
        // sorted for a stable text; null values are not stored (an unset attribute reads back as null)
        Map<String, String> sorted = new TreeMap<>();
        values.forEach((key, value) -> {
            if (value != null) {
                sorted.put(key.name(), value instanceof Enum<?> e ? e.name() : value.toString());
            }
        });
        Properties properties = new Properties();
        properties.putAll(sorted);
        StringWriter writer = new StringWriter();
        try {
            properties.store(writer, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // drop the date comment written by Properties.store
        StringBuilder text = new StringBuilder();
        writer.toString().lines().filter(line -> !line.startsWith("#")).sorted()
                .forEach(line -> text.append(line).append('\n'));
        return text.toString();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static Object readValue(EventAttribute attribute, String text) {
        Class<?> clazz = attribute.getAttributeClass();
        if (String.class.equals(clazz)) {
            return text;
        }
        if (Long.class.equals(clazz)) {
            return Long.valueOf(text);
        }
        if (Integer.class.equals(clazz)) {
            return Integer.valueOf(text);
        }
        if (Boolean.class.equals(clazz)) {
            return Boolean.valueOf(text);
        }
        if (BigDecimal.class.equals(clazz)) {
            return new BigDecimal(text);
        }
        if (LocalDateTime.class.equals(clazz)) {
            return LocalDateTime.parse(text);
        }
        if (clazz.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) clazz, text);
        }
        throw new IllegalArgumentException("Attribute " + attribute + " of class " + clazz.getName()
                + " can not be read back");
    }

    private static String abbreviate(String text) {
        if (text == null || text.length() <= MAX_FAILURE_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_FAILURE_LENGTH);
    }

    @Override
    public String toString() {
        return "EventRetry{id=" + id + ", eventType=" + eventType + ", status=" + status + ", retryCount="
                + retryCount + "}";
    }
}
