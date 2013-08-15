package io.pillopl.pricing;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * When a tariff element applies: hours of the day (night tariff), season dates (high / low season), days of the
 * week (weekend) and the kWh range of the session. Every part left empty does not restrict.
 * <p>
 * Hours: start inclusive, end exclusive; an end before the start wraps over midnight (22:00-06:00).
 * Dates: both inclusive (the season runs "until 31 August"). kWh: minimum inclusive, maximum exclusive, checked
 * against the energy of the whole session.
 */
@Entity
@Table(name = "TARIFF_RESTRICTION")
@Getter
@Setter
public class TariffRestriction {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_tariff_restriction")
    @SequenceGenerator(name = "seq_tariff_restriction", sequenceName = "TARIFF_RESTRICTION_ID_SEQ",
            allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "TARIFF_ELEMENT_ID", nullable = false, unique = true)
    private TariffElement tariffElement;

    @Column(name = "START_TIME")
    private LocalTime startTime;

    @Column(name = "END_TIME")
    private LocalTime endTime;

    @Column(name = "START_DATE")
    private LocalDate startDate;

    @Column(name = "END_DATE")
    private LocalDate endDate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "TARIFF_RESTRICTION_DAY", joinColumns = @JoinColumn(name = "RESTRICTION_ID"))
    @Column(name = "DAY_OF_WEEK", length = 16)
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> daysOfWeek = EnumSet.noneOf(DayOfWeek.class);

    @Column(name = "MIN_KWH", precision = 19, scale = 4)
    private BigDecimal minKwh;

    @Column(name = "MAX_KWH", precision = 19, scale = 4)
    private BigDecimal maxKwh;

    public TariffRestriction() {
    }

    public static TariffRestriction hours(LocalTime startTime, LocalTime endTime) {
        TariffRestriction restriction = new TariffRestriction();
        restriction.setStartTime(startTime);
        restriction.setEndTime(endTime);
        return restriction;
    }

    public static TariffRestriction season(LocalDate startDate, LocalDate endDate) {
        TariffRestriction restriction = new TariffRestriction();
        restriction.setStartDate(startDate);
        restriction.setEndDate(endDate);
        return restriction;
    }

    public static TariffRestriction days(DayOfWeek... days) {
        TariffRestriction restriction = new TariffRestriction();
        for (DayOfWeek day : days) {
            restriction.getDaysOfWeek().add(day);
        }
        return restriction;
    }

    public static TariffRestriction kwhRange(BigDecimal minKwh, BigDecimal maxKwh) {
        TariffRestriction restriction = new TariffRestriction();
        restriction.setMinKwh(minKwh);
        restriction.setMaxKwh(maxKwh);
        return restriction;
    }

    public boolean hasTimeOfDay() {
        return startTime != null && endTime != null;
    }

    /**
     * @param sessionKwh energy of the whole session; null skips the kWh range (not known yet, e.g. at the start)
     */
    public boolean matches(LocalDateTime at, BigDecimal sessionKwh) {
        return matchesDate(at.toLocalDate()) && matchesDayOfWeek(at.getDayOfWeek()) && matchesTime(at.toLocalTime())
                && matchesKwh(sessionKwh);
    }

    boolean matchesDate(LocalDate date) {
        if (startDate != null && date.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !date.isAfter(endDate);
    }

    boolean matchesDayOfWeek(DayOfWeek dayOfWeek) {
        return daysOfWeek == null || daysOfWeek.isEmpty() || daysOfWeek.contains(dayOfWeek);
    }

    boolean matchesTime(LocalTime time) {
        if (!hasTimeOfDay() || startTime.equals(endTime)) {
            return true;
        }
        if (startTime.isBefore(endTime)) {
            return !time.isBefore(startTime) && time.isBefore(endTime);
        }
        // over midnight
        return !time.isBefore(startTime) || time.isBefore(endTime);
    }

    boolean matchesKwh(BigDecimal sessionKwh) {
        if (sessionKwh == null) {
            return true;
        }
        if (minKwh != null && sessionKwh.compareTo(minKwh) < 0) {
            return false;
        }
        return maxKwh == null || sessionKwh.compareTo(maxKwh) < 0;
    }

    @Override
    public String toString() {
        return "TariffRestriction{id=" + id + ", time=" + startTime + "-" + endTime + ", dates=" + startDate + ".."
                + endDate + ", days=" + daysOfWeek + ", kWh=" + minKwh + ".." + maxKwh + "}";
    }
}
