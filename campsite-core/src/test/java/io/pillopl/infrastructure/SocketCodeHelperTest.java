package io.pillopl.infrastructure;

import io.pillopl.value.SocketCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SocketCodeHelperTest {

    @Test
    void socketCodeIsCampsitePedestalAndSocketNumber() {
        assertThat(SocketCodeHelper.createSocketCode("hrbay", "P012", 2)).isEqualTo("HRBAY*P012*2");
    }

    @Test
    void createdSocketCodeCanBeReadBackAsSocketCodeValue() {
        SocketCode code = SocketCode.parse(SocketCodeHelper.createSocketCode("hrbay", "HRBAY-P012", 3));

        assertThat(code.pedestalPart()).isEqualTo("P012");
        assertThat(code.socketNo()).isEqualTo(3);
    }

    @Test
    void campsiteIsNotRepeatedWhenThePedestalCodeStartsWithIt() {
        assertThat(SocketCodeHelper.createSocketCode("HRBAY", "HRBAY-P012", 1)).isEqualTo("HRBAY*P012*1");
    }

    @Test
    void socketCodeNeedsACampsiteCode() {
        assertThatThrownBy(() -> SocketCodeHelper.createSocketCode(" ", "P012", 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void socketCodeNeedsAPedestalCode() {
        assertThatThrownBy(() -> SocketCodeHelper.createSocketCode("HRBAY", null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pedestal code");
        assertThatThrownBy(() -> SocketCodeHelper.createSocketCode("HRBAY", "  ", 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allSocketsOfAPedestalGetTheirCodeAndLoseItAgain() {
        Location location = new Location("Rovinj", "52210", "Obala", "12");
        location.setCampsiteCode("HRBAY");
        Pedestal pedestal = new Pedestal("P7", null, location);
        pedestal.addSocket(new Socket("a", null, 1));
        pedestal.addSocket(new Socket("b", null, 2));

        SocketCodeHelper.setSocketCodes(pedestal, location);
        assertThat(pedestal.getSockets()).extracting(Socket::getSocketCode).containsExactly("HRBAY*P7*1", "HRBAY*P7*2");

        SocketCodeHelper.setSocketCodesToNull(pedestal.getSockets());
        assertThat(pedestal.getSockets()).extracting(Socket::getSocketCode).containsOnlyNulls();
    }
}
