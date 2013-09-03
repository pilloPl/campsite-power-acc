package io.pillopl.value;

/**
 * A socket as the pedestal link addresses it: the pedestal code and the socket number on the pedestal (1..6).
 */
public record SocketLinkId(PedestalCode pedestalCode, int socketNo) {

    public static final int MAX_SOCKET_NO = 6;

    public SocketLinkId {
        if (pedestalCode == null) {
            throw new InvalidValueException("SocketLinkId", "pedestal code is missing");
        }
        if (socketNo < 1 || socketNo > MAX_SOCKET_NO) {
            throw new InvalidValueException("SocketLinkId", "socket number must be 1.." + MAX_SOCKET_NO
                    + " but is " + socketNo);
        }
    }

    public static SocketLinkId of(String pedestalCode, Integer socketNo) {
        if (socketNo == null) {
            throw new InvalidValueException("SocketLinkId", "socket number is missing");
        }
        return new SocketLinkId(PedestalCode.of(pedestalCode), socketNo);
    }

    public String getPedestalCode() {
        return pedestalCode.value();
    }

    @Override
    public String toString() {
        return pedestalCode.value() + "/" + socketNo;
    }
}
