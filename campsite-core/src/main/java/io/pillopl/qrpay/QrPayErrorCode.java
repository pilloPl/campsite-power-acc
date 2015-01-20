package io.pillopl.qrpay;

/**
 * Why a QR payment process went to ERROR; the payment app shows the text to the guest.
 */
public enum QrPayErrorCode {
    SOCKET_UNKNOWN("The socket of the code is unknown."),
    SOCKET_UNAVAILABLE("The socket cannot supply now."),
    NO_TARIFF("The socket has no tariff, it cannot be paid by QR."),
    REMOTE_SUPPLY_START_FAILED("The pedestal did not accept the start."),
    REMOTE_SUPPLY_STOP_FAILED("The pedestal did not accept the stop."),
    SUPPLY_NOT_STARTED("The supply has not started yet."),
    SUPPLY_ALREADY_STARTED("The supply was started before."),
    CALCULATION_FAILED("The price of the supply could not be calculated."),
    UNSUPPORTED_OPERATION("This operation is not allowed in the state of the payment."),
    UNDEFINED("An unspecific error occurred during the payment.");

    private final String text;

    QrPayErrorCode(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
