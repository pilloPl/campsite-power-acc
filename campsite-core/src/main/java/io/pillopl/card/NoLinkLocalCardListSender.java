package io.pillopl.card;

import io.pillopl.infrastructure.Pedestal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sender used while no pedestal link is configured: nothing is sent, every send is reported as failed.
 */
@Component
public class NoLinkLocalCardListSender implements LocalCardListSender {

    private static final Logger LOG = LoggerFactory.getLogger(NoLinkLocalCardListSender.class);

    static final String NO_LINK = "No pedestal link configured";

    @Override
    public LocalCardListSendResult sendLocalList(Pedestal pedestal, boolean fullUpdate,
                                                 List<LocalCardListEntry> entries, int version, boolean useCache) {
        LOG.warn(String.format("Local card list version %d with %d entries for %s not sent: %s", version,
                entries.size(), pedestal, NO_LINK));
        return LocalCardListSendResult.failed(NO_LINK);
    }

    @Override
    public LocalCardListSendResult deleteLocalList(Pedestal pedestal) {
        LOG.warn(String.format("Local card list of %s not deleted: %s", pedestal, NO_LINK));
        return LocalCardListSendResult.failed(NO_LINK);
    }
}
