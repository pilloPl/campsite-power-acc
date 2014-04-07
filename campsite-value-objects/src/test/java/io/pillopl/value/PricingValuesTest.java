package io.pillopl.value;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingValuesTest {

    @Test
    void tariffIdIsPositiveOrAbsent() {
        assertThat(TariffId.of(3L).value()).isEqualTo(3L);
        assertThat(TariffId.ofNullable(null)).isNull();
        assertThatThrownBy(() -> TariffId.of(0L)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> TariffId.of(null)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void energyIsNotNegativeAndFitsTheMeterColumn() {
        assertThat(Kwh.of(" 12.3400 ").value()).isEqualByComparingTo("12.34");
        assertThat(Kwh.of(new BigDecimal("12.3400")).toString()).isEqualTo("12.34 kWh");
        assertThatThrownBy(() -> Kwh.of("-1")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> Kwh.of("1.23456")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> Kwh.of("ten")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> Kwh.of((String) null)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void moneyIsKeptInCentsRoundedHalfUp() {
        Money money = Money.of(new BigDecimal("5.665"), "eur");

        assertThat(money.amount()).isEqualByComparingTo("5.67");
        assertThat(money.toString()).isEqualTo("5.67 EUR");
        assertThat(Money.ofNullable(null, "EUR")).isNull();
        assertThatThrownBy(() -> Money.of(BigDecimal.ONE, "euro")).isInstanceOf(InvalidValueException.class);
    }
}
