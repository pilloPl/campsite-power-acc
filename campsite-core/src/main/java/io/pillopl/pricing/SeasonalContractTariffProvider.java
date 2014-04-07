package io.pillopl.pricing;

import io.pillopl.infrastructure.Socket;

import java.time.LocalDateTime;

/**
 * First step of the tariff resolution: the tariff of the seasonal contract of the card's guest. A seasonal
 * resident's contract tariff overrides every tariff of the socket and pedestal.
 * <p>
 * The guest and contract model comes with the guests module; until then {@link NoSeasonalContractTariffProvider}
 * answers that there is no contract.
 */
public interface SeasonalContractTariffProvider {

    /**
     * @param cardId the card used for the supply; null for supplies without a card
     * @return the tariff of the seasonal contract active at {@code at} for the card's guest, or null
     */
    Tariff findContractTariff(Socket socket, Long cardId, LocalDateTime at);
}
