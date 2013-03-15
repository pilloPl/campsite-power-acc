package io.pillopl.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;

/**
 * Heartbeat of a pedestal link: the approximate time of the last incoming message and the timeout flag set by the
 * heartbeat timeout job. "Approximate" because it is only written when it moved by more than the write threshold.
 */
@Entity
@Table(name = "PEDESTAL_STATE")
@Getter
@Setter
public class PedestalState {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_pedestal_state")
    @SequenceGenerator(name = "seq_pedestal_state", sequenceName = "PEDESTAL_STATE_ID_SEQ", allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PEDESTAL_ID", nullable = false, unique = true)
    private Pedestal pedestal;

    @Column(name = "APPROX_LAST_INCOMING", nullable = false)
    private LocalDateTime approxLastIncoming;

    @Column(name = "LAST_TIMEOUT")
    private LocalDateTime lastTimeout;

    @Column(name = "TIMEOUT", nullable = false)
    private boolean timeout;

    public PedestalState() {
    }

    public PedestalState(Pedestal pedestal, LocalDateTime approxLastIncoming) {
        this.pedestal = pedestal;
        this.approxLastIncoming = approxLastIncoming;
    }

    /**
     * A message arrived: the link is alive again.
     *
     * @return true when the timeout was cleared by this message
     */
    public boolean incoming(LocalDateTime at) {
        approxLastIncoming = at;
        if (timeout) {
            timeout = false;
            return true;
        }
        return false;
    }

    /**
     * @return true when the pedestal just timed out (it was alive until now)
     */
    public boolean checkTimeout(LocalDateTime now, int timeoutInMinutes) {
        if (!timeout && approxLastIncoming.plusMinutes(timeoutInMinutes).isBefore(now)) {
            timeout = true;
            lastTimeout = now;
            return true;
        }
        return false;
    }
}
