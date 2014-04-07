package io.pillopl.pricing;

import io.pillopl.api.exception.InvalidInputException;
import io.pillopl.dto.pricing.PriceComponentDTO;
import io.pillopl.dto.pricing.TariffDTO;
import io.pillopl.dto.pricing.TariffElementDTO;
import io.pillopl.dto.pricing.TariffRestrictionDTO;
import io.pillopl.group.Market;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TariffMapperTest {

    static TariffDTO weekendNights() {
        TariffRestrictionDTO restriction = new TariffRestrictionDTO(LocalTime.of(22, 0), LocalTime.of(6, 0), null,
                null, List.of("saturday", "SUNDAY"), null, null);
        return new TariffDTO(null, " WEEKEND ", "weekend nights", null, null, false, null, null, List.of(
                new TariffElementDTO(null, 0, List.of(new PriceComponentDTO(null, "energy", new BigDecimal("0.30"),
                        null, null)), restriction),
                new TariffElementDTO(null, 1, List.of(new PriceComponentDTO(null, "PER NIGHT", new BigDecimal("4"),
                        1, null)), null)));
    }

    @Test
    void tariffFromTheConsoleBecomesANewEntityInElementOrder() {
        Tariff tariff = TariffMapper.toEntity(weekendNights());

        assertThat(tariff.getId()).isNull();
        assertThat(tariff.getName()).isEqualTo("WEEKEND");
        assertThat(tariff.getMarket()).isNull();
        assertThat(tariff.getCurrency()).isEqualTo("EUR");
        TariffElement first = tariff.getTariffElements().get(0);
        assertThat(first.findComponent(TariffDimensionType.ENERGY).getStepSize()).isEqualTo(1);
        assertThat(first.getTariffRestriction().getDaysOfWeek())
                .containsExactlyInAnyOrder(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        assertThat(tariff.getTariffElements().get(1).findComponent(TariffDimensionType.PER_NIGHT)).isNotNull();
    }

    @Test
    void tariffIsSentWithItsPriceText() {
        Tariff tariff = TariffMapper.toEntity(weekendNights());
        tariff.setMarket(Market.HR);

        TariffDTO dto = TariffMapper.toDTO(tariff);

        assertThat(dto.market()).isEqualTo("HR");
        assertThat(dto.priceText()).isEqualTo("sat,sun 22:00-06:00: 0.30 EUR/kWh; 4.00 EUR/night");
        assertThat(dto.elements().get(0).restriction().daysOfWeek()).containsExactly("SATURDAY", "SUNDAY");
        assertThat(dto.elements().get(1).priceComponents().get(0).priceUnit()).isEqualTo("PER_NIGHT");
    }

    @Test
    void unknownDimensionIsAWrongRequest() {
        TariffDTO dto = new TariffDTO(null, "X", "x", null, null, false, null, null, List.of(new TariffElementDTO(
                null, 0, List.of(new PriceComponentDTO(null, "PARKING", BigDecimal.ONE, 1, null)), null)));

        assertThatThrownBy(() -> TariffMapper.toEntity(dto)).isInstanceOf(InvalidInputException.class);
    }
}
