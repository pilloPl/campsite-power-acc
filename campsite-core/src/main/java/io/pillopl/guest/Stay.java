package io.pillopl.guest;

import io.pillopl.card.CampingCard;
import io.pillopl.group.Group;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * A guest on a pitch of a campsite from arrival to departure day. The stay is what the camping card is valid for,
 * and its check-out is the moment the supply sessions left open by the pedestals are closed.
 */
@Entity
@Table(name = "STAY")
@Getter
@Setter
public class Stay {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_stay")
    @SequenceGenerator(name = "seq_stay", sequenceName = "STAY_ID_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "GUEST_ID", nullable = false)
    private Guest guest;

    /**
     * The campsite (SITE group) the pitch belongs to.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "SITE_GROUP_ID", nullable = false)
    private Group siteGroup;

    @Column(name = "PITCH_NO", length = 16)
    private String pitchNo;

    @ManyToOne
    @JoinColumn(name = "CARD_ID")
    private CampingCard card;

    @ManyToOne
    @JoinColumn(name = "UNIT_ID")
    private Unit unit;

    @Column(name = "ARRIVAL_DATE", nullable = false)
    private LocalDate arrivalDate;

    @Column(name = "DEPARTURE_DATE", nullable = false)
    private LocalDate departureDate;

    @Column(name = "ADULTS", nullable = false)
    private int adults = 1;

    @Column(name = "CHILDREN", nullable = false)
    private int children;

    @Column(name = "CHECK_IN_DATE", nullable = false)
    private LocalDateTime checkInDate;

    @Column(name = "CHECKED_IN_BY", length = 100)
    private String checkedInBy;

    @Column(name = "CHECK_OUT_DATE")
    private LocalDateTime checkOutDate;

    @Column(name = "CHECKED_OUT_BY", length = 100)
    private String checkedOutBy;

    @Column(name = "REMARKS", length = 1000)
    private String remarks;

    @Column(name = "DELETED", nullable = false)
    private boolean deleted;

    public Stay() {
    }

    public Stay(Guest guest, Group siteGroup, String pitchNo, LocalDate arrivalDate, LocalDate departureDate) {
        if (arrivalDate == null || departureDate == null || arrivalDate.isAfter(departureDate)) {
            throw new IllegalArgumentException("Stay needs an arrival not after the departure: " + arrivalDate
                    + " - " + departureDate);
        }
        this.guest = guest;
        this.siteGroup = siteGroup;
        this.pitchNo = pitchNo;
        this.arrivalDate = arrivalDate;
        this.departureDate = departureDate;
    }

    public boolean isCheckedOut() {
        return checkOutDate != null;
    }

    /**
     * The guest is on the campsite on the day: checked in, not checked out, between arrival and departure day.
     */
    public boolean covers(LocalDate day) {
        return !deleted && !isCheckedOut() && day != null && !day.isBefore(arrivalDate)
                && !day.isAfter(departureDate);
    }

    /**
     * Nights between arrival and departure; a stay leaving on its arrival day counts one night.
     */
    public long getNights() {
        return Math.max(1, ChronoUnit.DAYS.between(arrivalDate, departureDate));
    }

    /**
     * The plate of the stay's unit, otherwise the plate the guest arrives with.
     */
    public String getUnitPlate() {
        if (unit != null && unit.getPlate() != null) {
            return unit.getPlate();
        }
        return guest != null ? Unit.normalizePlate(guest.getUnitPlate()) : null;
    }

    /**
     * Extends or shortens the stay; the card handed out for it follows.
     */
    public void changeDeparture(LocalDate newDeparture) {
        if (isCheckedOut()) {
            throw new IllegalStateException("Stay " + id + " is checked out already");
        }
        if (newDeparture == null || newDeparture.isBefore(arrivalDate)) {
            throw new IllegalArgumentException("Departure " + newDeparture + " is before the arrival " + arrivalDate);
        }
        this.departureDate = newDeparture;
        if (card != null) {
            card.setValidTo(newDeparture);
        }
    }

    /**
     * The guest leaves: the card is valid until the end of the check-out day at the latest.
     */
    public void checkOut(LocalDateTime at, String username) {
        if (isCheckedOut()) {
            throw new IllegalStateException("Stay " + id + " is checked out already");
        }
        this.checkOutDate = at;
        this.checkedOutBy = username;
        LocalDate lastDay = at.toLocalDate();
        if (card != null && (card.getValidTo() == null || card.getValidTo().isAfter(lastDay))) {
            card.setValidTo(lastDay.isBefore(arrivalDate) ? arrivalDate : lastDay);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Stay other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Stay{id=" + id + ", pitch='" + pitchNo + "', " + arrivalDate + " - " + departureDate
                + (isCheckedOut() ? ", checked out " + checkOutDate : "") + "}";
    }
}
