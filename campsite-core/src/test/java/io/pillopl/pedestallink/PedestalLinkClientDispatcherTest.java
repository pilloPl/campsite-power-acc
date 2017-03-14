package io.pillopl.pedestallink;

import io.pillopl.card.LocalCardListEntry;
import io.pillopl.card.LocalCardListSendResult;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalLinkVersion;
import io.pillopl.infrastructure.PedestalType;
import io.pillopl.pedestallink.v1.PedestalLinkClientV1;
import io.pillopl.pedestallink.v2.PedestalLinkClientV2;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PedestalLinkClientDispatcherTest {

    final PedestalLinkClientV1 v1 = mock(PedestalLinkClientV1.class);
    final PedestalLinkClientV2 v2 = mock(PedestalLinkClientV2.class);
    final PedestalLinkClientDispatcher dispatcher = new PedestalLinkClientDispatcher(v1, v2);
    final List<LocalCardListEntry> entries = List.of();

    @Test
    void v1PedestalIsServedByTheV1Client() {
        Pedestal pedestal = pedestal(PedestalLinkVersion.V1);
        when(v1.sendLocalList(pedestal, true, entries, 3, true)).thenReturn(LocalCardListSendResult.ok());
        when(v1.remoteSupplyStart(pedestal, 1, "QR1")).thenReturn(true);

        assertThat(dispatcher.sendLocalList(pedestal, true, entries, 3, true).success()).isTrue();
        assertThat(dispatcher.remoteSupplyStart(pedestal, 1, "QR1")).isTrue();
        dispatcher.remoteSupplyStop(pedestal, 77);
        dispatcher.deleteLocalList(pedestal);

        verify(v1).remoteSupplyStop(pedestal, 77);
        verify(v1).deleteLocalList(pedestal);
        verifyNoInteractions(v2);
    }

    @Test
    void v2AndV3PedestalsAreServedByTheV2Client() {
        Pedestal second = pedestal(PedestalLinkVersion.V2);
        Pedestal third = pedestal(PedestalLinkVersion.V3);
        when(v2.remoteSupplyStart(third, 2, "QR2")).thenReturn(true);

        dispatcher.sendLocalList(second, false, entries, 4, false);
        dispatcher.deleteLocalList(second);
        dispatcher.remoteSupplyStop(second, 78);
        assertThat(dispatcher.remoteSupplyStart(third, 2, "QR2")).isTrue();

        verify(v2).sendLocalList(second, false, entries, 4, false);
        verify(v2).deleteLocalList(second);
        verify(v2).remoteSupplyStop(second, 78);
        verifyNoInteractions(v1);
    }

    @Test
    void serviceCommandsFollowTheLinkGenerationOfThePedestal() {
        Pedestal first = pedestal(PedestalLinkVersion.V1);
        Pedestal third = pedestal(PedestalLinkVersion.V3);
        when(v1.requestStatus(first, null)).thenReturn(true);
        when(v2.resetBreaker(third, 2)).thenReturn(true);

        assertThat(dispatcher.requestStatus(first, null)).isTrue();
        assertThat(dispatcher.resetBreaker(third, 2)).isTrue();

        verify(v1).requestStatus(first, null);
        verify(v2).resetBreaker(third, 2);
    }

    @Test
    void pedestalWithoutTypeIsALinkV1Pedestal() {
        Pedestal pedestal = new Pedestal();

        assertThat(PedestalLinkClientDispatcher.linkVersionOf(pedestal)).isEqualTo(PedestalLinkVersion.V1);
        dispatcher.remoteSupplyStop(pedestal, 79);
        verify(v1).remoteSupplyStop(pedestal, 79);
    }

    private static Pedestal pedestal(PedestalLinkVersion linkVersion) {
        Pedestal pedestal = new Pedestal();
        pedestal.setPedestalType(new PedestalType("ACME PEDESTALS", "P" + linkVersion, linkVersion));
        return pedestal;
    }
}
