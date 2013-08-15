package io.pillopl.pricing;

import io.pillopl.group.Market;
import io.pillopl.persistence.dao.TariffDao;
import io.pillopl.test.IntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class TariffIntegrationTest extends IntegrationTest {

    @Autowired TariffDao tariffDao;
    @Autowired PricingFixture pricing;
    @Autowired EntityManager entityManager;

    @Test
    void tariffIsStoredWithItsElementsComponentsAndRestrictionsInOrder() {
        Tariff tariff = new Tariff("SUMMER", "High season and weekend", Market.IT);
        TariffRestriction season = TariffRestriction.season(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 31));
        season.getDaysOfWeek().add(DayOfWeek.SATURDAY);
        season.getDaysOfWeek().add(DayOfWeek.SUNDAY);
        tariff.addElement(new TariffElement().addComponent(PriceComponent.energy("0.6500", 100)).restrictTo(season));
        tariff.addElement(new TariffElement().addComponent(PriceComponent.energy("0.4500", 100))
                .addComponent(PriceComponent.time("0.10", 900)));
        Long id = pricing.save(tariff).getId();
        entityManager.clear();

        Tariff loaded = tariffDao.findById(id).orElseThrow();

        assertThat(loaded.getTariffElements()).hasSize(2);
        TariffElement first = loaded.getTariffElements().get(0);
        assertThat(first.getPosition()).isZero();
        assertThat(first.getTariffRestriction().getDaysOfWeek())
                .containsExactlyInAnyOrder(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        assertThat(first.findComponent(TariffDimensionType.ENERGY).getPrice()).isEqualByComparingTo("0.65");
        assertThat(loaded.getTariffElements().get(1).getPriceComponents()).hasSize(2);
        assertThat(loaded.getDimensionTypes())
                .containsExactlyInAnyOrder(TariffDimensionType.ENERGY, TariffDimensionType.TIME);
    }

    @Test
    void marketHasOneDefaultTariffSeededAndCannotGetASecondOne() {
        assertThat(tariffDao.findDefaultTariffForMarket(Market.HR)).isNotNull();
        Tariff second = new Tariff("ANOTHER DEFAULT", "second default", Market.HR);
        second.setDefaultTariff(true);

        assertThatThrownBy(() -> tariffDao.saveAndFlush(second)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void tariffNameIsUniqueWithinTheMarketOnly() {
        pricing.energyTariff("BAY", Market.HR, "0.40");
        pricing.energyTariff("BAY", Market.FR, "0.40");

        assertThat(tariffDao.findForNameAndMarket(Market.FR, "BAY")).isNotNull();
        assertThatThrownBy(() -> pricing.energyTariff("BAY", Market.HR, "0.50"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void negativePriceIsRejectedByTheDatabase() {
        Tariff tariff = new Tariff("NEGATIVE", "negative", Market.HR);
        tariff.addElement(new TariffElement().addComponent(
                new PriceComponent(TariffDimensionType.ENERGY, new BigDecimal("-0.10"), 1)));

        assertThatThrownBy(() -> tariffDao.saveAndFlush(tariff)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
