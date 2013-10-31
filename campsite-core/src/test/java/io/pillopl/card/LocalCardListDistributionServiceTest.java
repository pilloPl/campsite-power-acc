package io.pillopl.card;

import io.pillopl.config.SystemPropertyService;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.persistence.dao.PedestalDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalCardListDistributionServiceTest {

    final PedestalDao pedestalDao = mock(PedestalDao.class);
    final LocalCardListService localCardListService = mock(LocalCardListService.class);
    final LocalCardListSender sender = mock(LocalCardListSender.class);
    final SystemPropertyService systemPropertyService = mock(SystemPropertyService.class);
    final LocalCardListDistributionService service = new LocalCardListDistributionService(pedestalDao,
            localCardListService, sender, systemPropertyService);

    final Pedestal sent = pedestal("HR-BAY-P001");
    final Pedestal withoutList = pedestal("HR-BAY-P002");
    final Pedestal offline = pedestal("HR-BAY-P003");

    @BeforeEach
    void setUp() {
        when(systemPropertyService.getLocalCardListDistributionConcurrentThreads()).thenReturn(2);
        when(systemPropertyService.getLocalCardListDistributionMaximumDurationInMinutes()).thenReturn(5);
        when(systemPropertyService.getLocalCardListDistributionWaitTimeAfterDeleteInMillis()).thenReturn(0L);
        when(sender.deleteLocalList(org.mockito.ArgumentMatchers.any()))
                .thenReturn(LocalCardListSendResult.failed("offline"));
        when(localCardListService.sendFull("HR-BAY-P001", true)).thenReturn(true);
        when(localCardListService.sendFull("HR-BAY-P002", true)).thenReturn(false);
        when(localCardListService.sendFull("HR-BAY-P003", true))
                .thenThrow(new LocalCardListException("HR-BAY-P003", "offline"));
    }

    @Test
    void everyPedestalGetsItsListDeletedAndSentAgain() {
        when(pedestalDao.findPedestalsForLocalCardListDistribution()).thenReturn(List.of(sent, withoutList, offline));

        LocalCardListDistributionResult result = service.distribute();

        assertThat(result).isEqualTo(new LocalCardListDistributionResult(3, 1, 1, true));
        verify(sender).deleteLocalList(sent);
        verify(sender).deleteLocalList(offline);
    }

    @Test
    void nothingToDistributeFinishesAtOnce() {
        when(pedestalDao.findPedestalsForLocalCardListDistribution()).thenReturn(List.of());

        assertThat(service.distribute()).isEqualTo(new LocalCardListDistributionResult(0, 0, 0, true));
    }

    private static Pedestal pedestal(String code) {
        Pedestal pedestal = new Pedestal();
        pedestal.setCode(code);
        return pedestal;
    }
}
