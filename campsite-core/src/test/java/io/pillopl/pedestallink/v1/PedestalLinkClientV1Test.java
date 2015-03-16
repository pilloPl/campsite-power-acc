package io.pillopl.pedestallink.v1;

import io.pillopl.card.LocalCardListEntry;
import io.pillopl.card.LocalCardListSendResult;
import io.pillopl.dto.card.LocalCardListEntryDTO;
import io.pillopl.dto.pedestallink.v1.SendLocalListRequest;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.infrastructure.PedestalLinkVersion;
import io.pillopl.infrastructure.PedestalType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PedestalLinkClientV1Test {

    static final String URL = "http://pedestal-p001.campsite.test/link";
    static final String ACCEPTED = "{\"status\":\"ACCEPTED\"}";

    MockRestServiceServer server;
    PedestalLinkClientV1 client;
    final Pedestal pedestal = new Pedestal();

    final LocalCardListEntry guestCard = new LocalCardListEntry("6101", LocalDate.of(2026, 7, 14), false, true);
    final LocalCardListEntry houseCard = new LocalCardListEntry("6103", null, true, true);
    final LocalCardListEntry blockedCard = new LocalCardListEntry("6102", LocalDate.of(2026, 7, 14), false, false);

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new PedestalLinkClientV1(builder.build());
        pedestal.setId(5L);
        pedestal.setCode("HR-BAY-P001");
        pedestal.setLinkUrl(URL);
        pedestal.setPedestalType(new PedestalType("ACME PEDESTALS", "P4", PedestalLinkVersion.V1));
    }

    @Test
    void fullListIsPostedToTheControllerWithItsVersion() {
        server.expect(requestTo(URL + "/sendLocalList")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.listVersion").value(7))
                .andExpect(jsonPath("$.updateType").value("FULL"))
                .andExpect(jsonPath("$.entries.length()").value(2))
                .andExpect(jsonPath("$.entries[1].houseCard").value(true))
                .andRespond(withSuccess(ACCEPTED, MediaType.APPLICATION_JSON));

        LocalCardListSendResult result = client.sendLocalList(pedestal, true, List.of(guestCard, houseCard, blockedCard),
                7, false);

        assertThat(result.success()).isTrue();
        server.verify();
    }

    @Test
    void differentialUpdateRemovesTheInactiveCards() {
        server.expect(requestTo(URL + "/sendLocalList"))
                .andExpect(jsonPath("$.updateType").value("DIFFERENTIAL"))
                .andExpect(jsonPath("$.removedRfids[0]").value("6102"))
                .andRespond(withSuccess(ACCEPTED, MediaType.APPLICATION_JSON));

        assertThat(client.sendLocalList(pedestal, false, List.of(blockedCard), 8, false).success()).isTrue();
        server.verify();
    }

    @Test
    void listTheControllerDoesNotAcceptIsAFailure() {
        server.expect(requestTo(URL + "/sendLocalList"))
                .andRespond(withSuccess("{\"status\":\"VERSION_MISMATCH\"}", MediaType.APPLICATION_JSON));

        LocalCardListSendResult result = client.sendLocalList(pedestal, true, List.of(guestCard), 3, false);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).contains("VERSION_MISMATCH");
    }

    @Test
    void unreachablePedestalFailsWithoutQueue() {
        server.expect(requestTo(URL + "/sendLocalList")).andRespond(withException(new IOException("timeout")));

        assertThat(client.sendLocalList(pedestal, true, List.of(guestCard), 3, false).success()).isFalse();
        assertThat(client.hasQueuedLocalList(pedestal)).isFalse();
    }

    @Test
    void listForAnUnreachablePedestalIsQueuedAndSentWhenThePedestalIsBack() {
        server.expect(requestTo(URL + "/sendLocalList")).andRespond(withException(new IOException("timeout")));
        server.expect(requestTo(URL + "/sendLocalList")).andRespond(withException(new IOException("timeout")));
        server.expect(requestTo(URL + "/sendLocalList")).andExpect(jsonPath("$.listVersion").value(3))
                .andRespond(withSuccess(ACCEPTED, MediaType.APPLICATION_JSON));

        assertThat(client.sendLocalList(pedestal, true, List.of(guestCard), 3, true).success()).isTrue();
        assertThat(client.hasQueuedLocalList(pedestal)).isTrue();

        client.sendQueuedLocalList(pedestal);
        assertThat(client.hasQueuedLocalList(pedestal)).isTrue();

        assertThat(client.sendQueuedLocalList(pedestal).success()).isTrue();
        assertThat(client.hasQueuedLocalList(pedestal)).isFalse();
        server.verify();
    }

    @Test
    void laterUpdateIsMergedIntoTheQueuedList() {
        server.expect(requestTo(URL + "/sendLocalList")).andRespond(withException(new IOException("timeout")));
        server.expect(requestTo(URL + "/sendLocalList"))
                .andExpect(jsonPath("$.listVersion").value(4))
                .andExpect(jsonPath("$.updateType").value("FULL"))
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.entries[0].rfid").value("6103"))
                .andRespond(withSuccess(ACCEPTED, MediaType.APPLICATION_JSON));
        client.sendLocalList(pedestal, true, List.of(guestCard), 3, true);

        // the guest card is removed and the house card added while the full list waits in the queue
        LocalCardListEntry guestCardRemoved = new LocalCardListEntry("6101", null, false, false);
        LocalCardListSendResult result = client.sendLocalList(pedestal, false, List.of(guestCardRemoved, houseCard),
                4, true);

        assertThat(result.success()).isTrue();
        assertThat(client.hasQueuedLocalList(pedestal)).isFalse();
        server.verify();
    }

    @Test
    void deleteClearsTheListInTheControllerAndDropsTheQueue() {
        server.expect(requestTo(URL + "/sendLocalList")).andRespond(withException(new IOException("timeout")));
        server.expect(requestTo(URL + "/sendLocalList")).andExpect(jsonPath("$.updateType").value("CLEAR"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(ACCEPTED, MediaType.APPLICATION_JSON));
        client.sendLocalList(pedestal, true, List.of(guestCard), 3, true);

        assertThat(client.deleteLocalList(pedestal).success()).isTrue();
        assertThat(client.hasQueuedLocalList(pedestal)).isFalse();
        server.verify();
    }

    @Test
    void pedestalWithoutLinkAddressCannotGetAList() {
        pedestal.setLinkUrl(null);

        LocalCardListSendResult result = client.sendLocalList(pedestal, true, List.of(guestCard), 3, true);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Pedestal has no link address");
    }

    @Test
    void pedestalOfAnotherLinkGenerationIsNotServedByLinkV1() {
        pedestal.setPedestalType(new PedestalType("ACME PEDESTALS", "P6", PedestalLinkVersion.V3));

        assertThat(client.deleteLocalList(pedestal).success()).isFalse();
    }

    @Test
    void differentialUpdatesAreMergedIntoOne() {
        SendLocalListRequest first = new SendLocalListRequest(4, SendLocalListRequest.DIFFERENTIAL,
                List.of(new LocalCardListEntryDTO("6101", null, false)), List.of("6102"));
        SendLocalListRequest second = new SendLocalListRequest(5, SendLocalListRequest.DIFFERENTIAL,
                List.of(new LocalCardListEntryDTO("6102", null, false)), List.of("6101"));

        SendLocalListRequest merged = PedestalLinkClientV1.merge(first, second);

        assertThat(merged.listVersion()).isEqualTo(5);
        assertThat(merged.updateType()).isEqualTo(SendLocalListRequest.DIFFERENTIAL);
        assertThat(merged.entries()).extracting(LocalCardListEntryDTO::rfid).containsExactly("6102");
        assertThat(merged.removedRfids()).containsExactly("6101");
    }

    @Test
    void fullListReplacesTheQueuedList() {
        SendLocalListRequest queued = new SendLocalListRequest(4, SendLocalListRequest.DIFFERENTIAL, List.of(),
                List.of("6102"));
        SendLocalListRequest full = new SendLocalListRequest(5, SendLocalListRequest.FULL, List.of(), List.of());

        assertThat(PedestalLinkClientV1.merge(queued, full)).isSameAs(full);
    }

    @Test
    void remoteSupplyStartPostsTheSocketAndTheAuthTag() {
        server.expect(requestTo(URL + "/remoteSupplyStart")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.socketNo").value(2))
                .andExpect(jsonPath("$.authTag").value("QR1005"))
                .andRespond(withSuccess(ACCEPTED, MediaType.APPLICATION_JSON));

        assertThat(client.remoteSupplyStart(pedestal, 2, "QR1005")).isTrue();
        server.verify();
    }

    @Test
    void remoteSupplyStopPostsTheTransaction() {
        server.expect(requestTo(URL + "/remoteSupplyStop")).andExpect(jsonPath("$.transactionId").value(1007))
                .andRespond(withSuccess(ACCEPTED, MediaType.APPLICATION_JSON));

        assertThat(client.remoteSupplyStop(pedestal, 1007)).isTrue();
        server.verify();
    }

    @Test
    void remoteSupplyStartRejectedByThePedestalFails() {
        server.expect(requestTo(URL + "/remoteSupplyStart"))
                .andRespond(withSuccess("{\"status\":\"REJECTED\"}", MediaType.APPLICATION_JSON));

        assertThat(client.remoteSupplyStart(pedestal, 1, "QR1005")).isFalse();
    }

    @Test
    void remoteCommandToAnUnreachablePedestalFailsAndIsNotQueued() {
        server.expect(requestTo(URL + "/remoteSupplyStop")).andRespond(withException(new IOException("timeout")));

        assertThat(client.remoteSupplyStop(pedestal, 1007)).isFalse();
        assertThat(client.hasQueuedLocalList(pedestal)).isFalse();
    }

    @Test
    void remoteCommandNeedsALinkAddress() {
        pedestal.setLinkUrl(" ");

        assertThat(client.remoteSupplyStart(pedestal, 1, "QR1005")).isFalse();
        server.verify();
    }
}
