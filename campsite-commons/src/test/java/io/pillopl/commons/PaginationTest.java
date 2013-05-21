package io.pillopl.commons;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaginationTest {

    @Test
    void missingPageOrSizeMeansTheWholeList() {
        assertThat(Pagination.ofNullable(null, 10)).isNull();
        assertThat(Pagination.ofNullable(2, null)).isNull();
        assertThat(Pagination.orFirstPage(null, null)).isEqualTo(Pagination.FIRST_PAGE);
    }

    @Test
    void offsetIsPageTimesRows() {
        assertThat(new Pagination(3, 20).offset()).isEqualTo(60);
    }

    @Test
    void negativePagesAndTooLargePagesAreRejected() {
        assertThatThrownBy(() -> new Pagination(-1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Pagination(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Pagination(0, Pagination.MAX_ROWS_PER_PAGE + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listPageCountsPagesAndKnowsWhetherMoreFollow() {
        ListPage<String> page = ListPage.of(List.of("a", "b"), 5, new Pagination(1, 2));

        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.hasNext()).isTrue();
        assertThat(ListPage.of(List.of("e"), 5, new Pagination(2, 2)).hasNext()).isFalse();
        assertThat(ListPage.of(List.of("a"), 1, null).totalPages()).isEqualTo(1);
        assertThat(ListPage.empty().totalPages()).isZero();
    }
}
