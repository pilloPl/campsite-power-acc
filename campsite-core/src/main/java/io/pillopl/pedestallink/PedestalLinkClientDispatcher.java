package io.pillopl.pedestallink;

import io.pillopl.card.LocalCardListEntry;
import io.pillopl.card.LocalCardListSendResult;
import io.pillopl.card.LocalCardListSender;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalLinkVersion;
import io.pillopl.pedestallink.v1.PedestalLinkClientV1;
import io.pillopl.pedestallink.v2.PedestalLinkClientV2;
import io.pillopl.supply.RemoteSupplyControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The pedestal link seen by the card lists and the remote control: picks the client of the link generation spoken by
 * the pedestal type. A pedestal without type is a v1 pedestal (the oldest controllers); v3 controllers kept the
 * v2 command set and are served by the v2 client.
 */
@Primary
@Component
public class PedestalLinkClientDispatcher implements LocalCardListSender, RemoteSupplyControl {

    private static final Logger LOG = LoggerFactory.getLogger(PedestalLinkClientDispatcher.class);

    private final PedestalLinkClientV1 pedestalLinkClientV1;

    private final PedestalLinkClientV2 pedestalLinkClientV2;

    public PedestalLinkClientDispatcher(PedestalLinkClientV1 pedestalLinkClientV1,
                                        PedestalLinkClientV2 pedestalLinkClientV2) {
        this.pedestalLinkClientV1 = pedestalLinkClientV1;
        this.pedestalLinkClientV2 = pedestalLinkClientV2;
    }

    @Override
    public LocalCardListSendResult sendLocalList(Pedestal pedestal, boolean fullUpdate,
                                                 List<LocalCardListEntry> entries, int version, boolean useCache) {
        return isLinkV1(pedestal)
                ? pedestalLinkClientV1.sendLocalList(pedestal, fullUpdate, entries, version, useCache)
                : pedestalLinkClientV2.sendLocalList(pedestal, fullUpdate, entries, version, useCache);
    }

    @Override
    public LocalCardListSendResult deleteLocalList(Pedestal pedestal) {
        return isLinkV1(pedestal)
                ? pedestalLinkClientV1.deleteLocalList(pedestal)
                : pedestalLinkClientV2.deleteLocalList(pedestal);
    }

    @Override
    public boolean remoteSupplyStart(Pedestal pedestal, int socketNo, String authTag) {
        return isLinkV1(pedestal)
                ? pedestalLinkClientV1.remoteSupplyStart(pedestal, socketNo, authTag)
                : pedestalLinkClientV2.remoteSupplyStart(pedestal, socketNo, authTag);
    }

    @Override
    public boolean remoteSupplyStop(Pedestal pedestal, int transactionId) {
        return isLinkV1(pedestal)
                ? pedestalLinkClientV1.remoteSupplyStop(pedestal, transactionId)
                : pedestalLinkClientV2.remoteSupplyStop(pedestal, transactionId);
    }

    static PedestalLinkVersion linkVersionOf(Pedestal pedestal) {
        if (pedestal.getPedestalType() == null || pedestal.getPedestalType().getLinkVersion() == null) {
            LOG.debug(String.format("Pedestal %s has no link version, served by link v1", pedestal));
            return PedestalLinkVersion.V1;
        }
        return pedestal.getPedestalType().getLinkVersion();
    }

    private static boolean isLinkV1(Pedestal pedestal) {
        return linkVersionOf(pedestal) == PedestalLinkVersion.V1;
    }
}
