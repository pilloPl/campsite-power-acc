package io.pillopl.commons;

import java.util.Collections;
import java.util.List;

/**
 * One page of a list with the total number of rows. Without paging (page and rows per page null) the page holds
 * the whole list.
 */
public record ListPage<T>(List<T> items, long total, Integer page, Integer rowsPerPage) {

    public ListPage {
        items = items != null ? List.copyOf(items) : Collections.emptyList();
    }

    public static <T> ListPage<T> of(List<T> items, long total, Pagination pagination) {
        if (pagination == null) {
            return new ListPage<>(items, total, null, null);
        }
        return new ListPage<>(items, total, pagination.page(), pagination.rowsPerPage());
    }

    public static <T> ListPage<T> empty() {
        return new ListPage<>(Collections.emptyList(), 0, null, null);
    }

    public int totalPages() {
        if (rowsPerPage == null || rowsPerPage == 0) {
            return total > 0 ? 1 : 0;
        }
        return (int) ((total + rowsPerPage - 1) / rowsPerPage);
    }

    public boolean hasNext() {
        return page != null && page + 1 < totalPages();
    }
}
