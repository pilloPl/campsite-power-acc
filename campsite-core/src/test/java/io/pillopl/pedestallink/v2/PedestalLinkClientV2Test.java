package io.pillopl.pedestallink.v2;

import io.pillopl.card.LocalCardListEntry;
import io.pillopl.card.LocalCardListSendResult;
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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PedestalLinkClientV2Test {

    static final String URL = "http://pedestal-p101.campsite.test/link";
    static final String ACCEPTED = "{\"status\":\"ACCEPTED\"}";

    MockRestServiceServer server;
    PedestalLinkClientV2 client;
    final Pedestal pedestal = new Pedestal();

    final LocalCardListEntry guestCard = new LocalCardListEntry("6101", LocalDate.of(2026, 7, 14), false, true);

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new PedestalLinkClientV2(builder.build());
        pedestal.setId(6L);
        pedestal.setCode("HR-BAY-P101");
        pedestal.setLinkUrl(URL);
        pedestal.setPedestalType(new PedestalType("ACME PEDESTALS", "P5", PedestalLinkVersion.V2));
    }

    @Test
    void listIsPostedUnderTheV2CommandName() {
        server.expect(requestTo(URL + "/localList")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.listVersion").value(7))
                .andExpect(jsonPath("$.updateType").value("FULL"))
                .andRespond(withSuccess(ACCEPTED, MediaType.APPLICATION_JSON));

        assertThat(client.sendLocalList(pedestal, true, List.of(guestCard), 7, false).success()).isTrue();
        server.verify();
    }

    @Test
    void v3PedestalIsServedWithTheV2Commands() {
        pedestal.setPedestalType(new PedestalType("ACME PEDESTALS", "P6", PedestalLinkVersion.V3));
        server.expect(requestTo(URL + "/requestSupplyStart")).andExpect(jsonPath("$.socketNo").value(2))
                .andExpect(jsonPath("$.authTag").value("QR1005"))
                .andRespond(withSuccess(ACCEPTED, MediaType.APPLICATION_JSON));

        assertThat(client.remoteSupplyStart(pedestal, 2, "QR1005")).isTrue();
        server.verify();
    }

    @Test
    void remoteSupplyStopPostsTheTransaction() {
        server.expect(requestTo(URL + "/requestSupplyStop")).andExpect(jsonPath("$.transactionId").value(1007))
                .andRespond(withSuccess(ACCEPTED, MediaType.APPLICATION_JSON));

        assertThat(client.remoteSupplyStop(pedestal, 1007)).isTrue();
        server.verify();
    }

    @Test
    void listForAnUnreachablePedestalIsQueuedAndSentWhenThePedestalIsBack() {
        server.expect(requestTo(URL + "/localList")).andRespond(withException(new IOException("timeout")));
        server.expect(requestTo(URL + "/localList")).andExpect(jsonPath("$.listVersion").value(3))
                .andRespond(withSuccess(ACCEPTED, MediaType.APPLICATION_JSON));

        assertThat(client.sendLocalList(pedestal, true, List.of(guestCard), 3, true).success()).isTrue();
        assertThat(client.hasQueuedLocalList(pedestal)).isTrue();

        assertThat(client.sendQueuedLocalList(pedestal).success()).isTrue();
        assertThat(client.hasQueuedLocalList(pedestal)).isFalse();
        server.verify();
    }

    @Test
    void remoteSupplyStartRejectedByThePedestalFails() {
        server.expect(requestTo(URL + "/requestSupplyStart"))
                .andRespond(withSuccess("{\"status\":\"REJECTED\"}", MediaType.APPLICATION_JSON));

        assertThat(client.remoteSupplyStart(pedestal, 1, "QR1005")).isFalse();
    }

    @Test
    void v1PedestalIsNotServedByLinkV2() {
        pedestal.setPedestalType(new PedestalType("ACME PEDESTALS", "P4", PedestalLinkVersion.V1));

        LocalCardListSendResult result = client.deleteLocalList(pedestal);

        assertThat(result.success()).isFalse();
        assertThat(result.errorMessage()).isEqualTo("Pedestal link V1 is not served by link v2");
        assertThat(client.remoteSupplyStop(pedestal, 1007)).isFalse();
        server.verify();
    }

    @Test
    void pedestalWithoutTypeIsNotServedByLinkV2() {
        pedestal.setPedestalType(null);

        assertThat(client.remoteSupplyStart(pedestal, 1, "QR1005")).isFalse();
        server.verify();
    }

    @Test
    void serviceCommandsPostTheSocketToTheirOwnAddress() {
        server.expect(requestTo(URL + "/requestStatus")).andExpect(jsonPath("$.socketNo").value(2))
                .andRespond(withSuccess(ACCEPTED, MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL + "/resetBreaker")).andExpect(jsonPath("$.socketNo").value(1))
                .andRespond(withSuccess(ACCEPTED, MediaType.APPLICATION_JSON));

        assertThat(client.requestStatus(pedestal, 2)).isTrue();
        assertThat(client.resetBreaker(pedestal, 1)).isTrue();
        server.verify();
    }
}
