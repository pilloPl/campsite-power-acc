package io.pillopl.order;

import io.pillopl.group.Group;
import io.pillopl.group.GroupType;
import io.pillopl.guest.BillingAccount;
import io.pillopl.guest.Guest;
import io.pillopl.guest.GuestType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 1, 10, 0);

    final Group club = new Group(GroupType.ISSUER, "CLUB");

    static ProductDefinition seasonalPackage() {
        ProductDefinition product = new ProductDefinition();
        product.setProductType(ProductDefinition.ProductType.SEASONAL_POWER_PACKAGE);
        product.setTitle("Seasonal pitch power package");
        product.setHasFixedPrice(true);
        product.setFixedPrice(new BigDecimal("390.00"));
        product.setFixedPricePeriod(ProductDefinition.FixedPricePeriod.SEASONAL);
        product.setPackageKwh(new BigDecimal("600"));
        return product;
    }

    Order newGuestOrder() {
        Order order = Order.forNewGuest(new Guest.Builder(club, GuestType.SEASONAL, "Horvat").firstName("Ana")
                .iban("HR1210010051863000160").carPlate("ZG 1 AB").build());
        order.setOrderNumber("HR-100001");
        return order;
    }

    @Test
    void orderOfANewGuestWaitsForTheErpPartnerAndAccount() {
        Order order = newGuestOrder();
        order.addItem(seasonalPackage(), 1);

        assertThat(order.submit(NOW)).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_ERP_ACCOUNT);
        assertThat(order.isExported()).isFalse();
    }

    @Test
    void orderOfAGuestKnownToTheErpCanBeActivatedAtOnce() {
        Guest guest = new Guest(club, GuestType.SEASONAL, "Horvat");
        guest.setErpPartnerNo("P-77");
        Order order = Order.forGuest(guest, new BillingAccount(guest, "400012345678", "Season"));
        order.addItem(seasonalPackage(), 1);

        assertThat(order.submit(NOW)).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
        order.activate(guest, order.getBillingAccount(), NOW);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ACTIVE);
        assertThat(order.getActivationDate()).isEqualTo(NOW);
    }

    @Test
    void emptyOrderOrSubmittedOrderCannotBeSubmitted() {
        Order order = newGuestOrder();
        assertThatThrownBy(() -> order.submit(NOW)).isInstanceOf(IllegalStateException.class);

        order.addItem(seasonalPackage(), 1);
        order.submit(NOW);
        assertThatThrownBy(() -> order.submit(NOW)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> order.addItem(seasonalPackage(), 1)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void erpNumbersAreTakenOnlyWhileTheOrderWaitsAndBothAreNeeded() {
        Order order = newGuestOrder();
        order.addItem(seasonalPackage(), 1);
        assertThatThrownBy(() -> order.takeErpNumbers("P-1", "400012345678", NOW))
                .isInstanceOf(IllegalStateException.class);

        order.submit(NOW);
        assertThatThrownBy(() -> order.takeErpNumbers("P-1", " ", NOW)).isInstanceOf(IllegalArgumentException.class);
        order.takeErpNumbers("P-1", "400012345678", NOW);
        assertThat(order.hasErpNumbers()).isTrue();
    }

    @Test
    void orderWithoutErpNumbersCannotBeActivated() {
        Order order = newGuestOrder();
        order.addItem(seasonalPackage(), 1);
        order.submit(NOW);

        assertThatThrownBy(() -> order.activate(new Guest(), null, NOW)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void onlyOpenOrdersCanBeCancelled() {
        Order order = newGuestOrder();
        order.addItem(seasonalPackage(), 1);
        order.submit(NOW);

        order.cancel("guest changed his mind", NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getRemarks()).isEqualTo("Cancelled: guest changed his mind");
        assertThatThrownBy(() -> order.cancel("again", NOW)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void itemKeepsThePriceAndPackageAsOrderedAndTheOrderSumsTheFixedAmounts() {
        Order order = newGuestOrder();
        ProductDefinition product = seasonalPackage();
        OrderItem item = order.addItem(product, 2);
        product.setFixedPrice(new BigDecimal("450.00"));

        assertThat(item.getPos()).isEqualTo(1);
        assertThat(item.getFixedPrice()).isEqualByComparingTo("390.00");
        assertThat(item.getPackageKwh()).isEqualByComparingTo("600");
        assertThat(order.getFixedAmount()).isEqualByComparingTo("780.00");
        assertThat(order.containsSeasonalPackage()).isTrue();
        assertThatThrownBy(() -> order.addItem(product, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void orderCopiesTheGuestDetailsAndGivesThemBack() {
        Order order = newGuestOrder();

        Guest guest = order.toGuestDetails().build();

        assertThat(order.getName()).isEqualTo("Horvat");
        assertThat(guest.getFirstName()).isEqualTo("Ana");
        assertThat(guest.getIban()).isEqualTo("HR1210010051863000160");
        assertThat(guest.getCarPlate()).isEqualTo("ZG 1 AB");
        assertThat(guest.getGuestType()).isEqualTo(GuestType.SEASONAL);
        assertThat(guest.getReferenceNo()).isEqualTo("HR-100001");
        assertThat(guest.getGroup()).isSameAs(club);
    }
}
