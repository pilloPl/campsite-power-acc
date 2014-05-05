package io.pillopl.commons;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextUtilTest {

    @Test
    void blankTextBecomesNull() {
        assertThat(TextUtil.trimToNull("  ")).isNull();
        assertThat(TextUtil.trimToNull(" a ")).isEqualTo("a");
        assertThat(TextUtil.isBlank(null)).isTrue();
    }

    @Test
    void longTextIsCutWithAnEllipsis() {
        assertThat(TextUtil.abbreviate("abcdefgh", 6)).isEqualTo("abc...");
        assertThat(TextUtil.abbreviate("abc", 6)).isEqualTo("abc");
        assertThat(TextUtil.abbreviate("abcdef", 2)).isEqualTo("ab");
    }

    @Test
    void negativeLengthIsRejected() {
        assertThatThrownBy(() -> TextUtil.abbreviate("abc", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("-1");
    }

    @Test
    void rootCauseMessageComesFromTheInnermostException() {
        Exception e = new RuntimeException("outer", new IllegalStateException(new NullPointerException()));

        assertThat(ExceptionUtil.getRootCauseMessage(e)).isEqualTo("NullPointerException");
        assertThat(ExceptionUtil.getStackTrace(e)).contains("outer");
    }
}
