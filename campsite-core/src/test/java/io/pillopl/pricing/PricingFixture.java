package io.pillopl.pricing;

import io.pillopl.group.Market;
import io.pillopl.persistence.dao.TariffDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * Builds and saves tariffs for integration tests.
 */
@Component
public class PricingFixture {

    @Autowired
    TariffDao tariffDao;

    /**
     * One element: energy per kWh (1 Wh steps) and nothing else.
     */
    public Tariff energyTariff(String name, Market market, String pricePerKwh) {
        Tariff tariff = new Tariff(name, name + " per kWh", market);
        tariff.addElement(new TariffElement().addComponent(PriceComponent.energy(pricePerKwh, 1)));
        return tariffDao.saveAndFlush(tariff);
    }

    /**
     * Night energy 22:00-06:00 at the night price, otherwise the day price, plus a connection fee.
     */
    public Tariff dayNightTariff(String name, Market market, String dayPrice, String nightPrice, String flat) {
        Tariff tariff = new Tariff(name, name + " day and night", market);
        tariff.addElement(new TariffElement().addComponent(PriceComponent.energy(nightPrice, 1))
                .restrictTo(TariffRestriction.hours(LocalTime.of(22, 0), LocalTime.of(6, 0))));
        tariff.addElement(new TariffElement().addComponent(PriceComponent.energy(dayPrice, 1))
                .addComponent(PriceComponent.flat(flat)));
        return tariffDao.saveAndFlush(tariff);
    }

    public Tariff save(Tariff tariff) {
        return tariffDao.saveAndFlush(tariff);
    }
}
