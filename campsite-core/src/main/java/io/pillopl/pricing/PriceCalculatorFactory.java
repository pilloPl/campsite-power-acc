package io.pillopl.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the calculators that price a supply with a tariff.
 * <p>
 * Restrictions are evaluated: for every dimension the first element (in tariff order) that has a component of the
 * dimension and whose restriction matches is used. ENERGY and TIME are evaluated per {@link TariffSlice}; a supply
 * crossing a window boundary is split proportionally to time (the energy of a slice is the energy of the supply
 * times the slice's share of the duration). FLAT and PER_NIGHT are evaluated once, at plug-in. The kWh range of a
 * restriction is checked against the energy of the whole supply.
 * <p>
 * A slice for which no element of a dimension applies is not charged in that dimension.
 */
public final class PriceCalculatorFactory {

    private PriceCalculatorFactory() {
    }

    public static List<PriceCalculator<PricingInput>> getCalculatorsForTariff(Tariff tariff, PricingInput pricingInput)
            throws PriceCalculationException {
        Long durationSeconds = pricingInput.getDurationSeconds();
        if (durationSeconds == null) {
            throw new PriceCalculationException("plug-in or plug-out is not set");
        }
        if (durationSeconds < 0) {
            throw new PriceCalculationException("Values for price calculation must not be negative! Duration was: "
                    + durationSeconds);
        }

        List<PriceCalculator<PricingInput>> calcs = new ArrayList<>();
        Set<TariffDimensionType> dimensionTypes = tariff.getDimensionTypes();
        List<TariffSlice> slices = null;

        for (TariffDimensionType dimensionType : TariffDimensionType.values()) {
            if (!dimensionTypes.contains(dimensionType)) {
                continue;
            }
            if (dimensionType.isSplitOverWindows()) {
                if (slices == null) {
                    slices = TariffWindowSplitter.split(tariff, pricingInput.getPlugIn(), pricingInput.getPlugOut());
                }
                for (Map.Entry<PriceComponent, Long> entry : secondsPerComponent(tariff, dimensionType, slices,
                        pricingInput.getKwh()).entrySet()) {
                    calcs.add(createCalculator(pricingInput, entry.getKey(), entry.getValue()));
                }
            } else {
                PriceComponent component = findApplyingComponent(tariff, dimensionType, pricingInput.getPlugIn(),
                        pricingInput.getKwh());
                if (component != null) {
                    calcs.add(createCalculator(pricingInput, component, null));
                }
            }
        }
        return calcs;
    }

    /**
     * @return the component of the first element that has the dimension and applies at the given time, or null
     */
    public static PriceComponent findApplyingComponent(Tariff tariff, TariffDimensionType dimensionType,
                                                       LocalDateTime at, BigDecimal sessionKwh) {
        for (TariffElement element : tariff.getTariffElements()) {
            PriceComponent component = element.findComponent(dimensionType);
            if (component != null && element.appliesAt(at, sessionKwh)) {
                return component;
            }
        }
        return null;
    }

    private static Map<PriceComponent, Long> secondsPerComponent(Tariff tariff, TariffDimensionType dimensionType,
                                                                 List<TariffSlice> slices, BigDecimal sessionKwh) {
        Map<PriceComponent, Long> result = new LinkedHashMap<>();
        for (TariffSlice slice : slices) {
            PriceComponent component = findApplyingComponent(tariff, dimensionType, slice.start(), sessionKwh);
            if (component != null) {
                result.merge(component, slice.seconds(), Long::sum);
            }
        }
        return result;
    }

    static PriceCalculator<PricingInput> createCalculator(PricingInput pricingInput, PriceComponent priceComponent,
                                                          Long windowSeconds) {
        switch (priceComponent.getDimensionType()) {
            case ENERGY:
                return new PriceByEnergyCalculator(pricingInput, priceComponent, windowSeconds);
            case TIME:
                return new PriceByTimeCalculator(pricingInput, priceComponent, windowSeconds);
            case PER_NIGHT:
                return new PriceByNightCalculator(pricingInput, priceComponent);
            case FLAT:
                return new PriceByFlatRateCalculator(pricingInput, priceComponent);
            default:
                throw new IllegalArgumentException("Unsupported tariff dimension type "
                        + priceComponent.getDimensionType());
        }
    }
}
