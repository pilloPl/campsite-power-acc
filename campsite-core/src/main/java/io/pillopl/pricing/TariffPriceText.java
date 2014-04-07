package io.pillopl.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Static helpers writing a tariff as short price text, e.g. for the display of the pedestal at the start of a
 * supply and the price lists of the console: {@code "0.45 EUR/kWh + 1.00 EUR/session; 22:00-06:00: 0.30 EUR/kWh"}.
 */
public final class TariffPriceText {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.");

    private TariffPriceText() {
        throw new AssertionError();
    }

    public static String describe(Tariff tariff) {
        if (tariff == null) {
            return null;
        }
        List<String> elements = new ArrayList<>();
        for (TariffElement element : tariff.getTariffElements()) {
            String restriction = describe(element.getTariffRestriction());
            String components = describeComponents(element, tariff.getCurrency());
            elements.add(restriction.isEmpty() ? components : restriction + ": " + components);
        }
        return String.join("; ", elements);
    }

    static String describeComponents(TariffElement element, String currency) {
        List<String> parts = new ArrayList<>();
        for (PriceComponent component : element.getPriceComponents()) {
            parts.add(price(component.getPrice()) + " " + currency + "/" + component.getPriceUnit().getLabel());
        }
        return String.join(" + ", parts);
    }

    static String describe(TariffRestriction restriction) {
        if (restriction == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (restriction.getStartDate() != null || restriction.getEndDate() != null) {
            parts.add((restriction.getStartDate() != null ? DATE.format(restriction.getStartDate()) : "") + "-"
                    + (restriction.getEndDate() != null ? DATE.format(restriction.getEndDate()) : ""));
        }
        if (restriction.getDaysOfWeek() != null && !restriction.getDaysOfWeek().isEmpty()) {
            List<String> days = new ArrayList<>();
            restriction.getDaysOfWeek().stream().sorted().forEach(day -> days.add(
                    day.name().substring(0, 3).toLowerCase(Locale.ROOT)));
            parts.add(String.join(",", days));
        }
        if (restriction.hasTimeOfDay()) {
            parts.add(TIME.format(restriction.getStartTime()) + "-" + TIME.format(restriction.getEndTime()));
        }
        if (restriction.getMinKwh() != null || restriction.getMaxKwh() != null) {
            parts.add((restriction.getMinKwh() != null ? restriction.getMinKwh().stripTrailingZeros().toPlainString()
                    : "0") + "-" + (restriction.getMaxKwh() != null
                    ? restriction.getMaxKwh().stripTrailingZeros().toPlainString() : "") + " kWh");
        }
        return String.join(" ", parts);
    }

    private static String price(BigDecimal price) {
        return price.setScale(Math.max(2, price.stripTrailingZeros().scale()), RoundingMode.UNNECESSARY)
                .toPlainString();
    }
}
