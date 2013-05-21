package io.pillopl.value;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SocketCodeTest {

    @Test
    void socketCodeIsReadIntoCampsitePedestalAndSocketNumber() {
        SocketCode code = SocketCode.parse(" hrbay * P012 * 2 ");

        assertThat(code.campsiteCode()).isEqualTo("HRBAY");
        assertThat(code.pedestalPart()).isEqualTo("P012");
        assertThat(code.socketNo()).isEqualTo(2);
        assertThat(code.value()).isEqualTo("HRBAY*P012*2");
    }

    @Test
    void codesWithoutThreePartsOrWithoutSocketNumberAreRejected() {
        assertThatThrownBy(() -> SocketCode.parse("HRBAY*P012")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> SocketCode.parse("HRBAY*P012*two")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> SocketCode.parse("HRBAY*P012*0")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> SocketCode.parse("*P012*1")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> SocketCode.parse(null)).isInstanceOf(InvalidValueException.class);
    }
}
