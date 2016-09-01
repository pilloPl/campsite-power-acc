package io.pillopl.issuer;

import io.pillopl.dto.consumption.ConsumptionRecordDTO;
import io.pillopl.issuer.IssuerExchange.CardLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IssuerExchangeV2Test {

    static final String URL = "http://club.test/exchange";

    MockRestServiceServer server;
    IssuerExchangeV2 exchange;
    final IssuerPartner partner = new IssuerPartner("CARAVAN CLUB", null, IssuerPartner.InterfaceVersion.V2,
            URL + "/");

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        exchange = new IssuerExchangeV2(builder.build());
    }

    @Test
    void cardLinesOfThePartnerAreFetched() {
        server.expect(requestTo(URL + "/cards")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"cardRfidHex\":\"04A1B2C3\",\"cardNumber\":\"CC-1\",\"actionCode\":\"ADD\","
                        + "\"actionDate\":\"2026-07-01 09:30\",\"billingAccountNo\":\"770012345678\","
                        + "\"validTo\":\"2026-12-31\"}]", MediaType.APPLICATION_JSON));

        List<CardLine> cards = exchange.fetchCards(partner);

        assertThat(cards).containsExactly(new CardLine("04A1B2C3", "CC-1", "ADD", "2026-07-01 09:30",
                "770012345678", LocalDate.of(2026, 12, 31)));
        server.verify();
    }

    @Test
    void networkCardListIsPosted() {
        server.expect(requestTo(URL + "/cards")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$[0].cardRfidHex").value("0A0B0C0D"))
                .andExpect(jsonPath("$[0].actionCode").value("DELETE"))
                .andRespond(withSuccess());

        exchange.sendCards(partner, List.of(new CardLine("0A0B0C0D", "HR-1", "DELETE", "2026-07-11 02:30", null,
                null)));

        server.verify();
    }

    @Test
    void endpointWithoutTrailingSlashGetsThePathAppended() {
        partner.setEndpoint(URL);
        server.expect(requestTo(URL + "/consumption-records")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(exchange.fetchConsumptionRecords(partner)).isEmpty();
        server.verify();
    }

    @Test
    void emptyBodyOfThePartnerIsAnEmptyCardList() {
        server.expect(requestTo(URL + "/cards")).andExpect(method(HttpMethod.GET)).andRespond(withSuccess());

        assertThat(exchange.fetchCards(partner)).isEmpty();
        server.verify();
    }

    @Test
    void recordsOfThePartnersMembersArePosted() {
        server.expect(requestTo(URL + "/consumption-records")).andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$[0].recordNo").value("BAY-20260710-000001"))
                .andExpect(jsonPath("$[0].cost").value(4.38))
                .andRespond(withSuccess());

        exchange.sendConsumptionRecords(partner, List.of(new ConsumptionRecordDTO(1L, "BAY-20260710-000001", null,
                "BAY-P001-1", null, null, null, null, null, "3054198966", null, null, "CARD", true, false, false,
                LocalDateTime.of(2026, 7, 9, 18, 0), LocalDateTime.of(2026, 7, 10, 9, 0), 54000L, 1,
                new BigDecimal("12.5"), null, new BigDecimal("4.38"), "EUR", null, null, null, null, null, null, null,
                null)));

        server.verify();
    }

    @Test
    void recordsOfThePartnersCampsitesAreFetched() {
        server.expect(requestTo(URL + "/consumption-records")).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"recordNo\":\"CLUB-77\",\"socketCode\":\"CLUB-P001-1\","
                        + "\"cardRfid\":\"8101\",\"plugOut\":\"2026-07-11T09:00:00\",\"kwh\":12.5,\"nights\":1}]",
                        MediaType.APPLICATION_JSON));

        List<ConsumptionRecordDTO> records = exchange.fetchConsumptionRecords(partner);

        assertThat(records).hasSize(1);
        assertThat(records.get(0).recordNo()).isEqualTo("CLUB-77");
        assertThat(records.get(0).plugOut()).isEqualTo(LocalDateTime.of(2026, 7, 11, 9, 0));
    }

    @Test
    void failingPartnerFailsTheRun() {
        server.expect(requestTo(URL + "/cards")).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> exchange.fetchCards(partner)).isInstanceOf(IssuerExchangeException.class)
                .hasMessageStartingWith("Cards of CARAVAN CLUB not fetched");
    }

    @Test
    void partnerWithoutEndpointCannotExchange() {
        partner.setEndpoint(null);

        assertThatThrownBy(() -> exchange.fetchConsumptionRecords(partner))
                .hasMessage("No REST endpoint for CARAVAN CLUB");
    }
}
