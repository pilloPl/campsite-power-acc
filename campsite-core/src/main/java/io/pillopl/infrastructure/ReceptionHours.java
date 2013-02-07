package io.pillopl.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Opening hours of the campsite reception on one day of the week (several rows per day are allowed, e.g. a lunch
 * break).
 */
@Entity
@Table(name = "RECEPTION_HOURS")
@Getter
@Setter
public class ReceptionHours {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_reception_hours")
    @SequenceGenerator(name = "seq_reception_hours", sequenceName = "RECEPTION_HOURS_ID_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "LOCATION_ID", nullable = false)
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(name = "DAY_OF_WEEK", length = 16, nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "OPEN_FROM", nullable = false)
    private LocalTime openFrom;

    @Column(name = "OPEN_TO", nullable = false)
    private LocalTime openTo;

    public ReceptionHours() {
    }

    public ReceptionHours(DayOfWeek dayOfWeek, LocalTime openFrom, LocalTime openTo) {
        this.dayOfWeek = dayOfWeek;
        this.openFrom = openFrom;
        this.openTo = openTo;
    }

    /**
     * Inclusive start, exclusive end.
     */
    public boolean isOpenAt(DayOfWeek day, LocalTime time) {
        return dayOfWeek == day && !time.isBefore(openFrom) && time.isBefore(openTo);
    }

    @Override
    public String toString() {
        return dayOfWeek + " " + openFrom + "-" + openTo;
    }
}
