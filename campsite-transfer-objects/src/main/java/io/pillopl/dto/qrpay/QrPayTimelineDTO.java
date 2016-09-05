package io.pillopl.dto.qrpay;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The row chain of a QR payment as a timeline (one step per state, with its time span and error) together with the
 * state machine (all states and the allowed transitions), so the console can draw the path through the diagram.
 */
public record QrPayTimelineDTO(Long processId, String orderReference, String currentState, boolean archived,
                               boolean cancellable, List<Step> steps, List<String> states,
                               List<Transition> transitions) implements Serializable {

    public QrPayTimelineDTO {
        steps = steps != null ? List.copyOf(steps) : List.of();
        states = states != null ? List.copyOf(states) : List.of();
        transitions = transitions != null ? List.copyOf(transitions) : List.of();
    }

    /**
     * One row of the chain: the state from {@code since} until the next row ({@code until}, null for the current).
     */
    public record Step(Long rowId, String state, LocalDateTime since, LocalDateTime until, Integer transactionId,
                       String errorCode, String errorMessage) implements Serializable {
    }

    /**
     * An edge of the state machine.
     */
    public record Transition(String from, String to) implements Serializable {
    }
}
