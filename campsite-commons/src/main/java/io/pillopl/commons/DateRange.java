package io.pillopl.commons;

import java.time.LocalDateTime;

/**
 * A period with optional ends: from inclusive, to inclusive. A missing end is open.
 */
public record DateRange(LocalDateTime from, LocalDateTime to) {

    public static final DateRange UNBOUNDED = new DateRange(null, null);

    public DateRange {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Start " + from + " is after end " + to);
        }
    }

    /**
     * Range of whole days given as text ("2016-05-01" or "2016-05-01T10:00"): a date-only end is extended to the end
     * of that day.
     */
    public static DateRange ofDays(String from, String to) {
        return new DateRange(DateUtils.parseStartOfDay(from), DateUtils.parseEndOfDay(to));
    }

    public boolean contains(LocalDateTime time) {
        if (time == null) {
            return false;
        }
        return (from == null || !time.isBefore(from)) && (to == null || !time.isAfter(to));
    }

    public boolean isUnbounded() {
        return from == null && to == null;
    }
}
