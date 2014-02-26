package io.pillopl.value;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstallerTokenTest {

    @Test
    void tokenIsTrimmedAndStoredLowerCase() {
        InstallerToken token = InstallerToken.of(" 3F2504E0-4F89-11D3-9A0C-0305E82C3301 ");

        assertThat(token.value()).isEqualTo("3f2504e0-4f89-11d3-9a0c-0305e82c3301");
        assertThat(token).isEqualTo(InstallerToken.of("3f2504e0-4f89-11d3-9a0c-0305e82c3301"));
    }

    @Test
    void missingTokenIsRejected() {
        assertThatThrownBy(() -> InstallerToken.of(null)).isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("InstallerToken");
        assertThatThrownBy(() -> InstallerToken.of("  ")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void textThatIsNoTokenIsRejected() {
        assertThatThrownBy(() -> InstallerToken.of("pitch-A12")).isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("pitch-A12");
    }
}
