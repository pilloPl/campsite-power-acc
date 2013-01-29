package io.pillopl.group.attribute;

import io.pillopl.group.Group;
import io.pillopl.group.Market;
import io.pillopl.persistence.dao.SupplierMarkerDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class SupplierMarkerIntegrationTest extends GroupAttributeIntegrationTestBase {

    @Autowired SupplierMarkerService supplierMarkerService;
    @Autowired SupplierMarkerDao supplierMarkerDao;

    @Test
    void pedestalsInASupplierGroupAreInStock() {
        Group stock = supplierMarkerService.createSupplierStock("ACME PEDESTALS", Market.HR);
        Group pallet = groupManager.createChildGroup(stock, "ACME PALLET 1", "Pallet 1");

        assertThat(stock.getName()).isEqualTo("SUPPLIER ACME PEDESTALS");
        assertThat(stock.getParent()).isEqualTo(hrSites);
        assertThat(supplierMarkerService.isInSupplierStock(pallet)).isTrue();
        assertThat(supplierMarkerService.isInSupplierStock(section)).isFalse();
    }

    @Test
    void inactiveSupplierMarkerBelowAnActiveOneTakesTheGroupOutOfStock() {
        Group stock = supplierMarkerService.createSupplierStock("ACME PEDESTALS", Market.HR);
        Group delivered = groupManager.createChildGroup(stock, "ACME DELIVERED", "Delivered");
        supplierMarkerDao.saveAndFlush(on(delivered, new SupplierMarker("delivered", false)));

        assertThat(supplierMarkerService.isInSupplierStock(delivered)).isFalse();
    }
}
