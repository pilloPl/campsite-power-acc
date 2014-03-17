package io.pillopl.commons;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionUtilTest {

    @Test
    void rootCauseMessageIsTheMessageOfTheInnermostCause() {
        Exception e = new RuntimeException("outer", new IllegalStateException("pedestal 0042 unreachable"));

        assertThat(ExceptionUtil.getRootCauseMessage(e)).isEqualTo("pedestal 0042 unreachable");
        assertThat(ExceptionUtil.getRootCauseMessage(null)).isNull();
    }

    @Test
    void causeChainThatLoopsBackDoesNotHang() {
        RuntimeException outer = new RuntimeException("outer");
        IllegalStateException inner = new IllegalStateException("inner", outer);
        outer.initCause(inner);

        assertThat(ExceptionUtil.getRootCauseMessage(outer)).isEqualTo("inner");
    }

    @Test
    void missingThrowableGivesAnEmptyStackTrace() {
        assertThat(ExceptionUtil.getStackTrace(null)).isEmpty();
    }
}
