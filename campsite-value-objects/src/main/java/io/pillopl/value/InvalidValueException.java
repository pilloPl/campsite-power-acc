package io.pillopl.value;

/**
 * A value given from outside (request, import file) does not fit its value object.
 */
public class InvalidValueException extends IllegalArgumentException {

    private final String valueName;

    public InvalidValueException(String valueName, String message) {
        super(valueName + ": " + message);
        this.valueName = valueName;
    }

    public String getValueName() {
        return valueName;
    }
}
