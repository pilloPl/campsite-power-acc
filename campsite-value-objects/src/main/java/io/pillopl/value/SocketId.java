package io.pillopl.value;

/**
 * Database id of a socket.
 */
public record SocketId(long value) {

    public SocketId {
        if (value < 1) {
            throw new InvalidValueException("SocketId", "must be positive but is " + value);
        }
    }

    public static SocketId of(Long value) {
        if (value == null) {
            throw new InvalidValueException("SocketId", "is missing");
        }
        return new SocketId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
