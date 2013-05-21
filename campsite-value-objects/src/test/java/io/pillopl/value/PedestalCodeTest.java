package io.pillopl.value;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PedestalCodeTest {

    @Test
    void codeIsTrimmed() {
        assertThat(PedestalCode.of("  HR-BAY-P001 ").value()).isEqualTo("HR-BAY-P001");
    }

    @Test
    void blankTooLongAndSocketSeparatorAreRejected() {
        assertThatThrownBy(() -> PedestalCode.of(" ")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> PedestalCode.of("P".repeat(129))).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> PedestalCode.of("HRBAY*P012")).isInstanceOf(InvalidValueException.class);
    }
}
