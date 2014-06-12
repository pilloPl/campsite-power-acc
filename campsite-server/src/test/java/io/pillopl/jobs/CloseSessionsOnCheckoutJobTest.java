package io.pillopl.jobs;

import io.pillopl.supply.SupplyControlUnitImpl;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CloseSessionsOnCheckoutJobTest {

    @Test
    void jobOnlyAsksTheSupplyControlUnitToCloseStaleSessions() {
        SupplyControlUnitImpl supplyControlUnit = mock(SupplyControlUnitImpl.class);

        new CloseSessionsOnCheckoutJob(supplyControlUnit).executeInternal(null);

        verify(supplyControlUnit).closeStaleSessions();
    }
}
