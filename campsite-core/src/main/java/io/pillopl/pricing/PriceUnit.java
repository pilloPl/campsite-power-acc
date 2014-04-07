package io.pillopl.pricing;

/**
 * Unit of the price of a component, shown on price lists and in the price simulation.
 */
public enum PriceUnit {
    PER_KWH("kWh"),
    PER_HOUR("h"),
    PER_NIGHT("night"),
    PER_SESSION("session");

    private final String label;

    PriceUnit(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
