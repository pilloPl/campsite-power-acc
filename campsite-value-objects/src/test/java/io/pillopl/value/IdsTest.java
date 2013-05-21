package io.pillopl.value;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdsTest {

    @Test
    void idsArePositive() {
        assertThat(PedestalId.of(7L).value()).isEqualTo(7);
        assertThatThrownBy(() -> GroupId.of(0L)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> SocketId.of(-3L)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void missingIdIsNamedInTheMessage() {
        assertThatThrownBy(() -> GroupId.of(null))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("GroupId")
                .extracting(e -> ((InvalidValueException) e).getValueName()).isEqualTo("GroupId");
    }
}
