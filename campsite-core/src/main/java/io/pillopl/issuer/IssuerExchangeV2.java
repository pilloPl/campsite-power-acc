package io.pillopl.issuer;

import io.pillopl.dto.consumption.ConsumptionRecordDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface generation 2: JSON over HTTP to the partner's REST interface at {@link IssuerPartner#getEndpoint()}.
 * <pre>
 *   GET  /cards                 card list changes of the partner since the last call
 *   POST /cards                 card list changes of the network
 *   POST /consumption-records   records of the partner's members
 *   GET  /consumption-records   records of the partner's campsites since the last call
 * </pre>
 */
@Component
public class IssuerExchangeV2 implements IssuerExchange {

    private static final Logger LOG = LoggerFactory.getLogger(IssuerExchangeV2.class);

    static final String CARDS = "/cards";

    static final String CONSUMPTION_RECORDS = "/consumption-records";

    static final int CONNECT_TIMEOUT_IN_MILLIS = 10_000;

    static final int READ_TIMEOUT_IN_MILLIS = 60_000;

    private final RestClient restClient;

    @Autowired
    public IssuerExchangeV2(ObjectProvider<RestClient.Builder> restClientBuilder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_IN_MILLIS);
        requestFactory.setReadTimeout(READ_TIMEOUT_IN_MILLIS);
        this.restClient = restClientBuilder.getIfAvailable(RestClient::builder).requestFactory(requestFactory)
                .build();
    }

    IssuerExchangeV2(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public IssuerPartner.InterfaceVersion getInterfaceVersion() {
        return IssuerPartner.InterfaceVersion.V2;
    }

    @Override
    public List<CardLine> fetchCards(IssuerPartner partner) {
        try {
            List<CardLine> cards = restClient.get()
                    .uri(url(partner, CARDS))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<CardLine>>() {
                    });
            LOG.info(String.format("%s card lines fetched from %s", cards != null ? cards.size() : 0,
                    partner.getName()));
            return cards != null ? cards : new ArrayList<>();
        } catch (RestClientException e) {
            throw new IssuerExchangeException("Cards of " + partner.getName() + " not fetched: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendCards(IssuerPartner partner, List<CardLine> cards) {
        try {
            restClient.post()
                    .uri(url(partner, CARDS))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cards)
                    .retrieve()
                    .toBodilessEntity();
            LOG.info(String.format("%s card lines sent to %s", cards.size(), partner.getName()));
        } catch (RestClientException e) {
            throw new IssuerExchangeException("Cards for " + partner.getName() + " not sent: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendConsumptionRecords(IssuerPartner partner, List<ConsumptionRecordDTO> records) {
        try {
            restClient.post()
                    .uri(url(partner, CONSUMPTION_RECORDS))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(records)
                    .retrieve()
                    .toBodilessEntity();
            LOG.info(String.format("%s consumption records sent to %s", records.size(), partner.getName()));
        } catch (RestClientException e) {
            throw new IssuerExchangeException("Consumption records for " + partner.getName() + " not sent: "
                    + e.getMessage(), e);
        }
    }

    @Override
    public List<ConsumptionRecordDTO> fetchConsumptionRecords(IssuerPartner partner) {
        try {
            List<ConsumptionRecordDTO> records = restClient.get()
                    .uri(url(partner, CONSUMPTION_RECORDS))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ConsumptionRecordDTO>>() {
                    });
            LOG.info(String.format("%s consumption records fetched from %s", records != null ? records.size() : 0,
                    partner.getName()));
            return records != null ? records : new ArrayList<>();
        } catch (RestClientException e) {
            throw new IssuerExchangeException("Consumption records of " + partner.getName() + " not fetched: "
                    + e.getMessage(), e);
        }
    }

    private static String url(IssuerPartner partner, String path) {
        String endpoint = partner.getEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            throw new IssuerExchangeException("No REST endpoint for " + partner.getName());
        }
        return endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) + path : endpoint + path;
    }
}
