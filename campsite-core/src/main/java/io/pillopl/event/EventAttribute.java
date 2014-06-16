package io.pillopl.event;

import io.pillopl.infrastructure.SocketStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Keys of the event attributes, each with the class its value must have.
 */
public enum EventAttribute {
    ID(Long.class),
    EXTERNAL_USER_ID(Long.class),
    PEDESTAL_ID(Long.class),
    SOCKET_ID(Long.class),
    PEDESTAL_CODE(String.class),
    SOCKET_NO(Integer.class),
    RFID(String.class),
    TRANSACTION_ID(Integer.class),
    SUPPLY_SESSION_ID(Long.class),
    FIRMWARE_VERSION(String.class),
    MODEM_ICCID(String.class),
    METER_VALUE(BigDecimal.class),
    METER_SERIAL_NUMBER(String.class),
    PEDESTAL_SERIAL_NUMBER(String.class),
    PEDESTAL_VENDOR(String.class),
    PEDESTAL_MODEL(String.class),
    PEDESTAL_ERROR_CODE(String.class),
    PEDESTAL_VENDOR_ERROR_CODE(String.class),
    PEDESTAL_INFO(String.class),
    CAMPSITE_NAME(String.class),
    CAMPSITE_ADDRESS(String.class),
    GROUP_ID(Long.class),
    SOCKET_STATUS(SocketStatus.class),
    TIME(LocalDateTime.class),
    STATUS(String.class),
    HEARTBEAT_INTERVAL(Integer.class),
    START_TIME(LocalDateTime.class),
    STOP_TIME(LocalDateTime.class),
    START_METER_KWH(BigDecimal.class),
    STOP_METER_KWH(BigDecimal.class),
    AUTHORIZATION_RESULT(String.class),
    URL(String.class),
    ISSUER_PARTNER_ID(Long.class),
    USERNAME(String.class),
    ACCESS_MODE(String.class),
    CHANGE_TYPE(String.class),
    TARIFF_ID(Long.class),
    STOP_REASON(String.class),
    BILLABLE(Boolean.class);

    private final Class<?> clazz;

    EventAttribute(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Class<?> getAttributeClass() {
        return clazz;
    }

    public Integer getIntegerValue(Event event) {
        Object value = event.getAttribute(this);
        if (value == null) {
            return null;
        }
        if (Integer.class.equals(clazz) && value instanceof Number number) {
            return number.intValue();
        }
        throw new ClassCastException(name() + " is not of type Integer but of class " + clazz.getName());
    }

    public Long getLongValue(Event event) {
        Object value = event.getAttribute(this);
        if (value == null) {
            return null;
        }
        if (Long.class.equals(clazz) && value instanceof Number number) {
            return number.longValue();
        }
        throw new ClassCastException(name() + " is not of type Long but of class " + clazz.getName());
    }

    public String getStringValue(Event event) {
        Object value = event.getAttribute(this);
        return value != null ? value.toString() : null;
    }
}
