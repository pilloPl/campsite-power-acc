package io.pillopl.guest;

import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import io.pillopl.group.Market;
import io.pillopl.order.ProductDefinition;
import io.pillopl.pricing.Tariff;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeasonalContractTest {

    static final LocalDate START = LocalDate.of(2026, 4, 1);
    static final LocalDate END = LocalDate.of(2026, 10, 31);
    static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 1, 9, 0);

    final Guest guest = new Guest(new Group(GroupType.ISSUER, "NETWORK CARD"), GuestType.SEASONAL, "Horvat");
    final SeasonalContract contract = new SeasonalContract(new BillingAccount(guest, "400012345678", "Season"),
            new Tariff("HR SEASONAL", "Seasonal", Market.HR), new Group(GroupType.SITE, "BAY"), "A12", START, END);

    @Test
    void contractRunsFromTheStartToTheEndOfTheSeasonInclusive() {
        assertThat(contract.isActiveOn(START)).isTrue();
        assertThat(contract.isActiveOn(END)).isTrue();
        assertThat(contract.isActiveOn(START.minusDays(1))).isFalse();
        assertThat(contract.isActiveOn(END.plusDays(1))).isFalse();
    }

    @Test
    void terminationEndsTheContractOnTheRequestedDayButNotAfterTheSeason() {
        contract.requestTermination(LocalDate.of(2026, 8, 15), NOW);

        assertThat(contract.getStatus()).isEqualTo(ContractStatus.TERMINATION_REQUESTED);
        assertThat(contract.isActiveOn(LocalDate.of(2026, 8, 15))).isTrue();
        assertThat(contract.isActiveOn(LocalDate.of(2026, 8, 16))).isFalse();

        contract.requestTermination(END.plusMonths(2), NOW);
        assertThat(contract.getLastDay()).isEqualTo(END);
    }

    @Test
    void terminationBeforeTheStartOrOfAnEndedContractIsRefused() {
        assertThatThrownBy(() -> contract.requestTermination(START.minusDays(1), NOW))
                .isInstanceOf(IllegalArgumentException.class);

        contract.end(LocalDate.of(2026, 9, 30), NOW);
        assertThatThrownBy(() -> contract.requestTermination(END, NOW)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void endedContractNoLongerRunsButIsBilledUpToItsEnd() {
        contract.end(LocalDate.of(2026, 9, 30), NOW);

        assertThat(contract.isActiveOn(LocalDate.of(2026, 9, 15))).isFalse();
        assertThat(contract.isActiveIn(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))).isTrue();
        assertThat(contract.isActiveIn(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))).isFalse();
    }

    @Test
    void contractWithoutEndRunsOpenEnded() {
        SeasonalContract open = new SeasonalContract(contract.getBillingAccount(), contract.getTariff(), null, null,
                START, null);

        assertThat(open.isActiveOn(LocalDate.of(2030, 1, 1))).isTrue();
        assertThat(open.isActiveIn(LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31))).isTrue();
    }

    @Test
    void packageOfTheContractIsTheSumOfItsPackageItems() {
        ProductDefinition product = new ProductDefinition();
        contract.addPackageItem(product, null, new BigDecimal("600"));
        contract.addPackageItem(product, null, new BigDecimal("150.5"));
        contract.addPackageItem(product, null, null);

        assertThat(contract.getPackageKwh()).isEqualByComparingTo("750.5");
    }

    @Test
    void seasonEndingBeforeItStartsIsNoContract() {
        assertThatThrownBy(() -> new SeasonalContract(contract.getBillingAccount(), contract.getTariff(), null, null,
                END, START)).isInstanceOf(IllegalArgumentException.class);
    }
}
