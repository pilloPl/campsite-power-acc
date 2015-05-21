package io.pillopl.guest;

import io.pillopl.group.Group;
import io.pillopl.order.OrderItem;
import io.pillopl.order.ProductDefinition;
import io.pillopl.pricing.Tariff;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Seasonal resident's contract: a fixed pitch for the season, a fixed fee and a kWh package included; the kWh above
 * the package are priced by the contract's tariff. While the contract runs its tariff overrides the pedestal's.
 */
@Entity
@Table(name = "SEASONAL_CONTRACT")
@Getter
@Setter
public class SeasonalContract {

    public enum FeePeriod {
        MONTHLY,
        SEASONAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_seasonal_contract")
    @SequenceGenerator(name = "seq_seasonal_contract", sequenceName = "SEASONAL_CONTRACT_ID_SEQ",
            allocationSize = 1)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "BILLING_ACCOUNT_ID", nullable = false)
    private BillingAccount billingAccount;

    @ManyToOne
    @JoinColumn(name = "ORDER_ITEM_ID")
    private OrderItem orderItem;

    @ManyToOne(optional = false)
    @JoinColumn(name = "TARIFF_ID", nullable = false)
    private Tariff tariff;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 32, nullable = false)
    private ContractStatus status = ContractStatus.ACTIVE;

    @ManyToOne
    @JoinColumn(name = "SITE_GROUP_ID")
    private Group siteGroup;

    @Column(name = "PITCH_NO", length = 16)
    private String pitchNo;

    @Column(name = "START_DATE", nullable = false)
    private LocalDate startDate;

    @Column(name = "END_DATE")
    private LocalDate endDate;

    @Column(name = "REQUESTED_END_DATE")
    private LocalDate requestedEndDate;

    @Column(name = "FIXED_FEE", precision = 19, scale = 2, nullable = false)
    private BigDecimal fixedFee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "FEE_PERIOD", length = 16, nullable = false)
    private FeePeriod feePeriod = FeePeriod.SEASONAL;

    @Column(name = "CREATE_DATE", nullable = false)
    private LocalDateTime createDate = LocalDateTime.now();

    @Column(name = "MOD_DATE")
    private LocalDateTime modDate;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<PackageItem> packageItems = new ArrayList<>();

    public SeasonalContract() {
    }

    public SeasonalContract(BillingAccount billingAccount, Tariff tariff, Group siteGroup, String pitchNo,
                            LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Season needs a start not after its end: " + startDate + " - "
                    + endDate);
        }
        this.billingAccount = billingAccount;
        this.tariff = tariff;
        this.siteGroup = siteGroup;
        this.pitchNo = pitchNo;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public PackageItem addPackageItem(ProductDefinition product, OrderItem fromOrderItem, BigDecimal kwh) {
        PackageItem item = new PackageItem(this, product, fromOrderItem, kwh);
        packageItems.add(item);
        return item;
    }

    /**
     * kWh included in the fixed fee: the sum of the package items.
     */
    public BigDecimal getPackageKwh() {
        BigDecimal sum = BigDecimal.ZERO;
        for (PackageItem item : packageItems) {
            sum = sum.add(item.getPackageKwh());
        }
        return sum;
    }

    /**
     * Last day the contract runs: the requested end of a termination if earlier than the season's end.
     */
    public LocalDate getLastDay() {
        if (requestedEndDate != null && (endDate == null || requestedEndDate.isBefore(endDate))) {
            return requestedEndDate;
        }
        return endDate;
    }

    /**
     * The contract runs on the day: not ended, the day within start and last day (both inclusive).
     */
    public boolean isActiveOn(LocalDate day) {
        if (status == ContractStatus.ENDED || day == null || day.isBefore(startDate)) {
            return false;
        }
        LocalDate lastDay = getLastDay();
        return lastDay == null || !day.isAfter(lastDay);
    }

    /**
     * The contract ran on some day of the period [from, to]; an ended contract counts up to its end date.
     */
    public boolean isActiveIn(LocalDate from, LocalDate to) {
        if (to.isBefore(startDate) || status == ContractStatus.ENDED && endDate == null) {
            return false;
        }
        LocalDate lastDay = getLastDay();
        return lastDay == null || !from.isAfter(lastDay);
    }

    /**
     * The resident leaves before the end of the season.
     */
    public void requestTermination(LocalDate requestedEnd, LocalDateTime at) {
        if (status == ContractStatus.ENDED) {
            throw new IllegalStateException("Contract " + id + " has ended already");
        }
        if (requestedEnd == null || requestedEnd.isBefore(startDate)) {
            throw new IllegalArgumentException("Requested end " + requestedEnd + " is before the start " + startDate);
        }
        this.requestedEndDate = requestedEnd;
        this.status = ContractStatus.TERMINATION_REQUESTED;
        this.modDate = at;
    }

    public void end(LocalDate lastDay, LocalDateTime at) {
        this.endDate = lastDay;
        this.status = ContractStatus.ENDED;
        this.modDate = at;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SeasonalContract other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "SeasonalContract{id=" + id + ", " + status + ", pitch='" + pitchNo + "', " + startDate + " - "
                + getLastDay() + ", fee=" + fixedFee + " " + feePeriod + "}";
    }
}
