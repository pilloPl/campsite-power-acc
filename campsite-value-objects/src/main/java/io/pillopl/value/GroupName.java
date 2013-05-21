package io.pillopl.value;

import io.pillopl.commons.TextUtil;

/**
 * Name of a group. Names carry logic (market prefix, AFFILIATE, SUPPLIER) and are unique per group type; the
 * separator of the group path in V_GROUP (" | ") must not appear in them.
 */
public record GroupName(String value) {

    public static final int MAX_LENGTH = 255;

    public static final String PATH_SEPARATOR = "|";

    public GroupName {
        value = TextUtil.trimToNull(value);
        if (value == null) {
            throw new InvalidValueException("GroupName", "is missing");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidValueException("GroupName", "longer than " + MAX_LENGTH + " characters");
        }
        if (value.contains(PATH_SEPARATOR)) {
            throw new InvalidValueException("GroupName", "must not contain '" + PATH_SEPARATOR + "': " + value);
        }
    }

    public static GroupName of(String value) {
        return new GroupName(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
