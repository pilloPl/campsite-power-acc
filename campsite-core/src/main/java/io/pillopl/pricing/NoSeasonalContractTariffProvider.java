package io.pillopl.pricing;

import io.pillopl.infrastructure.Socket;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Default until the seasonal contracts exist: no card has a contract tariff. The guests module replaces it with a
 * {@code @Primary} implementation.
 */
@Component
public class NoSeasonalContractTariffProvider implements SeasonalContractTariffProvider {

    @Override
    public Tariff findContractTariff(Socket socket, Long cardId, LocalDateTime at) {
        return null;
    }
}
