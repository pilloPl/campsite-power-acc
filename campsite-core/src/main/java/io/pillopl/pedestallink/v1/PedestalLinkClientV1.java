package io.pillopl.pedestallink.v1;

import io.pillopl.card.LocalCardListEntry;
import io.pillopl.card.LocalCardListSendResult;
import io.pillopl.card.LocalCardListSender;
import io.pillopl.dto.card.LocalCardListEntryDTO;
import io.pillopl.dto.pedestallink.v1.SendLocalListRequest;
import io.pillopl.dto.pedestallink.v1.SendLocalListResponse;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalLinkVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Link v1 from the pedestal backend to the pedestal: posts local card lists to the controller's link address. A list
 * that cannot be delivered is queued when the caller asks for it and sent with the next boot or heartbeat of the
 * pedestal; a later list for the same pedestal is merged into the queued one.
 */
@Primary
@Component
public class PedestalLinkClientV1 implements LocalCardListSender {

    private static final Logger LOG = LoggerFactory.getLogger(PedestalLinkClientV1.class);

    static final String SEND_LOCAL_LIST = "/sendLocalList";

    static final int CONNECT_TIMEOUT_IN_MILLIS = 5_000;

    static final int READ_TIMEOUT_IN_MILLIS = 15_000;

    private final RestClient restClient;

    private final Map<Long, SendLocalListRequest> queuedLists = new ConcurrentHashMap<>();

    @Autowired
    public PedestalLinkClientV1(ObjectProvider<RestClient.Builder> restClientBuilder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_IN_MILLIS);
        requestFactory.setReadTimeout(READ_TIMEOUT_IN_MILLIS);
        this.restClient = restClientBuilder.getIfAvailable(RestClient::builder).requestFactory(requestFactory)
                .build();
    }

    PedestalLinkClientV1(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public LocalCardListSendResult sendLocalList(Pedestal pedestal, boolean fullUpdate,
                                                 List<LocalCardListEntry> entries, int version, boolean useCache) {
        String notSendable = checkSendable(pedestal);
        if (notSendable != null) {
            LOG.warn(String.format("Local card list version %d for %s not sent: %s", version, pedestal, notSendable));
            return LocalCardListSendResult.failed(notSendable);
        }
        SendLocalListRequest request = toRequest(fullUpdate, entries, version);
        if (queuedLists.containsKey(pedestal.getId())) {
            // keep the order: a list is queued already, the new one is merged into it
            queue(pedestal, request);
            return sendQueuedLocalList(pedestal);
        }
        try {
            return send(pedestal, request);
        } catch (RestClientException e) {
            if (useCache) {
                LOG.info(String.format("Pedestal %s not reachable (%s), local card list version %d queued", pedestal,
                        e.getMessage(), version));
                queue(pedestal, request);
                return LocalCardListSendResult.ok();
            }
            LOG.warn(String.format("Local card list version %d for %s not sent: %s", version, pedestal,
                    e.getMessage()));
            return LocalCardListSendResult.failed("Pedestal not reachable: " + e.getMessage());
        }
    }

    @Override
    public LocalCardListSendResult deleteLocalList(Pedestal pedestal) {
        String notSendable = checkSendable(pedestal);
        if (notSendable != null) {
            return LocalCardListSendResult.failed(notSendable);
        }
        queuedLists.remove(pedestal.getId());
        try {
            return send(pedestal, new SendLocalListRequest(0, SendLocalListRequest.CLEAR, List.of(), List.of()));
        } catch (RestClientException e) {
            LOG.warn(String.format("Local card list of %s not deleted: %s", pedestal, e.getMessage()));
            return LocalCardListSendResult.failed("Pedestal not reachable: " + e.getMessage());
        }
    }

    /**
     * The pedestal is online (boot, heartbeat): sends the list queued for it, if any. A list that fails again stays
     * queued.
     */
    public LocalCardListSendResult sendQueuedLocalList(Pedestal pedestal) {
        SendLocalListRequest queued = pedestal != null ? queuedLists.remove(pedestal.getId()) : null;
        if (queued == null) {
            return LocalCardListSendResult.ok();
        }
        try {
            LocalCardListSendResult result = send(pedestal, queued);
            LOG.info(String.format("Queued local card list version %d sent to %s: %s", queued.listVersion(),
                    pedestal, result));
            return result;
        } catch (RestClientException e) {
            queuedLists.merge(pedestal.getId(), queued, (newer, older) -> merge(older, newer));
            LOG.info(String.format("Queued local card list version %d for %s still not sent: %s",
                    queued.listVersion(), pedestal, e.getMessage()));
            return LocalCardListSendResult.ok();
        }
    }

    public boolean hasQueuedLocalList(Pedestal pedestal) {
        return pedestal != null && queuedLists.containsKey(pedestal.getId());
    }

    private LocalCardListSendResult send(Pedestal pedestal, SendLocalListRequest request) {
        SendLocalListResponse response = restClient.post()
                .uri(pedestal.getLinkUrl() + SEND_LOCAL_LIST)
                .body(request)
                .retrieve()
                .body(SendLocalListResponse.class);
        if (response == null || !response.isAccepted()) {
            String status = response != null ? response.status() : "no answer";
            LOG.warn(String.format("Pedestal %s did not accept local card list version %d (%s): %s", pedestal,
                    request.listVersion(), request.updateType(), status));
            return LocalCardListSendResult.failed("Pedestal answered " + status);
        }
        LOG.debug(String.format("Pedestal %s accepted local card list version %d (%s, %d entries)", pedestal,
                request.listVersion(), request.updateType(), request.entries().size()));
        return LocalCardListSendResult.ok();
    }

    private void queue(Pedestal pedestal, SendLocalListRequest request) {
        queuedLists.merge(pedestal.getId(), request, PedestalLinkClientV1::merge);
    }

    /**
     * One list for the pedestal from a queued list and a newer one: a full list or a clear replaces the queued
     * list; a differential update is applied to it (to a queued full list or clear it gives a full list).
     */
    static SendLocalListRequest merge(SendLocalListRequest older, SendLocalListRequest newer) {
        if (!SendLocalListRequest.DIFFERENTIAL.equals(newer.updateType())) {
            return newer;
        }
        Map<String, LocalCardListEntryDTO> entries = new LinkedHashMap<>();
        older.entries().forEach(entry -> entries.put(entry.rfid(), entry));
        Set<String> removed = new LinkedHashSet<>(older.removedRfids());
        for (LocalCardListEntryDTO entry : newer.entries()) {
            entries.put(entry.rfid(), entry);
            removed.remove(entry.rfid());
        }
        for (String rfid : newer.removedRfids()) {
            entries.remove(rfid);
            removed.add(rfid);
        }
        if (SendLocalListRequest.DIFFERENTIAL.equals(older.updateType())) {
            return new SendLocalListRequest(newer.listVersion(), SendLocalListRequest.DIFFERENTIAL,
                    new ArrayList<>(entries.values()), new ArrayList<>(removed));
        }
        return new SendLocalListRequest(newer.listVersion(), SendLocalListRequest.FULL,
                new ArrayList<>(entries.values()), List.of());
    }

    private static String checkSendable(Pedestal pedestal) {
        if (pedestal.getPedestalType() != null
                && pedestal.getPedestalType().getLinkVersion() != PedestalLinkVersion.V1) {
            return "Pedestal link " + pedestal.getPedestalType().getLinkVersion() + " cannot send local card lists";
        }
        if (pedestal.getLinkUrl() == null || pedestal.getLinkUrl().isBlank()) {
            return "Pedestal has no link address";
        }
        return null;
    }

    private static SendLocalListRequest toRequest(boolean fullUpdate, List<LocalCardListEntry> entries, int version) {
        List<LocalCardListEntryDTO> active = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        for (LocalCardListEntry entry : entries) {
            if (entry.active()) {
                active.add(new LocalCardListEntryDTO(entry.rfid(), entry.validTo(), entry.houseCard()));
            } else if (!fullUpdate) {
                removed.add(entry.rfid());
            }
        }
        return new SendLocalListRequest(version, fullUpdate ? SendLocalListRequest.FULL
                : SendLocalListRequest.DIFFERENTIAL, active, removed);
    }
}
