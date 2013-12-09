package io.pillopl.value;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardValuesTest {

    @Test
    void cardUidIsTheTrimmedDecimalUidOfTheReader() {
        assertThat(CardRfid.of(" 3283263748 ").value()).isEqualTo("3283263748");
        assertThatThrownBy(() -> CardRfid.of("04A1B2C3")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> CardRfid.of(" ")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> CardRfid.of("1".repeat(65))).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void cardIdsArePositive() {
        assertThat(CardId.of(5L).value()).isEqualTo(5);
        assertThatThrownBy(() -> CardId.of(0L)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> CardId.of(null)).isInstanceOf(InvalidValueException.class);
        assertThat(HouseCardId.ofNullable(null)).isNull();
        assertThat(HouseCardId.ofNullable(3L)).hasToString("3");
        assertThatThrownBy(() -> HouseCardId.of(-1L)).isInstanceOf(InvalidValueException.class);
    }
}
