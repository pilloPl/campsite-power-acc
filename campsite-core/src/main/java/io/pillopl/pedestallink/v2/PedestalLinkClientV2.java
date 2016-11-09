package io.pillopl.pedestallink.v2;

import io.pillopl.card.LocalCardListEntry;
import io.pillopl.card.LocalCardListSendResult;
import io.pillopl.card.LocalCardListSender;
import io.pillopl.dto.card.LocalCardListEntryDTO;
import io.pillopl.dto.pedestallink.v1.RemoteCommandRequest;
import io.pillopl.dto.pedestallink.v1.RemoteCommandResponse;
import io.pillopl.dto.pedestallink.v1.SendLocalListRequest;
import io.pillopl.dto.pedestallink.v1.SendLocalListResponse;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalLinkVersion;
import io.pillopl.supply.RemoteSupplyControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Link v2 and v3 from the pedestal backend to the pedestal: a conscious copy of the v1 client with the v2 command names
 * (v3 controllers kept the v2 command set). Local card lists and remote supply starts and stops are posted to the
 * controller's link address. A list that cannot be delivered is queued when the caller asks for it and sent with the
 * next boot or heartbeat of the pedestal; a later list for the same pedestal is merged into the queued one.
 */
@Component
public class PedestalLinkClientV2 implements LocalCardListSender, RemoteSupplyControl {

    private static final Logger LOG = LoggerFactory.getLogger(PedestalLinkClientV2.class);

    static final String SEND_LOCAL_LIST = "/localList";

    static final String REMOTE_SUPPLY_START = "/requestSupplyStart";

    static final String REQUEST_STATUS = "/requestStatus";

    static final String RESET_BREAKER = "/resetBreaker";

    static final String REMOTE_SUPPLY_STOP = "/requestSupplyStop";

    static final int CONNECT_TIMEOUT_IN_MILLIS = 5_000;

    static final int READ_TIMEOUT_IN_MILLIS = 15_000;

    private final RestClient restClient;

    private final Map<Long, SendLocalListRequest> queuedLists = new ConcurrentHashMap<>();

    @Autowired
    public PedestalLinkClientV2(ObjectProvider<RestClient.Builder> restClientBuilder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_IN_MILLIS);
        requestFactory.setReadTimeout(READ_TIMEOUT_IN_MILLIS);
        this.restClient = restClientBuilder.getIfAvailable(RestClient::builder).requestFactory(requestFactory)
                .build();
    }

    PedestalLinkClientV2(RestClient restClient) {
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
     * Remote supply start of a socket with an auth tag. Not queued: a start the pedestal does not get now is refused.
     */
    @Override
    public boolean remoteSupplyStart(Pedestal pedestal, int socketNo, String authTag) {
        return sendRemoteCommand(pedestal, REMOTE_SUPPLY_START, RemoteCommandRequest.start(socketNo, authTag));
    }

    /**
     * Remote supply stop of a transaction. Not queued: the caller decides what an unreachable pedestal means.
     */
    @Override
    public boolean remoteSupplyStop(Pedestal pedestal, int transactionId) {
        return sendRemoteCommand(pedestal, REMOTE_SUPPLY_STOP, RemoteCommandRequest.stop(transactionId));
    }

    /**
     * Status request of one socket (or of all). Not queued: the console asks again.
     */
    @Override
    public boolean requestStatus(Pedestal pedestal, Integer socketNo) {
        return sendRemoteCommand(pedestal, REQUEST_STATUS, RemoteCommandRequest.forSocket(socketNo));
    }

    /**
     * Breaker reset of a socket. Not queued: a reset the pedestal does not get now is refused.
     */
    @Override
    public boolean resetBreaker(Pedestal pedestal, int socketNo) {
        return sendRemoteCommand(pedestal, RESET_BREAKER, RemoteCommandRequest.forSocket(socketNo));
    }

    private boolean sendRemoteCommand(Pedestal pedestal, String command, RemoteCommandRequest request) {
        String notSendable = checkSendable(pedestal);
        if (notSendable != null) {
            LOG.warn(String.format("%s %s for %s not sent: %s", command, request, pedestal, notSendable));
            return false;
        }
        try {
            RemoteCommandResponse response = restClient.post()
                    .uri(pedestal.getLinkUrl() + command)
                    .body(request)
                    .retrieve()
                    .body(RemoteCommandResponse.class);
            if (response == null || !response.isAccepted()) {
                LOG.warn(String.format("Pedestal %s did not accept %s %s: %s", pedestal, command, request,
                        response != null ? response.status() : "no answer"));
                return false;
            }
            LOG.info(String.format("Pedestal %s accepted %s %s", pedestal, command, request));
            return true;
        } catch (RestClientException e) {
            LOG.warn(String.format("%s %s for %s failed: %s", command, request, pedestal, e.getMessage()));
            return false;
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
        queuedLists.merge(pedestal.getId(), request, PedestalLinkClientV2::merge);
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
        if (pedestal.getPedestalType() == null
                || pedestal.getPedestalType().getLinkVersion() == PedestalLinkVersion.V1) {
            return "Pedestal link " + (pedestal.getPedestalType() != null
                    ? pedestal.getPedestalType().getLinkVersion() : null) + " is not served by link v2";
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
