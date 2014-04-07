package io.pillopl.pricing;

import io.pillopl.group.AccessDeniedException;
import io.pillopl.group.Group;
import io.pillopl.group.GroupService;
import io.pillopl.group.Market;
import io.pillopl.group.attribute.DefaultTariffService;
import io.pillopl.persistence.dao.TariffDao;
import io.pillopl.persistence.dao.UserinfoDao;
import io.pillopl.test.IntegrationTest;
import io.pillopl.user.Userinfo;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class TariffServiceIntegrationTest extends IntegrationTest {

    @Autowired TariffService tariffService;
    @Autowired TariffDao tariffDao;
    @Autowired UserinfoDao userinfoDao;
    @Autowired GroupService groupService;
    @Autowired DefaultTariffService defaultTariffService;
    @Autowired PricingFixture pricing;
    @Autowired EntityManager entityManager;

    Userinfo hrManager;

    @BeforeEach
    void setUp() {
        hrManager = new Userinfo("hr.manager", "HR manager", "hr.manager@example.org");
        hrManager.setGroup(groupService.fetchStaffGroup("HR STAFF"));
        hrManager = userinfoDao.saveAndFlush(hrManager);
    }

    static Tariff energyTariff(String name) {
        Tariff tariff = new Tariff(name, null, null);
        tariff.addElement(new TariffElement().addComponent(PriceComponent.energy("0.40", 1)));
        return tariff;
    }

    @Test
    void newTariffLandsInTheMarketOfTheUser() {
        Tariff saved = tariffService.saveTariff(hrManager, energyTariff("BAY"));

        assertThat(saved.getMarket()).isEqualTo(Market.HR);
        assertThat(saved.getDescription()).isEqualTo("BAY");
        assertThat(tariffService.getAllTariffs(hrManager)).extracting(Tariff::getName).contains("BAY", "HR STANDARD")
                .doesNotContain("IT STANDARD");
    }

    @Test
    void tariffNameMayExistOnlyOncePerMarket() {
        tariffService.saveTariff(hrManager, energyTariff("BAY"));

        assertThatThrownBy(() -> tariffService.saveTariff(hrManager, energyTariff("BAY")))
                .isInstanceOf(TariffOperationException.class).hasMessageContaining("already exists");
    }

    @Test
    void secondDefaultTariffOfTheMarketIsRefused() {
        Tariff second = energyTariff("SECOND DEFAULT");
        second.setDefaultTariff(true);

        assertThatThrownBy(() -> tariffService.saveTariff(hrManager, second))
                .isInstanceOf(TariffOperationException.class).hasMessageContaining("Default tariff for HR");
    }

    @Test
    void userCannotCreateTariffsOfAnotherMarket() {
        Tariff italian = energyTariff("LAGO");
        italian.setMarket(Market.IT);

        assertThatThrownBy(() -> tariffService.saveTariff(hrManager, italian))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> tariffService.findTariffForUser(hrManager, 2L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void userWithoutMarketCannotWorkWithTariffs() {
        Userinfo nowhere = userinfoDao.saveAndFlush(new Userinfo("nowhere", "Nowhere", "nowhere@example.org"));

        assertThatThrownBy(() -> tariffService.getAllTariffs(nowhere)).isInstanceOf(TariffOperationException.class);
    }

    @Test
    void unusedTariffIsChangedAndUsedTariffIsNot() {
        Tariff saved = tariffService.saveTariff(hrManager, energyTariff("BAY"));
        Tariff changes = new Tariff("ignored", "Bay with fee", null);
        changes.addElement(new TariffElement().addComponent(PriceComponent.energy("0.45", 1))
                .addComponent(PriceComponent.flat("1.00")));

        tariffService.updateTariff(hrManager, saved.getId(), changes);
        entityManager.flush();
        entityManager.clear();
        Tariff reloaded = tariffDao.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("BAY");
        assertThat(reloaded.getTariffElements()).hasSize(1);
        assertThat(reloaded.getTariffElements().get(0).getPriceComponents()).hasSize(2);

        Group bay = groupService.fetchSiteGroup("HR SITES");
        defaultTariffService.setDefaultTariff(bay, reloaded);
        assertThatThrownBy(() -> tariffService.updateTariff(hrManager, saved.getId(), energyTariff("x")))
                .isInstanceOf(TariffOperationException.class).hasMessageContaining("in use");
    }

    @Test
    void marketDefaultMovesToAnotherTariff() {
        Tariff bay = tariffService.saveTariff(hrManager, energyTariff("BAY"));

        tariffService.setMarketDefaultTariff(hrManager, bay.getId());

        assertThat(tariffService.getDefaultTariff(Market.HR)).isEqualTo(bay);
        assertThat(tariffDao.findById(1L).orElseThrow().isDefaultTariff()).isFalse();
    }

    @Test
    void invalidTariffContentIsRefusedBeforeSaving() {
        Tariff twoFees = new Tariff("TWO FEES", "two fees", null);
        twoFees.addElement(new TariffElement().addComponent(PriceComponent.flat("1.00"))
                .addComponent(PriceComponent.flat("2.00")));

        assertThatThrownBy(() -> tariffService.saveTariff(hrManager, twoFees))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("two FLAT");
    }

    @Test
    void contentRulesOfATariff() {
        assertThatThrownBy(() -> TariffService.validateTariff(new Tariff("EMPTY", "empty", Market.HR)))
                .hasMessageContaining("no elements");
        Tariff noComponents = new Tariff("NO COMPONENTS", "x", Market.HR);
        noComponents.addElement(new TariffElement());
        assertThatThrownBy(() -> TariffService.validateTariff(noComponents)).hasMessageContaining("no price");
        Tariff negative = energyTariff("NEGATIVE");
        negative.getTariffElements().get(0).getPriceComponents().get(0).setPrice(new BigDecimal("-1"));
        assertThatThrownBy(() -> TariffService.validateTariff(negative)).hasMessageContaining("negative");
        Tariff zeroStep = energyTariff("ZERO STEP");
        zeroStep.getTariffElements().get(0).getPriceComponents().get(0).setStepSize(0);
        assertThatThrownBy(() -> TariffService.validateTariff(zeroStep)).hasMessageContaining("positive");
        Tariff flatStep = new Tariff("FLAT STEP", "x", Market.HR);
        flatStep.addElement(new TariffElement().addComponent(new PriceComponent(TariffDimensionType.FLAT,
                BigDecimal.ONE, 5)));
        assertThatThrownBy(() -> TariffService.validateTariff(flatStep)).hasMessageContaining("step size 1");
        Tariff currency = energyTariff("CURRENCY");
        currency.setCurrency("euro");
        assertThatThrownBy(() -> TariffService.validateTariff(currency)).hasMessageContaining("ISO");
        assertThatThrownBy(() -> TariffService.validateTariff(energyTariff(" "))).hasMessageContaining("name");
    }
}
