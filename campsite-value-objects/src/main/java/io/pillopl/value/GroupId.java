package io.pillopl.value;

/**
 * Database id of a group.
 */
public record GroupId(long value) {

    public GroupId {
        if (value < 1) {
            throw new InvalidValueException("GroupId", "must be positive but is " + value);
        }
    }

    public static GroupId of(Long value) {
        if (value == null) {
            throw new InvalidValueException("GroupId", "is missing");
        }
        return new GroupId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
