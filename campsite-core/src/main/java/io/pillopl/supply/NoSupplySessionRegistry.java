package io.pillopl.supply;

import io.pillopl.value.SocketLinkId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Registry used until supply sessions are recorded: no socket has a session.
 */
@Component
public class NoSupplySessionRegistry implements SupplySessionRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(NoSupplySessionRegistry.class);

    @Override
    public boolean hasConcurrentSession(SocketLinkId socketLinkId, String cardRfid) {
        return false;
    }

    @Override
    public void createSession(SocketLinkId socketLinkId, String cardRfid) {
        LOG.info(String.format("Card %s authorized at socket %s, no session registry configured", cardRfid,
                socketLinkId));
    }
}
