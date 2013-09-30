package io.pillopl.card;

import io.pillopl.infrastructure.Pedestal;

import java.util.List;

/**
 * Sends local card lists to pedestal controllers over the pedestal link. The pedestal link implements it (as primary
 * bean) for the link generation of the pedestal type.
 */
public interface LocalCardListSender {

    /**
     * @param fullUpdate true replaces the whole list in the controller, false adds or changes the given entries
     * @param useCache   true queues the list when the pedestal is not reachable now
     */
    LocalCardListSendResult sendLocalList(Pedestal pedestal, boolean fullUpdate, List<LocalCardListEntry> entries,
                                          int version, boolean useCache);

    /**
     * Clears the list in the controller (before a full distribution).
     */
    LocalCardListSendResult deleteLocalList(Pedestal pedestal);
}
