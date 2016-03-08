package io.pillopl.supply;

import io.pillopl.infrastructure.PedestalService;
import io.pillopl.persistence.dao.StayDao;
import io.pillopl.persistence.dao.SupplySessionDao;
import io.pillopl.util.DateService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SupplySessionManagerTest {

    static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 21, 30);

    final SupplySessionDao supplySessionDao = mock(SupplySessionDao.class);

    final SupplySessionManager sessionManager = new SupplySessionManager(supplySessionDao,
            mock(PedestalService.class), new DateService(), mock(StayDao.class));

    @Test
    void deactivatingNoSessionStoresNothing() {
        assertThat(sessionManager.deactivate(null, StopReason.LOCAL, NOW)).isFalse();

        verify(supplySessionDao, never()).save(any());
    }

    @Test
    void deactivatedSessionIsStoredClosed() {
        SupplySession session = new SupplySession();
        session.setActive(true);

        assertThat(sessionManager.deactivate(session, StopReason.BREAKER_TRIP, NOW)).isTrue();

        assertThat(session.isActive()).isFalse();
        assertThat(session.getStopReason()).isEqualTo(StopReason.BREAKER_TRIP);
        assertThat(session.getStopDate()).isEqualTo(NOW);
        verify(supplySessionDao).save(session);
    }
}
