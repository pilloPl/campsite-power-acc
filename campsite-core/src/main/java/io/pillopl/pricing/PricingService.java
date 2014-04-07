package io.pillopl.pricing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Prices supplies with the one tariff model: the tariff from {@link TariffResolver}, the calculators from
 * {@link PriceCalculatorFactory}, the sum rounded once to scale 2 HALF_UP. Never answers with a sentinel cost: a
 * supply that cannot be priced is PRICING_FAILED with the reason.
 */
@Service
@Transactional(readOnly = true)
public class PricingService {

    public static final int MONEY_SCALE = 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(PricingService.class);

    private final TariffResolver tariffResolver;

    public PricingService(TariffResolver tariffResolver) {
        this.tariffResolver = tariffResolver;
    }

    /**
     * Resolves the tariff of the socket (and card) at plug-in and prices the supply.
     */
    public PriceCalculation calculateCost(PricingInput pricingInput) {
        if (pricingInput == null) {
            throw new IllegalArgumentException("Pricing data is missing");
        }

        if (!pricingInput.isBillable()) {
            return PriceCalculation.notBillable();
        }

        if (pricingInput.getSocketId() == null) {
            LOGGER.warn("Socket data is missing: " + pricingInput);
            return PriceCalculation.failed(null, "Socket is missing");
        }

        if (pricingInput.getPlugIn() == null) {
            LOGGER.warn("Plug-in is missing: " + pricingInput);
            return PriceCalculation.failed(null, "Plug-in is missing");
        }

        ResolvedTariff resolved;
        try {
            resolved = tariffResolver.resolve(pricingInput.getSocketId(), pricingInput.getCardId(),
                    pricingInput.getPlugIn());
        } catch (NoTariffFoundException | IllegalArgumentException e) {
            LOGGER.warn("No tariff found: " + e.getMessage());
            return PriceCalculation.failed(null, e.getMessage());
        }

        return calculateCost(resolved, pricingInput);
    }

    /**
     * Prices the supply with a tariff chosen by the caller (seasonal billing, simulation of a tariff).
     */
    public PriceCalculation calculateCost(Tariff tariff, PricingInput pricingInput) {
        if (pricingInput == null) {
            throw new IllegalArgumentException("Pricing data is missing");
        }
        if (tariff == null) {
            return PriceCalculation.failed(null, "Tariff is missing");
        }
        if (!pricingInput.isBillable()) {
            return PriceCalculation.notBillable();
        }
        return calculateCost(new ResolvedTariff(tariff, TariffSource.GIVEN), pricingInput);
    }

    /**
     * Price text of the tariff of a socket for the answer to the start of a supply (shown on the pedestal display).
     *
     * @return the text or null (logged) when the socket has no tariff
     */
    public String getPriceInformation(Long socketId, Long cardId, LocalDateTime at) {
        try {
            return TariffPriceText.describe(tariffResolver.resolve(socketId, cardId, at).tariff());
        } catch (NoTariffFoundException | IllegalArgumentException e) {
            LOGGER.warn(String.format("No price information for socket %s: %s", socketId, e.getMessage()));
            return null;
        }
    }

    PriceCalculation calculateCost(ResolvedTariff resolved, PricingInput pricingInput) {
        List<PriceCalculator<PricingInput>> calculators;
        try {
            calculators = PriceCalculatorFactory.getCalculatorsForTariff(resolved.tariff(), pricingInput);
        } catch (PriceCalculationException e) {
            LOGGER.error("Error while calculating price of " + pricingInput + ": " + e.getMessage());
            return PriceCalculation.failed(resolved, e.getMessage());
        }

        if (calculators.isEmpty()) {
            LOGGER.warn(String.format("No element of tariff '%s' applies to %s", resolved.tariff().getName(),
                    pricingInput));
            return PriceCalculation.failed(resolved, "No element of tariff '" + resolved.tariff().getName()
                    + "' applies");
        }

        return calculateSupplyCost(resolved, pricingInput, calculators);
    }

    private PriceCalculation calculateSupplyCost(ResolvedTariff resolved, PricingInput pricingInput,
                                                 List<PriceCalculator<PricingInput>> calculators) {
        BigDecimal cost = BigDecimal.ZERO;
        List<PriceCalculationLine> lines = new ArrayList<>();

        for (PriceCalculator<PricingInput> priceCalculator : calculators) {
            try {
                BigDecimal amount = priceCalculator.calculate();
                cost = cost.add(amount);
                PriceComponent component = priceCalculator.getPrice();
                lines.add(new PriceCalculationLine(
                        component.getTariffElement() != null ? component.getTariffElement().getPosition() : 0,
                        component.getDimensionType(), component.getPrice(), component.getStepSize(),
                        priceCalculator.getShare(), priceCalculator.getBilledQuantity(), amount));
            } catch (PriceCalculationException e) {
                LOGGER.error("Error while calculating price of " + pricingInput + ": " + e.getMessage());
                return PriceCalculation.failed(resolved, e.getMessage());
            }
        }

        return PriceCalculation.priced(resolved, cost.setScale(MONEY_SCALE, RoundingMode.HALF_UP), lines);
    }
}
