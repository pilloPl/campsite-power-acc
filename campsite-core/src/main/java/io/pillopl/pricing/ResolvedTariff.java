package io.pillopl.pricing;

import java.util.Objects;

/**
 * The tariff that prices a supply and the step of the resolution that found it.
 */
public record ResolvedTariff(Tariff tariff, TariffSource source) {

    public ResolvedTariff {
        Objects.requireNonNull(tariff, "tariff");
        Objects.requireNonNull(source, "source");
    }
}
