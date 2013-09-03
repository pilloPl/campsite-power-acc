package io.pillopl.value;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SocketLinkIdTest {

    @Test
    void socketIsAddressedByPedestalCodeAndSocketNumber() {
        SocketLinkId id = SocketLinkId.of(" HR-BAY-P001", 2);

        assertThat(id.getPedestalCode()).isEqualTo("HR-BAY-P001");
        assertThat(id.socketNo()).isEqualTo(2);
        assertThat(id).hasToString("HR-BAY-P001/2");
    }

    @Test
    void socketNumberIsBetweenOneAndSix() {
        assertThatThrownBy(() -> SocketLinkId.of("HR-BAY-P001", 0)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> SocketLinkId.of("HR-BAY-P001", 7)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> SocketLinkId.of("HR-BAY-P001", null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> SocketLinkId.of(" ", 1)).isInstanceOf(InvalidValueException.class);
    }
}
