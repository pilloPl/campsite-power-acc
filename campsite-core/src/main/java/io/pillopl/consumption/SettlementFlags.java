package io.pillopl.consumption;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * How a consumption record is settled: billable (billed to the guest at all), settled by the campsite (the operator
 * bills its guests itself, the network only reports) and house card rental (a rental house card of the campsite
 * supplied, billed by the reception with the rental).
 */
@Embeddable
public class SettlementFlags {

    @Column(name = "BILLABLE", nullable = false)
    private boolean billable;

    @Column(name = "SETTLED_BY_CAMPSITE", nullable = false)
    private boolean settledByCampsite;

    @Column(name = "HOUSE_CARD_RENTAL", nullable = false)
    private boolean houseCardRental;

    protected SettlementFlags() {
    }

    public SettlementFlags(boolean billable, boolean settledByCampsite, boolean houseCardRental) {
        this.billable = billable;
        this.settledByCampsite = settledByCampsite;
        this.houseCardRental = houseCardRental;
    }

    public static SettlementFlags forCampingCard(boolean billable, boolean settledByCampsite) {
        return new SettlementFlags(billable, settledByCampsite, false);
    }

    /**
     * House cards are the campsite's own use and never billed; a rental house card is marked for the reception.
     */
    public static SettlementFlags forHouseCard(boolean rental) {
        return new SettlementFlags(false, false, rental);
    }

    public boolean isBillable() {
        return billable;
    }

    public boolean isSettledByCampsite() {
        return settledByCampsite;
    }

    public boolean isHouseCardRental() {
        return houseCardRental;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SettlementFlags that)) {
            return false;
        }
        return billable == that.billable && settledByCampsite == that.settledByCampsite
                && houseCardRental == that.houseCardRental;
    }

    @Override
    public int hashCode() {
        return Objects.hash(billable, settledByCampsite, houseCardRental);
    }

    @Override
    public String toString() {
        return "SettlementFlags{billable=" + billable + ", settledByCampsite=" + settledByCampsite
                + ", houseCardRental=" + houseCardRental + "}";
    }
}
