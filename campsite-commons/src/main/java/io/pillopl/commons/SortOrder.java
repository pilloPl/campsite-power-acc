package io.pillopl.commons;

/**
 * Column and direction of a sorted list. Which columns are allowed is decided by the service sorting.
 */
public record SortOrder(String column, SortDirection direction) {

    public static final SortOrder UNSORTED = new SortOrder(null, SortDirection.DESC);

    public SortOrder {
        column = TextUtil.trimToNull(column);
        direction = direction != null ? direction : SortDirection.DESC;
    }

    public static SortOrder of(String column, String direction) {
        if (TextUtil.isBlank(column) && TextUtil.isBlank(direction)) {
            return UNSORTED;
        }
        return new SortOrder(column, SortDirection.fromString(direction, SortDirection.DESC));
    }

    public boolean isSorted() {
        return column != null;
    }
}
