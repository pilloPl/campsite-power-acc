package io.pillopl.pricing;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of pricing a supply. PRICED carries the cost (scale 2) and its lines; PRICING_FAILED carries no cost but
 * the reason; NOT_BILLABLE costs zero.
 */
public final class PriceCalculation {

    private final PricingStatus status;

    private final BigDecimal cost;

    private final String currency;

    private final Long tariffId;

    private final String tariffName;

    private final TariffSource tariffSource;

    private final List<PriceCalculationLine> lines;

    private final String failureReason;

    private PriceCalculation(PricingStatus status, BigDecimal cost, String currency, Long tariffId,
                             String tariffName, TariffSource tariffSource, List<PriceCalculationLine> lines,
                             String failureReason) {
        this.status = status;
        this.cost = cost;
        this.currency = currency;
        this.tariffId = tariffId;
        this.tariffName = tariffName;
        this.tariffSource = tariffSource;
        this.lines = List.copyOf(lines);
        this.failureReason = failureReason;
    }

    public static PriceCalculation priced(ResolvedTariff resolved, BigDecimal cost, List<PriceCalculationLine> lines) {
        Tariff tariff = resolved.tariff();
        return new PriceCalculation(PricingStatus.PRICED, cost, tariff.getCurrency(), tariff.getId(), tariff.getName(),
                resolved.source(), lines, null);
    }

    /**
     * @param resolved null when no tariff was found
     */
    public static PriceCalculation failed(ResolvedTariff resolved, String reason) {
        return new PriceCalculation(PricingStatus.PRICING_FAILED, null,
                resolved != null ? resolved.tariff().getCurrency() : null,
                resolved != null ? resolved.tariff().getId() : null,
                resolved != null ? resolved.tariff().getName() : null,
                resolved != null ? resolved.source() : null, List.of(), reason);
    }

    public static PriceCalculation notBillable() {
        return new PriceCalculation(PricingStatus.NOT_BILLABLE, BigDecimal.ZERO.setScale(PricingService.MONEY_SCALE),
                null, null, null, null, List.of(), null);
    }

    public boolean isPriced() {
        return status == PricingStatus.PRICED;
    }

    public PricingStatus getStatus() {
        return status;
    }

    /**
     * @return the cost at scale 2, null when the pricing failed
     */
    public BigDecimal getCost() {
        return cost;
    }

    public String getCurrency() {
        return currency;
    }

    public Long getTariffId() {
        return tariffId;
    }

    public String getTariffName() {
        return tariffName;
    }

    public TariffSource getTariffSource() {
        return tariffSource;
    }

    public List<PriceCalculationLine> getLines() {
        return lines;
    }

    public String getFailureReason() {
        return failureReason;
    }

    @Override
    public String toString() {
        return "PriceCalculation{" + status + ", cost=" + cost + " " + currency + ", tariff='" + tariffName + "' ("
                + tariffSource + "), lines=" + lines.size() + (failureReason != null ? ", reason='" + failureReason
                + "'" : "") + "}";
    }
}
