package io.pillopl.value;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PitchNoTest {

    @Test
    void pitchNumberIsStoredUpperCase() {
        assertThat(PitchNo.ofNullable(" a12 ").value()).isEqualTo("A12");
    }

    @Test
    void blankPitchMeansNoPitch() {
        assertThat(PitchNo.ofNullable("  ")).isNull();
        assertThat(PitchNo.ofNullable(null)).isNull();
    }

    @Test
    void pitchNumberFitsTheColumn() {
        assertThatThrownBy(() -> PitchNo.ofNullable("A".repeat(17))).isInstanceOf(InvalidValueException.class);
    }
}
