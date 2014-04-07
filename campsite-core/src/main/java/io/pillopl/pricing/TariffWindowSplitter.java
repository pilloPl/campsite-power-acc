package io.pillopl.pricing;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Cuts a supply into {@link TariffSlice}s at every point where a restriction of the tariff may change its answer:
 * every midnight (dates and days of the week) and every start and end hour of a window, on every day of the supply.
 */
public final class TariffWindowSplitter {

    private TariffWindowSplitter() {
        throw new AssertionError();
    }

    /**
     * @return consecutive slices from {@code from} to {@code to}; one empty slice when both are equal
     */
    public static List<TariffSlice> split(Tariff tariff, LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException(String.format("Cannot split supply from %s to %s", from, to));
        }
        List<TariffSlice> slices = new ArrayList<>();
        if (from.equals(to)) {
            slices.add(new TariffSlice(from, to));
            return slices;
        }

        TreeSet<LocalDateTime> boundaries = new TreeSet<>();
        boundaries.add(from);
        boundaries.add(to);
        for (LocalDate day = from.toLocalDate(); !day.isAfter(to.toLocalDate()); day = day.plusDays(1)) {
            addIfInside(boundaries, day.atStartOfDay(), from, to);
            for (TariffElement element : tariff.getTariffElements()) {
                TariffRestriction restriction = element.getTariffRestriction();
                if (restriction != null && restriction.hasTimeOfDay()) {
                    addIfInside(boundaries, day.atTime(restriction.getStartTime()), from, to);
                    addIfInside(boundaries, day.atTime(restriction.getEndTime()), from, to);
                }
            }
        }

        LocalDateTime start = null;
        for (LocalDateTime boundary : boundaries) {
            if (start != null) {
                slices.add(new TariffSlice(start, boundary));
            }
            start = boundary;
        }
        return slices;
    }

    private static void addIfInside(TreeSet<LocalDateTime> boundaries, LocalDateTime boundary, LocalDateTime from,
                                    LocalDateTime to) {
        if (boundary.isAfter(from) && boundary.isBefore(to)) {
            boundaries.add(boundary);
        }
    }
}
