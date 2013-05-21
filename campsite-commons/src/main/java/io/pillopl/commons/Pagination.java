package io.pillopl.commons;

/**
 * A page of a list as asked by the console: page number from 0 and rows per page.
 */
public record Pagination(int page, int rowsPerPage) {

    public static final int DEFAULT_ROWS_PER_PAGE = 50;

    public static final int MAX_ROWS_PER_PAGE = 500;

    public static final Pagination FIRST_PAGE = new Pagination(0, DEFAULT_ROWS_PER_PAGE);

    public Pagination {
        if (page < 0) {
            throw new IllegalArgumentException("Page must not be negative: " + page);
        }
        if (rowsPerPage < 1 || rowsPerPage > MAX_ROWS_PER_PAGE) {
            throw new IllegalArgumentException("Rows per page must be between 1 and " + MAX_ROWS_PER_PAGE + ": "
                    + rowsPerPage);
        }
    }

    /**
     * @return null when the page or the rows per page are missing (the caller then lists everything)
     */
    public static Pagination ofNullable(Integer page, Integer rowsPerPage) {
        if (page == null || rowsPerPage == null) {
            return null;
        }
        return new Pagination(page, rowsPerPage);
    }

    /**
     * @return the first page with the default size when the page or the rows per page are missing
     */
    public static Pagination orFirstPage(Integer page, Integer rowsPerPage) {
        Pagination pagination = ofNullable(page, rowsPerPage);
        return pagination != null ? pagination : FIRST_PAGE;
    }

    public long offset() {
        return (long) page * rowsPerPage;
    }
}
